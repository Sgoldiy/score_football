package com.footballpluse.footballapp.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import com.footballpluse.footballapp.data.repository.BillingRepository
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.domain.model.*
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClubDetailUiState(
    val isLoading: Boolean = false,
    val loadedSeason: Int = 2025,

    val fixtures: List<Match> = emptyList(),
    val fixturesError: String? = null,

    val teamDetail: TeamDetail? = null,
    val teamDetailError: String? = null
)

data class PollOption(
    val text: String,
    val votes: Int
)

data class SocialPost(
    val id: Int,
    val username: String,
    val userAvatarUrl: String?,
    val timestamp: Long,
    val content: String,
    val imageUrl: String? = null,
    val hotTakeTag: String? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<PollOption> = emptyList(),
    val userVotedIndex: Int? = null,
    val likes: Int = 0,
    val hasLiked: Boolean = false,
    val commentCount: Int = 0
)

data class FavouriteClubsUiState(
    val clubs: List<FavouriteClub> = emptyList(),
    val activeClubId: Int? = null,
    val activeClub: FavouriteClub? = null,
    val detail: ClubDetailUiState = ClubDetailUiState(),
    val isPremium: Boolean = false,
    val socialPosts: List<SocialPost> = emptyList()
)

@HiltViewModel
class FavouriteClubsViewModel @Inject constructor(
    private val favouriteRepository: FavouriteRepository,
    private val footballRepository: FootballRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavouriteClubsUiState())
    val uiState: StateFlow<FavouriteClubsUiState> = _uiState.asStateFlow()

    private val cache = ClubDetailCache()

    init {
        viewModelScope.launch {
            favouriteRepository.getFavouriteClubs().collectLatest { clubs ->
                val active = _uiState.value.activeClubId ?: clubs.firstOrNull()?.clubId
                val activeClub = clubs.firstOrNull { it.clubId == active } ?: clubs.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    clubs = clubs,
                    activeClubId = activeClub?.clubId,
                    activeClub = activeClub
                )
                if (activeClub != null) {
                    loadClub(activeClub)
                    loadCommunityData(activeClub.clubId)
                } else {
                    _uiState.value = _uiState.value.copy(detail = ClubDetailUiState())
                }
            }
        }

        viewModelScope.launch {
            billingRepository.isPurchased.collectLatest { purchased ->
                _uiState.value = _uiState.value.copy(isPremium = purchased)
            }
        }
    }

    fun setActiveClub(clubId: Int) {
        val club = _uiState.value.clubs.firstOrNull { it.clubId == clubId } ?: return
        _uiState.value = _uiState.value.copy(activeClubId = clubId, activeClub = club)
        loadClub(club)
        loadCommunityData(clubId)
    }

    fun retry() {
        _uiState.value.activeClub?.let { loadClub(it, force = true) }
    }

    private fun loadClub(club: FavouriteClub, force: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                detail = ClubDetailUiState(
                    isLoading = true
                )
            )

            val currentSeason = SeasonUtils.currentSeasonStartYear()
            var (fixtures, detail) = fetchClubDataForSeason(club, currentSeason, force)
            var loadedSeason = currentSeason

            // Fallback to the previous season when the current one has no data yet
            // (pre-season, cups not drawn, or quota exhausted during first fetch).
            if (fixtures.isEmpty() && (detail == null || detail.squad.isEmpty())) {
                val backup = fetchClubDataForSeason(club, currentSeason - 1, force)
                if (backup.first.isNotEmpty() || (backup.second != null && backup.second!!.squad.isNotEmpty())) {
                    fixtures = backup.first
                    detail = backup.second
                    loadedSeason = currentSeason - 1
                }
            }

            // No fabricated fixtures/squads: when the API returns nothing we surface an
            // honest error state instead of pretending Chelsea/Real Madrid lineups.
            _uiState.value = _uiState.value.copy(
                detail = ClubDetailUiState(
                    isLoading = false,
                    loadedSeason = loadedSeason,
                    fixtures = fixtures.sortedBy { it.timestamp },
                    fixturesError = if (fixtures.isEmpty())
                        "No fixtures found for ${club.clubName} (${SeasonUtils.displaySeasonLabel(loadedSeason)}). " +
                            "The data feed may be rate-limited — tap refresh to retry."
                    else null,
                    teamDetail = detail,
                    teamDetailError = if (detail == null)
                        "Club info unavailable right now (data feed rate-limited). Tap refresh to retry."
                    else null
                )
            )
        }
    }

    private suspend fun fetchClubDataForSeason(
        club: FavouriteClub,
        season: Int,
        force: Boolean
    ): Pair<List<Match>, TeamDetail?> {
        val leagueId = club.leagueId
        val teamId = club.clubId

        return coroutineScope {
            val fixturesJob = async(Dispatchers.IO) {
                runCatching {
                    cache.getFixtures(teamId, leagueId, season, force) {
                        val teamFirst = footballRepository.getFixturesByTeamSeasonLeague(teamId, leagueId, season)
                        when (teamFirst) {
                            is ApiResult.Success -> teamFirst.data
                            is ApiResult.Error -> {
                                val flow = footballRepository.getFixturesByLeagueSeason(leagueId, season)
                                val first = flow.first { it !is ApiResult.Loading }
                                when (first) {
                                    is ApiResult.Success -> first.data.filter { m ->
                                        m.homeTeam.id == teamId || m.awayTeam.id == teamId
                                    }
                                    is ApiResult.Error -> throw IllegalStateException(first.message)
                                    ApiResult.Loading -> emptyList()
                                }
                            }
                            ApiResult.Loading -> emptyList()
                        }
                    }
                }.getOrDefault(emptyList())
            }

            val detailJob = async(Dispatchers.IO) {
                runCatching {
                    cache.getTeamDetail(teamId, leagueId, season, force) {
                        when (val td = footballRepository.getTeamDetail(teamId, leagueId, season)) {
                            is ApiResult.Success -> td.data
                            is ApiResult.Error -> throw IllegalStateException(td.message)
                            ApiResult.Loading -> throw IllegalStateException("Loading")
                        }
                    }
                }.getOrNull()
            }

            Pair(fixturesJob.await(), detailJob.await())
        }
    }

    private val clubPostCache = mutableMapOf<Int, List<SocialPost>>()

    private fun loadCommunityData(clubId: Int) {
        // Community is a local-only feature (the API has no social data).
        // It starts empty per club and only shows what the user has posted.
        _uiState.value = _uiState.value.copy(
            socialPosts = clubPostCache[clubId].orEmpty()
        )
    }

    fun togglePremium() {
        viewModelScope.launch {
            val nextState = !_uiState.value.isPremium
            billingRepository.setPurchased(nextState)
        }
    }

    fun createSocialPost(content: String, hotTakeTag: String?, pollOptions: List<String>?) {
        val currentPosts = _uiState.value.socialPosts.toMutableList()
        val optionsList = pollOptions?.filter { it.isNotBlank() }?.map { PollOption(it, 0) } ?: emptyList()
        val newPost = SocialPost(
            id = (currentPosts.maxOfOrNull { it.id } ?: 0) + 1,
            username = "You (Fan)",
            userAvatarUrl = null,
            timestamp = System.currentTimeMillis(),
            content = content,
            hotTakeTag = hotTakeTag,
            pollQuestion = if (optionsList.isNotEmpty()) "Poll" else null,
            pollOptions = optionsList,
            likes = 0,
            hasLiked = false,
            commentCount = 0
        )
        currentPosts.add(0, newPost)
        _uiState.value = _uiState.value.copy(socialPosts = currentPosts)
        _uiState.value.activeClub?.let { club ->
            clubPostCache[club.clubId] = currentPosts
        }
    }

    fun likePost(postId: Int) {
        val currentPosts = _uiState.value.socialPosts.map { post ->
            if (post.id == postId) {
                val nextLiked = !post.hasLiked
                post.copy(
                    hasLiked = nextLiked,
                    likes = if (nextLiked) post.likes + 1 else post.likes - 1
                )
            } else {
                post
            }
        }
        _uiState.value = _uiState.value.copy(socialPosts = currentPosts)
        _uiState.value.activeClub?.let { club ->
            clubPostCache[club.clubId] = currentPosts
        }
    }

    fun voteInPoll(postId: Int, optionIndex: Int) {
        val currentPosts = _uiState.value.socialPosts.map { post ->
            if (post.id == postId && post.userVotedIndex == null) {
                val updatedOptions = post.pollOptions.mapIndexed { idx, opt ->
                    if (idx == optionIndex) opt.copy(votes = opt.votes + 1) else opt
                }
                post.copy(
                    pollOptions = updatedOptions,
                    userVotedIndex = optionIndex
                )
            } else {
                post
            }
        }
        _uiState.value = _uiState.value.copy(socialPosts = currentPosts)
        _uiState.value.activeClub?.let { club ->
            clubPostCache[club.clubId] = currentPosts
        }
    }



}

