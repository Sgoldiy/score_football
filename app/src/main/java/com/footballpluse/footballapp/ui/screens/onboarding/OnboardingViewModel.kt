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
        League("39", "Premier League", "England", "\uD83C\uDFF4\uD83C\uDFE6\uD83C\uDFFD\u200D\uD83C\uDFF3\uFE0F\u200D\uD83C\uDFF4", "https://media.api-sports.io/football/leagues/39.png"),
        League("140", "La Liga", "Spain", "\uD83C\uDDEA\uD83C\uDDF8", "https://media.api-sports.io/football/leagues/140.png"),
        League("135", "Serie A", "Italy", "\uD83C\uDDEE\uD83C\uDDF9", "https://media.api-sports.io/football/leagues/135.png"),
        League("78", "Bundesliga", "Germany", "\uD83C\uDDE9\uD83C\uDDEA", "https://media.api-sports.io/football/leagues/78.png"),
        League("61", "Ligue 1", "France", "\uD83C\uDDEB\uD83C\uDDF7", "https://media.api-sports.io/football/leagues/61.png"),
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
                _state.update { it.copy(usernameStatus = UsernameStatus.Checking) }
                usernameCheckJob = viewModelScope.launch {
                    delay(600)
                    try {
                        val localUsername = onboardingRepository.getUsernamePref()
                        val isOwnUsername = !localUsername.isNullOrBlank() && clean.equals(localUsername, ignoreCase = true)
                        val available = onboardingRepository.checkUsernameAvailability(clean)
                        val isOwnDevice = !available && !isOwnUsername && onboardingRepository.isUsernameOwnedByCurrentDevice(clean)

                        when {
                            available || isOwnUsername || isOwnDevice -> {
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

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                onboardingRepository.signInAndRegisterUsername(status.username)
                _state.update { it.copy(isLoading = false) }
                _events.emit(OnboardingEvent.NavigateToLeague)
                _navigationEvent.send(NavigationEvent.GoToLeagueScreen)
            } catch (e: UsernameTakenException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        usernameStatus = UsernameStatus.Taken(status.username),
                        suggestions = generateSuggestions(status.username),
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
                        addedAt = System.currentTimeMillis()
                    )
                }
                onboardingRepository.completeOnboarding(
                    uid = uid,
                    leagueId = league.id,
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
        viewModelScope.launch {
            val entities = clubs.map { club ->
                com.footballpluse.footballapp.data.local.db.FavoriteClubEntity(
                    clubId = club.id,
                    clubName = club.name,
                    leagueId = club.leagueId,
                    addedAt = System.currentTimeMillis()
                )
            }
            try {
                val uid = onboardingRepository.getUidPref() ?: ""
                val leagueId = _state.value.selectedLeague?.id ?: return@launch
                onboardingRepository.saveSelectedClubsOnly(uid, leagueId, entities)
            } catch (_: Exception) { }
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
            "39" to listOf(
                Club("50", "Manchester City", "39", "https://media.api-sports.io/football/teams/50.png", 1),
                Club("42", "Arsenal", "39", "https://media.api-sports.io/football/teams/42.png", 2),
                Club("40", "Liverpool", "39", "https://media.api-sports.io/football/teams/40.png", 3),
                Club("49", "Chelsea", "39", "https://media.api-sports.io/football/teams/49.png", 4),
                Club("33", "Manchester United", "39", "https://media.api-sports.io/football/teams/33.png", 5),
                Club("47", "Tottenham", "39", "https://media.api-sports.io/football/teams/47.png", 6),
                Club("48", "West Ham", "39", "https://media.api-sports.io/football/teams/48.png", 7),
                Club("52", "Aston Villa", "39", "https://media.api-sports.io/football/teams/52.png", 8),
                Club("66", "Newcastle", "39", "https://media.api-sports.io/football/teams/66.png", 9),
                Club("36", "Brighton", "39", "https://media.api-sports.io/football/teams/36.png", 10),
                Club("38", "Crystal Palace", "39", "https://media.api-sports.io/football/teams/38.png", 11),
                Club("39", "Wolves", "39", "https://media.api-sports.io/football/teams/39.png", 12),
                Club("45", "Everton", "39", "https://media.api-sports.io/football/teams/45.png", 13),
                Club("54", "Fulham", "39", "https://media.api-sports.io/football/teams/54.png", 14),
                Club("55", "Brentford", "39", "https://media.api-sports.io/football/teams/55.png", 15),
                Club("34", "Nottingham Forest", "39", "https://media.api-sports.io/football/teams/34.png", 16),
                Club("35", "Bournemouth", "39", "https://media.api-sports.io/football/teams/35.png", 17),
                Club("41", "Southampton", "39", "https://media.api-sports.io/football/teams/41.png", 18),
                Club("46", "Leicester City", "39", "https://media.api-sports.io/football/teams/46.png", 19),
                Club("56", "Ipswich Town", "39", "https://media.api-sports.io/football/teams/56.png", 20),
            ),
            "140" to listOf(
                Club("541", "Real Madrid", "140", "https://media.api-sports.io/football/teams/541.png", 1),
                Club("529", "Barcelona", "140", "https://media.api-sports.io/football/teams/529.png", 2),
                Club("530", "Atletico Madrid", "140", "https://media.api-sports.io/football/teams/530.png", 3),
                Club("536", "Sevilla", "140", "https://media.api-sports.io/football/teams/536.png", 4),
                Club("532", "Valencia", "140", "https://media.api-sports.io/football/teams/532.png", 5),
                Club("531", "Athletic Bilbao", "140", "https://media.api-sports.io/football/teams/531.png", 6),
                Club("533", "Villarreal", "140", "https://media.api-sports.io/football/teams/533.png", 7),
                Club("548", "Real Betis", "140", "https://media.api-sports.io/football/teams/548.png", 8),
                Club("547", "Real Sociedad", "140", "https://media.api-sports.io/football/teams/547.png", 9),
                Club("534", "Getafe", "140", "https://media.api-sports.io/football/teams/534.png", 10),
                Club("538", "Celta Vigo", "140", "https://media.api-sports.io/football/teams/538.png", 11),
                Club("540", "Mallorca", "140", "https://media.api-sports.io/football/teams/540.png", 12),
                Club("543", "Osasuna", "140", "https://media.api-sports.io/football/teams/543.png", 13),
                Club("535", "Rayo Vallecano", "140", "https://media.api-sports.io/football/teams/535.png", 14),
                Club("546", "Girona", "140", "https://media.api-sports.io/football/teams/546.png", 15),
                Club("542", "Las Palmas", "140", "https://media.api-sports.io/football/teams/542.png", 16),
                Club("549", "Alaves", "140", "https://media.api-sports.io/football/teams/549.png", 17),
                Club("544", "Espanyol", "140", "https://media.api-sports.io/football/teams/544.png", 18),
                Club("550", "Valladolid", "140", "https://media.api-sports.io/football/teams/550.png", 19),
                Club("545", "Leganes", "140", "https://media.api-sports.io/football/teams/545.png", 20),
            ),
            "135" to listOf(
                Club("505", "Inter Milan", "135", "https://media.api-sports.io/football/teams/505.png", 1),
                Club("496", "Juventus", "135", "https://media.api-sports.io/football/teams/496.png", 2),
                Club("489", "AC Milan", "135", "https://media.api-sports.io/football/teams/489.png", 3),
                Club("492", "Napoli", "135", "https://media.api-sports.io/football/teams/492.png", 4),
                Club("497", "Roma", "135", "https://media.api-sports.io/football/teams/497.png", 5),
                Club("487", "Lazio", "135", "https://media.api-sports.io/football/teams/487.png", 6),
                Club("499", "Atalanta", "135", "https://media.api-sports.io/football/teams/499.png", 7),
                Club("502", "Fiorentina", "135", "https://media.api-sports.io/football/teams/502.png", 8),
                Club("500", "Bologna", "135", "https://media.api-sports.io/football/teams/500.png", 9),
                Club("495", "Torino", "135", "https://media.api-sports.io/football/teams/495.png", 10),
                Club("494", "Udinese", "135", "https://media.api-sports.io/football/teams/494.png", 11),
                Club("510", "Monza", "135", "https://media.api-sports.io/football/teams/510.png", 12),
                Club("504", "Lecce", "135", "https://media.api-sports.io/football/teams/504.png", 13),
                Club("493", "Verona", "135", "https://media.api-sports.io/football/teams/493.png", 14),
                Club("488", "Cagliari", "135", "https://media.api-sports.io/football/teams/488.png", 15),
                Club("490", "Empoli", "135", "https://media.api-sports.io/football/teams/490.png", 16),
                Club("511", "Parma", "135", "https://media.api-sports.io/football/teams/511.png", 17),
                Club("503", "Venezia", "135", "https://media.api-sports.io/football/teams/503.png", 18),
                Club("520", "Como", "135", "https://media.api-sports.io/football/teams/520.png", 19),
                Club("491", "Genoa", "135", "https://media.api-sports.io/football/teams/491.png", 20),
            ),
            "78" to listOf(
                Club("157", "Bayern Munich", "78", "https://media.api-sports.io/football/teams/157.png", 1),
                Club("165", "Borussia Dortmund", "78", "https://media.api-sports.io/football/teams/165.png", 2),
                Club("173", "RB Leipzig", "78", "https://media.api-sports.io/football/teams/173.png", 3),
                Club("168", "Bayer Leverkusen", "78", "https://media.api-sports.io/football/teams/168.png", 4),
                Club("169", "Eintracht Frankfurt", "78", "https://media.api-sports.io/football/teams/169.png", 5),
                Club("161", "Wolfsburg", "78", "https://media.api-sports.io/football/teams/161.png", 6),
                Club("163", "Borussia Monchengladbach", "78", "https://media.api-sports.io/football/teams/163.png", 7),
                Club("172", "Stuttgart", "78", "https://media.api-sports.io/football/teams/172.png", 8),
                Club("160", "Freiburg", "78", "https://media.api-sports.io/football/teams/160.png", 9),
                Club("167", "Hoffenheim", "78", "https://media.api-sports.io/football/teams/167.png", 10),
                Club("154", "Mainz 05", "78", "https://media.api-sports.io/football/teams/154.png", 11),
                Club("158", "Augsburg", "78", "https://media.api-sports.io/football/teams/158.png", 12),
                Club("162", "Werder Bremen", "78", "https://media.api-sports.io/football/teams/162.png", 13),
                Club("155", "Union Berlin", "78", "https://media.api-sports.io/football/teams/155.png", 14),
                Club("156", "Bochum", "78", "https://media.api-sports.io/football/teams/156.png", 15),
                Club("176", "Heidenheim", "78", "https://media.api-sports.io/football/teams/176.png", 16),
                Club("179", "St. Pauli", "78", "https://media.api-sports.io/football/teams/179.png", 17),
                Club("188", "Holstein Kiel", "78", "https://media.api-sports.io/football/teams/188.png", 18),
            ),
            "61" to listOf(
                Club("85", "PSG", "61", "https://media.api-sports.io/football/teams/85.png", 1),
                Club("81", "Marseille", "61", "https://media.api-sports.io/football/teams/81.png", 2),
                Club("91", "Monaco", "61", "https://media.api-sports.io/football/teams/91.png", 3),
                Club("80", "Lyon", "61", "https://media.api-sports.io/football/teams/80.png", 4),
                Club("79", "Lille", "61", "https://media.api-sports.io/football/teams/79.png", 5),
                Club("84", "Nice", "61", "https://media.api-sports.io/football/teams/84.png", 6),
                Club("94", "Rennes", "61", "https://media.api-sports.io/football/teams/94.png", 7),
                Club("83", "Lens", "61", "https://media.api-sports.io/football/teams/83.png", 8),
                Club("98", "Toulouse", "61", "https://media.api-sports.io/football/teams/98.png", 9),
                Club("95", "Brest", "61", "https://media.api-sports.io/football/teams/95.png", 10),
                Club("82", "Strasbourg", "61", "https://media.api-sports.io/football/teams/82.png", 11),
                Club("92", "Montpellier", "61", "https://media.api-sports.io/football/teams/92.png", 12),
                Club("93", "Nantes", "61", "https://media.api-sports.io/football/teams/93.png", 13),
                Club("99", "Reims", "61", "https://media.api-sports.io/football/teams/99.png", 14),
                Club("97", "Le Havre", "61", "https://media.api-sports.io/football/teams/97.png", 15),
                Club("87", "Angers", "61", "https://media.api-sports.io/football/teams/87.png", 16),
                Club("89", "Auxerre", "61", "https://media.api-sports.io/football/teams/89.png", 17),
                Club("86", "Saint-Etienne", "61", "https://media.api-sports.io/football/teams/86.png", 18),
            ),
        )
    }
}
