package com.footballpluse.footballapp.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.repository.OnboardingRepository
import com.footballpluse.footballapp.data.repository.UsernameTakenException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<NavigationEvent> = _navigationEvent.receiveAsFlow()

    private var usernameCheckJob: Job? = null

    val defaultLeagues: List<League> = listOf(
        League("152", "Premier League", "England", "\uD83C\uDFF4\uD83C\uDFE6\uD83C\uDFFD\u200D\uD83C\uDFF3\uFE0F\u200D\uD83C\uDFF4", "https://apiv3.apifootball.com/badges/logo_leagues/152_premier-league.png"),
        League("302", "La Liga", "Spain", "\uD83C\uDDEA\uD83C\uDDF8", "https://apiv3.apifootball.com/badges/logo_leagues/302_la-liga.png"),
        League("207", "Serie A", "Italy", "\uD83C\uDDEE\uD83C\uDDF9", "https://apiv3.apifootball.com/badges/logo_leagues/207_serie-a.png"),
        League("175", "Bundesliga", "Germany", "\uD83C\uDDE9\uD83C\uDDEA", "https://apiv3.apifootball.com/badges/logo_leagues/175_bundesliga.png"),
        League("168", "Ligue 1", "France", "\uD83C\uDDEB\uD83C\uDDF7", "https://apiv3.apifootball.com/badges/logo_leagues/168_ligue-1.png"),
    )

    init {
        val savedUsername = onboardingRepository.getUsernamePref()
        if (!savedUsername.isNullOrBlank()) {
            _state.update { it.copy(username = savedUsername) }
        }

        val savedLeague = onboardingRepository.getOnboardingLeagueProgress()
        if (savedLeague != null) {
            val league = defaultLeagues.find { it.id == savedLeague }
            if (league != null) {
                _state.update { it.copy(selectedLeague = league) }
            }
        }

        val savedClubIds = onboardingRepository.getOnboardingClubProgress()
        if (savedClubIds.isNotEmpty()) {
            val allClubs = clubDataByLeagueId.values.flatten()
            val clubs = allClubs.filter { it.id in savedClubIds }
            if (clubs.isNotEmpty()) {
                _state.update { it.copy(selectedClubs = clubs) }
            }
        }
    }

    // --- Username ---

    fun onUsernameChanged(input: String) {
        val clean = input.filter { it.isLetterOrDigit() || it == '_' }
        if (clean.length > 20) return

        _state.update { it.copy(username = clean) }

        usernameCheckJob?.cancel()

        when {
            clean.isEmpty() -> {
                _state.update { it.copy(usernameStatus = UsernameStatus.Idle, suggestions = emptyList()) }
            }
            clean.length < 3 -> {
                _state.update {
                    it.copy(
                        usernameStatus = UsernameStatus.Invalid("Username must be at least 3 characters"),
                        suggestions = emptyList()
                    )
                }
            }
            !clean.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                _state.update {
                    it.copy(
                        usernameStatus = UsernameStatus.Invalid("Only letters, numbers and underscores allowed"),
                        suggestions = emptyList()
                    )
                }
            }
            else -> {
                // If username matches locally saved one, skip Firebase check entirely
                val localUsername = onboardingRepository.getUsernamePref()
                val isOwnUsername = !localUsername.isNullOrBlank() && clean.equals(localUsername, ignoreCase = true)
                if (isOwnUsername) {
                    _state.update {
                        it.copy(
                            usernameStatus = UsernameStatus.Available(clean),
                            suggestions = emptyList()
                        )
                    }
                    return
                }

                _state.update { it.copy(usernameStatus = UsernameStatus.Checking) }
                usernameCheckJob = viewModelScope.launch {
                    delay(600)
                    try {
                        val available = onboardingRepository.checkUsernameAvailability(clean)
                        val isOwnDevice = !available && onboardingRepository.isUsernameOwnedByCurrentDevice(clean)

                        when {
                            available || isOwnDevice -> {
                                _state.update {
                                    it.copy(
                                        usernameStatus = UsernameStatus.Available(clean),
                                        suggestions = emptyList()
                                    )
                                }
                            }
                            else -> {
                                _state.update {
                                    it.copy(
                                        usernameStatus = UsernameStatus.Taken(clean),
                                        suggestions = generateSuggestions(clean)
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        val message = e.localizedMessage ?: "Could not check availability. Try again."
                        _state.update {
                            it.copy(
                                usernameStatus = UsernameStatus.Invalid(message),
                                suggestions = emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    fun submitUsername() {
        val state = _state.value
        val status = state.usernameStatus
        if (status !is UsernameStatus.Available) return

        val username = status.username
        // If already registered locally, skip Firebase and go straight to leagues
        val savedUsername = onboardingRepository.getUsernamePref()
        val savedUid = onboardingRepository.getUidPref()
        if (!savedUsername.isNullOrBlank() && !savedUid.isNullOrBlank() &&
            username.equals(savedUsername, ignoreCase = true)
        ) {
            viewModelScope.launch {
                _events.emit(OnboardingEvent.NavigateToLeague)
                _navigationEvent.send(NavigationEvent.GoToLeagueScreen)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                onboardingRepository.signInAndRegisterUsername(username)
                _state.update { it.copy(isLoading = false) }
                _events.emit(OnboardingEvent.NavigateToLeague)
                _navigationEvent.send(NavigationEvent.GoToLeagueScreen)
            } catch (e: UsernameTakenException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        usernameStatus = UsernameStatus.Taken(username),
                        suggestions = generateSuggestions(username),
                        errorMessage = e.message
                    )
                }
                _events.emit(OnboardingEvent.ShowSnackbar(e.message ?: "Username taken"))
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        usernameStatus = UsernameStatus.Invalid(
                            e.localizedMessage ?: "Failed to save. Check your connection."
                        ),
                        errorMessage = e.localizedMessage
                    )
                }
            }
        }
    }

    // --- League ---

    fun selectLeague(league: League) {
        _state.update { it.copy(selectedLeague = league) }
        onboardingRepository.saveOnboardingLeagueProgress(league.id)
    }

    fun confirmLeague() {
        val league = _state.value.selectedLeague ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                onboardingRepository.saveFavoriteLeague(league.id)
                _state.update { it.copy(isLoading = false) }
                _events.emit(OnboardingEvent.NavigateToClubs)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _events.emit(OnboardingEvent.ShowSnackbar("Failed to save league"))
            }
        }
    }

    // --- Clubs ---

    fun toggleClub(club: Club) {
        _state.update { state ->
            val current = state.selectedClubs.toMutableList()
            val exists = current.any { it.id == club.id }
            if (exists) current.removeAll { it.id == club.id } else current.add(club)
            onboardingRepository.saveOnboardingClubProgress(current.map { it.id })
            state.copy(selectedClubs = current)
        }
    }

    fun getClubsForLeague(leagueId: String): List<Club> {
        return clubDataByLeagueId[leagueId].orEmpty()
    }

    fun clubsContinue() {
        val clubs = _state.value.selectedClubs
        if (clubs.isEmpty()) return
        val league = _state.value.selectedLeague ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val uid = onboardingRepository.getUidPref() ?: ""
                val entities = clubs.map { club ->
                    com.footballpluse.footballapp.data.local.db.FavoriteClubEntity(
                        clubId = club.id,
                        clubName = club.name,
                        leagueId = club.leagueId,
                        logoUrl = club.logoUrl,
                        addedAt = System.currentTimeMillis()
                    )
                }
                onboardingRepository.completeOnboarding(
                    uid = uid,
                    leagueId = league.id,
                    leagueName = league.name,
                    clubs = entities
                )
                _state.update { it.copy(isLoading = false) }
                _events.emit(OnboardingEvent.NavigateToHome)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _events.emit(OnboardingEvent.ShowSnackbar("Failed to save clubs"))
            }
        }
    }

    fun saveClubsEdit() {
        val clubs = _state.value.selectedClubs
        if (clubs.isEmpty()) return
        viewModelScope.launch {
            val entities = clubs.map { club ->
                com.footballpluse.footballapp.data.local.db.FavoriteClubEntity(
                    clubId = club.id,
                    clubName = club.name,
                    leagueId = club.leagueId,
                    logoUrl = club.logoUrl,
                    addedAt = System.currentTimeMillis()
                )
            }
            val league = _state.value.selectedLeague ?: return@launch
            try {
                val uid = onboardingRepository.getUidPref() ?: ""
                onboardingRepository.saveSelectedClubsOnly(uid, league.id, league.name, entities)
            } catch (e: Exception) {
                _events.emit(OnboardingEvent.ShowSnackbar("Failed to save clubs"))
            }
        }
    }

    fun restoreFromEdit(clubIds: Set<String>) {
        val allClubs = clubDataByLeagueId.values.flatten()
        val selected = allClubs.filter { it.id in clubIds }
        _state.update { it.copy(selectedClubs = selected) }
    }

    fun randomizeUsername() {
        val prefixes = listOf(
            "Striker", "Keeper", "Defender", "Winger", "Midfield",
            "Fanatic", "Legend", "Goal", "Hero", "Ace",
            "Star", "King", "Queen", "Champ", "Pro",
            "Ultra", "Magic", "Turbo", "Rapid", "Prime",
            "Golden", "Silver", "Storm", "Blitz", "Flash",
            "Thunder", "Phoenix", "Cobra", "Falcon", "Panther"
        )
        val suffix = (10..99).random()
        val word = prefixes.random()
        onUsernameChanged("$word$suffix")
    }

    private fun generateSuggestions(username: String): List<String> {
        val suffixes = listOf(
            "123", "fc", "fan", "01", "23", "24", "10",
            "007", "99", "loyal", "forever", "cfc", "utd"
        )
        return suffixes.shuffled().take(4).map { "$username$it" }
    }

    companion object {
        val clubDataByLeagueId: Map<String, List<Club>> = mapOf(
            "152" to listOf(
                Club("80", "Manchester City", "152", "https://apiv3.apifootball.com/badges/80_manchester-city.jpg", 1),
                Club("141", "Arsenal FC", "152", "https://apiv3.apifootball.com/badges/141_arsenal-fc.jpg", 2),
                Club("84", "Liverpool", "152", "https://apiv3.apifootball.com/badges/84_liverpool.jpg", 3),
                Club("88", "Chelsea", "152", "https://apiv3.apifootball.com/badges/88_chelsea.jpg", 4),
                Club("102", "Manchester United", "152", "https://apiv3.apifootball.com/badges/102_manchester-united.jpg", 5),
                Club("164", "Tottenham Hotspur", "152", "https://apiv3.apifootball.com/badges/164_tottenham-hotspur.jpg", 6),
                Club("3081", "West Ham United", "152", "https://apiv3.apifootball.com/badges/3081_west-ham-united.jpg", 7),
                Club("3088", "Aston Villa", "152", "https://apiv3.apifootball.com/badges/3088_aston-villa.jpg", 8),
                Club("3100", "Newcastle United", "152", "https://apiv3.apifootball.com/badges/3100_newcastle-united.jpg", 9),
                Club("3079", "Brighton & Hove Albion", "152", "https://apiv3.apifootball.com/badges/3079_brighton--hove-albion.jpg", 10),
                Club("3429", "Crystal Palace", "152", "https://apiv3.apifootball.com/badges/3429_crystal-palace.jpg", 11),
                Club("3077", "Wolverhampton Wanderers", "152", "https://apiv3.apifootball.com/badges/3077_wolverhampton-wanderers.jpg", 12),
                Club("3073", "Everton", "152", "https://apiv3.apifootball.com/badges/3073_everton.jpg", 13),
                Club("3085", "Fulham", "152", "https://apiv3.apifootball.com/badges/3085_fulham.jpg", 14),
                Club("3086", "Brentford", "152", "https://apiv3.apifootball.com/badges/3086_brentford.jpg", 15),
                Club("3089", "Nottingham Forest", "152", "https://apiv3.apifootball.com/badges/3089_nottingham-forest.jpg", 16),
                Club("3071", "AFC Bournemouth", "152", "https://apiv3.apifootball.com/badges/3071_afc-bournemouth.jpg", 17),
                Club("3111", "Sunderland", "152", "https://apiv3.apifootball.com/badges/3111_sunderland.jpg", 18),
                Club("3103", "Leeds United", "152", "https://apiv3.apifootball.com/badges/3103_leeds-united.jpg", 19),
                Club("3075", "Burnley", "152", "https://apiv3.apifootball.com/badges/3075_burnley.jpg", 20),
            ),
            "302" to listOf(
                Club("76", "Real Madrid", "302", "https://apiv3.apifootball.com/badges/76_real-madrid.jpg", 1),
                Club("97", "Barcelona", "302", "https://apiv3.apifootball.com/badges/97_barcelona.jpg", 2),
                Club("73", "Atlético de Madrid", "302", "https://apiv3.apifootball.com/badges/73_atletico-de-madrid.jpg", 3),
                Club("89", "Sevilla", "302", "https://apiv3.apifootball.com/badges/89_sevilla.jpg", 4),
                Club("7272", "Valencia", "302", "https://apiv3.apifootball.com/badges/7272_valencia.jpg", 5),
                Club("7258", "Athletic Club", "302", "https://apiv3.apifootball.com/badges/7258_athletic-club.jpg", 6),
                Club("162", "Villarreal", "302", "https://apiv3.apifootball.com/badges/162_villarreal.jpg", 7),
                Club("7261", "Real Betis", "302", "https://apiv3.apifootball.com/badges/7261_real-betis.jpg", 8),
                Club("153", "Real Sociedad", "302", "https://apiv3.apifootball.com/badges/153_real-sociedad.jpg", 9),
                Club("7288", "Getafe", "302", "https://apiv3.apifootball.com/badges/7288_getafe.jpg", 10),
                Club("7290", "Celta de Vigo", "302", "https://apiv3.apifootball.com/badges/7290_celta-de-vigo.jpg", 11),
                Club("7285", "Mallorca", "302", "https://apiv3.apifootball.com/badges/7285_mallorca.jpg", 12),
                Club("7269", "Osasuna", "302", "https://apiv3.apifootball.com/badges/7269_osasuna.jpg", 13),
                Club("7264", "Rayo Vallecano", "302", "https://apiv3.apifootball.com/badges/7264_rayo-vallecano.jpg", 14),
                Club("7263", "Girona", "302", "https://apiv3.apifootball.com/badges/7263_girona.jpg", 15),
                Club("7259", "Levante", "302", "https://apiv3.apifootball.com/badges/7259_levante.jpg", 16),
                Club("7275", "Alavés", "302", "https://apiv3.apifootball.com/badges/7275_alaves.jpg", 17),
                Club("7268", "Espanyol", "302", "https://apiv3.apifootball.com/badges/7268_espanyol.jpg", 18),
                Club("7282", "Real Oviedo", "302", "https://apiv3.apifootball.com/badges/7282_real-oviedo.jpg", 19),
                Club("7274", "Elche", "302", "https://apiv3.apifootball.com/badges/7274_elche.jpg", 20),
            ),
            "207" to listOf(
                Club("79", "Internazionale", "207", "https://apiv3.apifootball.com/badges/79_internazionale.jpg", 1),
                Club("96", "Juventus FC", "207", "https://apiv3.apifootball.com/badges/96_juventus-fc.jpg", 2),
                Club("159", "Milan", "207", "https://apiv3.apifootball.com/badges/159_milan.jpg", 3),
                Club("152", "Napoli", "207", "https://apiv3.apifootball.com/badges/152_napoli.jpg", 4),
                Club("139", "Roma", "207", "https://apiv3.apifootball.com/badges/139_roma.jpg", 5),
                Club("93", "Lazio", "207", "https://apiv3.apifootball.com/badges/93_lazio.jpg", 6),
                Club("85", "Atalanta", "207", "https://apiv3.apifootball.com/badges/85_atalanta.jpg", 7),
                Club("4974", "Fiorentina", "207", "https://apiv3.apifootball.com/badges/4974_fiorentina.jpg", 8),
                Club("4983", "Bologna", "207", "https://apiv3.apifootball.com/badges/4983_bologna.jpg", 9),
                Club("4973", "Torino", "207", "https://apiv3.apifootball.com/badges/4973_torino.jpg", 10),
                Club("4984", "Udinese", "207", "https://apiv3.apifootball.com/badges/4984_udinese.jpg", 11),
                Club("8239", "Como", "207", "https://apiv3.apifootball.com/badges/8239_como.jpg", 12),
                Club("5010", "Lecce", "207", "https://apiv3.apifootball.com/badges/5010_lecce.jpg", 13),
                Club("4982", "Hellas Verona", "207", "https://apiv3.apifootball.com/badges/4982_hellas-verona.jpg", 14),
                Club("4981", "Cagliari", "207", "https://apiv3.apifootball.com/badges/4981_cagliari.jpg", 15),
                Club("4998", "Cremonese", "207", "https://apiv3.apifootball.com/badges/4998_cremonese.jpg", 16),
                Club("4978", "Parma", "207", "https://apiv3.apifootball.com/badges/4978_parma.jpg", 17),
                Club("4988", "Pisa", "207", "https://apiv3.apifootball.com/badges/4988_pisa.jpg", 18),
                Club("4975", "Sassuolo", "207", "https://apiv3.apifootball.com/badges/4975_sassuolo.jpg", 19),
                Club("4986", "Genoa", "207", "https://apiv3.apifootball.com/badges/4986_genoa.jpg", 20),
            ),
            "175" to listOf(
                Club("72", "Bayern München", "175", "https://apiv3.apifootball.com/badges/72_bayern-munchen.jpg", 1),
                Club("92", "Borussia Dortmund", "175", "https://apiv3.apifootball.com/badges/92_borussia-dortmund.jpg", 2),
                Club("101", "RB Leipzig", "175", "https://apiv3.apifootball.com/badges/101_rb-leipzig.jpg", 3),
                Club("143", "Bayer Leverkusen", "175", "https://apiv3.apifootball.com/badges/143_bayer-leverkusen.jpg", 4),
                Club("3945", "Eintracht Frankfurt", "175", "https://apiv3.apifootball.com/badges/3945_eintracht-frankfurt.jpg", 5),
                Club("196", "Wolfsburg", "175", "https://apiv3.apifootball.com/badges/196_wolfsburg.jpg", 6),
                Club("77", "Borussia M'gladbach", "175", "https://apiv3.apifootball.com/badges/77_borussia-mgladbach.jpg", 7),
                Club("3933", "Stuttgart", "175", "https://apiv3.apifootball.com/badges/3933_stuttgart.jpg", 8),
                Club("3962", "Freiburg", "175", "https://apiv3.apifootball.com/badges/3962_freiburg.jpg", 9),
                Club("170", "Hoffenheim", "175", "https://apiv3.apifootball.com/badges/170_hoffenheim.jpg", 10),
                Club("3939", "Mainz 05", "175", "https://apiv3.apifootball.com/badges/3939_mainz-05.jpg", 11),
                Club("3934", "Augsburg", "175", "https://apiv3.apifootball.com/badges/3934_augsburg.jpg", 12),
                Club("3930", "Werder Bremen", "175", "https://apiv3.apifootball.com/badges/3930_werder-bremen.jpg", 13),
                Club("3936", "Union Berlin", "175", "https://apiv3.apifootball.com/badges/3936_union-berlin.jpg", 14),
                Club("3912", "Hamburger SV", "175", "https://apiv3.apifootball.com/badges/3912_hamburger-sv.jpg", 15),
                Club("3917", "Heidenheim", "175", "https://apiv3.apifootball.com/badges/3917_heidenheim.jpg", 16),
                Club("3921", "St. Pauli", "175", "https://apiv3.apifootball.com/badges/3921_st.-pauli.jpg", 17),
                Club("3932", "Köln", "175", "https://apiv3.apifootball.com/badges/3932_koln.jpg", 18),
                Club("3920", "Paderborn", "175", "https://apiv3.apifootball.com/badges/3920_paderborn.jpg", 19),
            ),
            "168" to listOf(
                Club("100", "PSG", "168", "https://apiv3.apifootball.com/badges/100_psg.jpg", 1),
                Club("83", "Olympique Marseille", "168", "https://apiv3.apifootball.com/badges/83_olympique-marseille.jpg", 2),
                Club("3817", "Monaco", "168", "https://apiv3.apifootball.com/badges/3817_monaco.jpg", 3),
                Club("3815", "Olympique Lyonnais", "168", "https://apiv3.apifootball.com/badges/3815_olympique-lyonnais.jpg", 4),
                Club("160", "Lille", "168", "https://apiv3.apifootball.com/badges/160_lille.jpg", 5),
                Club("145", "Nice", "168", "https://apiv3.apifootball.com/badges/145_nice.jpg", 6),
                Club("91", "Rennes", "168", "https://apiv3.apifootball.com/badges/91_rennes.jpg", 7),
                Club("3821", "Lens", "168", "https://apiv3.apifootball.com/badges/3821_lens.jpg", 8),
                Club("3794", "Toulouse", "168", "https://apiv3.apifootball.com/badges/3794_toulouse.jpg", 9),
                Club("3823", "Brest", "168", "https://apiv3.apifootball.com/badges/3823_brest.jpg", 10),
                Club("3818", "Strasbourg", "168", "https://apiv3.apifootball.com/badges/3818_strasbourg.jpg", 11),
                Club("3822", "Metz", "168", "https://apiv3.apifootball.com/badges/3822_metz.jpg", 12),
                Club("3820", "Nantes", "168", "https://apiv3.apifootball.com/badges/3820_nantes.jpg", 13),
                Club("3796", "Paris FC", "168", "https://apiv3.apifootball.com/badges/3796_paris-fc.jpg", 14),
                Club("3804", "Le Havre", "168", "https://apiv3.apifootball.com/badges/3804_le-havre.jpg", 15),
                Club("3827", "Angers SCO", "168", "https://apiv3.apifootball.com/badges/3827_angers-sco.jpg", 16),
                Club("3797", "Auxerre", "168", "https://apiv3.apifootball.com/badges/3797_auxerre.jpg", 17),
                Club("3826", "Saint-Étienne", "168", "https://apiv3.apifootball.com/badges/3826_saint-etienne.jpg", 18),
                Club("3814", "Lorient", "168", "https://apiv3.apifootball.com/badges/3814_lorient.jpg", 19),
                Club("3833", "Red Star", "168", "https://apiv3.apifootball.com/badges/3833_red-star.jpg", 20),
            ),
        )
    }
}
