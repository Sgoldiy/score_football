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
    private val onboardingRepository: OnboardingRepository,
    private val apiService: com.footballpluse.footballapp.data.remote.ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<NavigationEvent> = _navigationEvent.receiveAsFlow()

    private var usernameCheckJob: Job? = null

    /** Leagues whose saved selections have already been re-applied. */
    private val restoredLeagueIds = mutableSetOf<String>()

    val defaultLeagues: List<League> = listOf(
        // League ids are the app's legacy ids, mapped to the upstream in
        // UitslagenLeagues. Logos are null: the old badge CDN is dead, and
        // the UIs fall back to their league icon/initials.
        League("152", "Premier League", "England", "\uD83C\uDFF4\uD83C\uDFE6\uD83C\uDFFD\u200D\uD83C\uDFF3\uFE0F\u200D\uD83C\uDFF4"),
        League("302", "La Liga", "Spain", "\uD83C\uDDEA\uD83C\uDDF8"),
        League("207", "Serie A", "Italy", "\uD83C\uDDEE\uD83C\uDDF9"),
        League("175", "Bundesliga", "Germany", "\uD83C\uDDE9\uD83C\uDDEA"),
        League("168", "Ligue 1", "France", "\uD83C\uDDEB\uD83C\uDDF7"),
    )

    init {
        viewModelScope.launch {
            onboardingRepository.getProfileFlow().collect { profile ->
                if (profile != null && profile.username.isNotBlank()) {
                    _state.update { it.copy(username = profile.displayUsername) }
                }
            }
        }

        val savedLeague = onboardingRepository.getOnboardingLeagueProgress()
        if (savedLeague != null) {
            val league = defaultLeagues.find { it.id == savedLeague }
            if (league != null) {
                _state.update { it.copy(selectedLeague = league) }
            }
        }

        // Live current-season teams for all top-5 leagues (real ids, names,
        // logos from the standings). ensureLeagueTeams dedupes per league.
        defaultLeagues.forEach { ensureLeagueTeams(it.id) }
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

    // --- Live team lists (current-season standings from the upstream) ---

    /**
     * Fetches the current-season teams of a top-5 league from the live
     * upstream (real team ids + names + logo urls). Replaces the stale
     * hardcoded apiv3 lists. Failures mark the league as unavailable so the
     * UI can say so instead of silently showing nothing.
     */
    fun ensureLeagueTeams(leagueId: String) {
        if (_state.value.clubsByLeague.containsKey(leagueId) ||
            leagueId in _state.value.loadingLeagues
        ) return
        _state.update { it.copy(loadingLeagues = it.loadingLeagues + leagueId) }
        viewModelScope.launch {
            val teams: List<Club> = try {
                apiService.getStandings(leagueId).mapNotNull { s ->
                    val id = s.team_id?.toIntOrNull() ?: return@mapNotNull null
                    val name = s.team_name ?: return@mapNotNull null
                    Club(
                        id = id.toString(),
                        name = name,
                        leagueId = leagueId,
                        logoUrl = s.team_badge?.takeIf { it.isNotBlank() }
                    )
                }.sortedBy { it.name.lowercase() }
            } catch (_: Exception) {
                // Stored empty so the UI can say the league is unavailable
                // instead of spinning forever.
                emptyList()
            }
            _state.update {
                it.copy(
                    clubsByLeague = it.clubsByLeague + (leagueId to teams),
                    loadingLeagues = it.loadingLeagues - leagueId
                )
            }

            // Once per league: re-select previously saved clubs that are in
            // the current-season list (restores in-progress onboarding and
            // the edit-clubs flow after process death).
            val savedIds = onboardingRepository.getOnboardingClubProgress()
            if (teams.isNotEmpty() && savedIds.isNotEmpty() && leagueId !in restoredLeagueIds) {
                restoredLeagueIds.add(leagueId)
                _state.update { state ->
                    val toAdd = teams.filter { it.id in savedIds }
                        .filter { added -> state.selectedClubs.none { it.id == added.id } }
                    state.copy(selectedClubs = state.selectedClubs + toAdd)
                }
            }
        }
    }

    fun getClubsForLeague(leagueId: String): List<Club> =
        _state.value.clubsByLeague[leagueId].orEmpty()

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

}
