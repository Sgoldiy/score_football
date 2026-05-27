package com.footballpluse.footballapp.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.local.OnboardingDataStore
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.domain.model.StandingItem
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class OnboardingState(
    val step: Int = 1,
    val topLeagues: List<LeagueInfo> = emptyList(),
    val selectedLeague: LeagueInfo? = null,
    val leagueClubs: List<ClubItem> = emptyList(),
    val allClubs: List<ClubItem> = emptyList(),
    val primaryClub: ClubItem? = null,
    val followedClubIds: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ClubItem(
    val id: Int,
    val name: String,
    val crestUrl: String?,
    val leagueId: Int,
    val leagueName: String = ""
)

private val TOP_5_LEAGUE_IDS = setOf(39, 140, 135, 78, 61)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val dataStore: OnboardingDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        loadTopLeagues()
    }

    fun loadTopLeagues() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getLeagues()) {
                is ApiResult.Success -> {
                    val filtered = result.data.filter { it.id in TOP_5_LEAGUE_IDS }
                    _state.update { it.copy(topLeagues = filtered, isLoading = false) }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun selectLeague(league: LeagueInfo) {
        _state.update { it.copy(selectedLeague = league) }
    }

    fun loadLeagueClubs() {
        val league = _state.value.selectedLeague ?: return
        val season = league.season ?: Calendar.getInstance().get(Calendar.YEAR)

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.getStandings(league.id, season).first().let { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val clubs = result.data.take(5).map { standing ->
                            ClubItem(
                                id = standing.team.id,
                                name = standing.team.name,
                                crestUrl = standing.team.logo,
                                leagueId = league.id,
                                leagueName = league.name
                            )
                        }
                        _state.update { it.copy(leagueClubs = clubs, isLoading = false) }
                    }
                    is ApiResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is ApiResult.Loading -> {}
                }
            }
        }
    }

    fun setPrimaryClub(club: ClubItem) {
        _state.update {
            it.copy(
                primaryClub = club,
                followedClubIds = setOf(club.id)
            )
        }
    }

    fun loadAllClubs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val leagues = _state.value.topLeagues
            val season = Calendar.getInstance().get(Calendar.YEAR)

            val deferred = leagues.map { league ->
                async {
                    try {
                        val flow = repository.getStandings(league.id, season)
                        val result = flow.first()
                        if (result is ApiResult.Success) {
                            result.data.take(5).map { standing ->
                                ClubItem(
                                    id = standing.team.id,
                                    name = standing.team.name,
                                    crestUrl = standing.team.logo,
                                    leagueId = league.id,
                                    leagueName = league.name
                                )
                            }
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }

            val allClubs = deferred.flatMap { it.await() }
            _state.update { it.copy(allClubs = allClubs, isLoading = false) }
        }
    }

    fun toggleFollowClub(clubId: Int) {
        val current = _state.value.followedClubIds
        val primaryId = _state.value.primaryClub?.id
        if (clubId == primaryId) return

        _state.update {
            if (current.contains(clubId)) {
                it.copy(followedClubIds = current - clubId)
            } else if (current.size < 10) {
                it.copy(followedClubIds = current + clubId)
            } else it
        }
    }

    fun isClubLocked(clubId: Int): Boolean =
        clubId == _state.value.primaryClub?.id

    fun isMaxReached(): Boolean =
        _state.value.followedClubIds.size >= 10

    fun goToStep(step: Int) {
        _state.update { it.copy(step = step) }
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            dataStore.saveOnboardingCompleted(true)
            dataStore.saveOnboardingResult(
                state.value.selectedLeague,
                state.value.primaryClub,
                state.value.followedClubIds
            )
            onDone()
        }
    }
}

data class OnboardingResult(
    val league: LeagueInfo?,
    val primaryClub: ClubItem?,
    val followedClubIds: Set<Int>
)