private class ClubDetailCache {
    private data class Entry<T>(val value: T, val expiresAt: Long)
    private val fixtures = mutableMapOf<String, Entry<List<Match>>>()
    private val standings = mutableMapOf<String, Entry<List<StandingItem>>>()
    private val details = mutableMapOf<String, Entry<TeamDetail>>()

    suspend fun getFixtures(
        teamId: Int,
        leagueId: Int,
        season: Int,
        force: Boolean,
        loader: suspend () -> List<Match>
    ): List<Match> {
        val key = "$teamId:$leagueId:$season"
        val now = System.currentTimeMillis()
        val existing = fixtures[key]
        if (!force && existing != null && existing.expiresAt > now) return existing.value
        val loaded = loader()
        fixtures[key] = Entry(loaded, now + 5 * 60 * 1000L)
        return loaded
    }

    suspend fun getStandings(
        leagueId: Int,
        season: Int,
        force: Boolean,
        loader: suspend () -> List<StandingItem>
    ): List<StandingItem> {
        val key = "$leagueId:$season"
        val now = System.currentTimeMillis()
        val existing = standings[key]
        if (!force && existing != null && existing.expiresAt > now) return existing.value
        val loaded = loader()
        standings[key] = Entry(loaded, now + 5 * 60 * 1000L)
        return loaded
    }

    suspend fun getTeamDetail(
        teamId: Int,
        leagueId: Int,
        season: Int,
        force: Boolean,
        loader: suspend () -> TeamDetail
    ): TeamDetail {
        val key = "$teamId:$leagueId:$season"
        val now = System.currentTimeMillis()
        val existing = details[key]
        if (!force && existing != null && existing.expiresAt > now) return existing.value
        val loaded = loader()
        // TeamDetail includes squad/transfers/coaches/stats/venue; cache at 1h (squad) / 30m (transfers) tradeoff
        details[key] = Entry(loaded, now + 60 * 60 * 1000L)
        return loaded
    }
}
