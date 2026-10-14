package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.data.util.UiState
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import com.footballpluse.footballapp.domain.model.FavouriteClub
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject


@HiltViewModel
class ClubInfoViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val favouriteRepository: FavouriteRepository
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private var currentTeamId: Int = 0
    private var currentLeagueId: Int = 0

    init {
        viewModelScope.launch {
            favouriteRepository.getFavouriteClubs().collectLatest { favs ->
                _isFavorite.value = favs.any { it.clubId == currentTeamId }
            }
        }
    }

    fun toggleFavoriteClub() {
        viewModelScope.launch {
            val team = (_teamInfo.value as? UiState.Success)?.data?.team
            val detail = (_teamDetail.value as? ApiResult.Success)?.data
            val clubName = team?.name ?: detail?.info?.name ?: "Club"
            val clubLogo = team?.logo ?: detail?.info?.logo ?: ""
            
            if (_isFavorite.value) {
                favouriteRepository.removeFavouriteClub(currentTeamId)
            } else {
                favouriteRepository.addFavouriteClub(
                    FavouriteClub(
                        clubId = currentTeamId,
                        clubName = clubName,
                        leagueId = currentLeagueId,
                        leagueName = detail?.info?.country ?: "League",
                        logoUrl = clubLogo,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private val _teamInfo = MutableStateFlow<UiState<TeamInfoResponse>>(UiState.Loading)
    val teamInfo: StateFlow<UiState<TeamInfoResponse>> = _teamInfo

    private val _teamStats = MutableStateFlow<UiState<TeamStatistics>>(UiState.Loading)
    val teamStats: StateFlow<UiState<TeamStatistics>> = _teamStats

    private val _teamDetail = MutableStateFlow<ApiResult<com.footballpluse.footballapp.domain.model.TeamDetail>>(ApiResult.Loading)
    val teamDetail: StateFlow<ApiResult<com.footballpluse.footballapp.domain.model.TeamDetail>> = _teamDetail

    private val _squad = MutableStateFlow<UiState<List<SquadResponse>>>(UiState.Loading)
    val squad: StateFlow<UiState<List<SquadResponse>>> = _squad

    private val _coach = MutableStateFlow<UiState<List<Coach>>>(UiState.Loading)
    val coach: StateFlow<UiState<List<Coach>>> = _coach

    private val _recentFixtures = MutableStateFlow<UiState<List<FixtureResponse>>>(UiState.Loading)
    val recentFixtures: StateFlow<UiState<List<FixtureResponse>>> = _recentFixtures

    private val _topScorers = MutableStateFlow<UiState<List<PlayerProfileStatisticsResponse>>>(UiState.Loading)
    val topScorers: StateFlow<UiState<List<PlayerProfileStatisticsResponse>>> = _topScorers

    fun loadClubData(teamId: Int, leagueId: Int) {
        currentTeamId = teamId
        currentLeagueId = leagueId
        viewModelScope.launch {
            launch { fetchTeamDetail(teamId, leagueId) }
            launch { fetchTeamInfo(teamId) }
            launch { fetchTeamStats(teamId, leagueId) }
            launch { fetchSquad(teamId) }
            launch { fetchCoach(teamId) }
            launch { fetchRecentFixtures(teamId, leagueId) }
            launch { fetchTopScorers(leagueId) }
        }
    }

    private suspend fun fetchTeamDetail(teamId: Int, leagueId: Int) {
        try {
            val season = com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            val result = repository.getTeamDetail(teamId, leagueId, season)
            _teamDetail.value = result
        } catch (e: Exception) {
            _teamDetail.value = ApiResult.Error(e.message ?: "Failed to load team details")
        }
    }


    private suspend fun fetchTeamInfo(teamId: Int) {
        try {
            val info = repository.getTeamInfoDirect(teamId)
            _teamInfo.value = UiState.Success(info)
        } catch (e: Exception) {
            _teamInfo.value = UiState.Error(e.message ?: "Error loading team info")
        }
    }

    private suspend fun fetchTeamStats(teamId: Int, leagueId: Int) {
        try {
            val stats = repository.getTeamStatisticsDirect(
                teamId, leagueId,
                com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            )
            _teamStats.value = UiState.Success(stats)
        } catch (e: Exception) {
            _teamStats.value = UiState.Error(e.message ?: "Error loading team stats")
        }
    }

    private suspend fun fetchSquad(teamId: Int) {
        try {
            val squadList = repository.getTeamSquadDirect(teamId)
            _squad.value = UiState.Success(squadList)
        } catch (e: Exception) {
            _squad.value = UiState.Error(e.message ?: "Error loading squad")
        }
    }

    private suspend fun fetchCoach(teamId: Int) {
        try {
            val coaches = repository.getTeamCoachesDirect(teamId)
            _coach.value = UiState.Success(coaches)
        } catch (e: Exception) {
            _coach.value = UiState.Error(e.message ?: "Error loading coach")
        }
    }

    private suspend fun fetchRecentFixtures(teamId: Int, leagueId: Int) {
        try {
            val fixtures = repository.getRecentFixturesDirect(
                teamId, leagueId,
                com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            )
            _recentFixtures.value = UiState.Success(fixtures)
        } catch (e: Exception) {
            _recentFixtures.value = UiState.Error(e.message ?: "Error loading fixtures")
        }
    }

    private suspend fun fetchTopScorers(leagueId: Int) {
        try {
            val scorers = repository.getTopScorersDirect(
                leagueId,
                com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            )
            _topScorers.value = UiState.Success(scorers.take(5))
        } catch (e: Exception) {
            _topScorers.value = UiState.Error(e.message ?: "Error loading top scorers")
        }
    }
}
