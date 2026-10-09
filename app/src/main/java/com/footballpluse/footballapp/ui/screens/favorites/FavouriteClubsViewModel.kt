package com.footballpluse.footballapp.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.repository.BillingRepository
import com.footballpluse.footballapp.data.repository.CommunityFirebaseRepository
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import com.footballpluse.footballapp.data.repository.LocalRepository
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.domain.model.FavouriteClub
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.domain.model.StandingItem
import com.footballpluse.footballapp.domain.model.TeamDetail
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val id: String,
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
    val socialPosts: List<SocialPost> = emptyList(),
    val hasPostedToday: Boolean = false,
    val postErrorMessage: String? = null
)

@HiltViewModel
class FavouriteClubsViewModel @Inject constructor(
    private val favouriteRepository: FavouriteRepository,
    private val footballRepository: FootballRepository,
    private val billingRepository: BillingRepository,
    private val communityRepository: CommunityFirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavouriteClubsUiState())
    val uiState: StateFlow<FavouriteClubsUiState> = _uiState.asStateFlow()
    private val cache = ClubDetailCache()
    private var communityJob: Job? = null

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
                        "No fixtures found for ${club.clubName} (${
                            SeasonUtils.displaySeasonLabel(
                                loadedSeason
                            )
                        }). " +
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
                        val teamResult = footballRepository.getFixturesByTeamSeasonLeague(
                            teamId,
                            leagueId,
                            season
                        )
                        val direct = when (teamResult) {
                            is ApiResult.Success -> teamResult.data
                            else -> emptyList()
                        }
                        direct.ifEmpty {
                            val flow =
                                footballRepository.getFixturesByLeagueSeason(leagueId, season)
                            val first = flow.first { it !is ApiResult.Loading }
                            when (first) {
                                is ApiResult.Success -> {
                                    val clubNorm =
                                        com.footballpluse.footballapp.data.remote.uitslagen.UitslagenMapper.normalize(
                                            club.clubName
                                        )
                                    first.data.filter { m ->
                                        m.homeTeam.id == teamId || m.awayTeam.id == teamId ||
                                                (clubNorm.isNotBlank() && (
                                                        com.footballpluse.footballapp.data.remote.uitslagen.UitslagenMapper.normalize(
                                                            m.homeTeam.name
                                                        ).contains(clubNorm) ||
                                                                clubNorm.contains(
                                                                    com.footballpluse.footballapp.data.remote.uitslagen.UitslagenMapper.normalize(
                                                                        m.homeTeam.name
                                                                    )
                                                                ) ||
                                                                com.footballpluse.footballapp.data.remote.uitslagen.UitslagenMapper.normalize(
                                                                    m.awayTeam.name
                                                                ).contains(clubNorm) ||
                                                                clubNorm.contains(
                                                                    com.footballpluse.footballapp.data.remote.uitslagen.UitslagenMapper.normalize(
                                                                        m.awayTeam.name
                                                                    )
                                                                )
                                                        ))
                                    }
                                }

                                is ApiResult.Error -> throw IllegalStateException(first.message)
                                ApiResult.Loading -> emptyList()
                            }
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

    private fun loadCommunityData(clubId: Int) {
        communityJob?.cancel()
        communityJob = viewModelScope.launch {
            val myUid = communityRepository.getCurrentUserId()
            val startOfDay = java.util.Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            communityRepository.observeClubPosts(clubId).collectLatest { fbPosts ->
                val userPostedToday =
                    myUid != null && fbPosts.any { it.userId == myUid && it.timestamp >= startOfDay }

                _uiState.value = _uiState.value.copy(
                    hasPostedToday = userPostedToday,
                    socialPosts = fbPosts.map { post ->
                        val options = post.pollOptions.mapIndexed { idx, optText ->
                            val votes = post.pollVotes.getOrNull(idx) ?: 0
                            PollOption(text = optText, votes = votes)
                        }
                        val userVote = if (myUid != null) post.voters[myUid] else null
                        val hasLiked = (myUid != null && post.likedBy.contains(myUid))

                        SocialPost(
                            id = post.id,
                            username = post.username,
                            userAvatarUrl = post.userAvatarUrl,
                            timestamp = post.timestamp,
                            content = post.content,
                            imageUrl = post.imageUrl,
                            hotTakeTag = post.hotTakeTag,
                            pollQuestion = post.pollQuestion,
                            pollOptions = options,
                            userVotedIndex = userVote,
                            likes = post.likes,
                            hasLiked = hasLiked,
                            commentCount = post.commentCount
                        )
                    }
                )
            }
        }
    }

    fun togglePremium() {
        viewModelScope.launch {
            val nextState = !_uiState.value.isPremium
            billingRepository.setPurchased(nextState)
        }
    }

    private fun getDisplayName(): String {
        return localRepository.getUsernamePref()?.takeIf { it.isNotBlank() } ?: "Fan"
    }

    fun clearPostError() {
        _uiState.value = _uiState.value.copy(postErrorMessage = null)
    }

    fun createSocialPost(content: String, hotTakeTag: String?, pollOptions: List<String>?) {
        val activeClub = _uiState.value.activeClub ?: return
        if (content.isBlank()) return

        if (_uiState.value.hasPostedToday) {
            _uiState.value = _uiState.value.copy(
                postErrorMessage = "Daily limit reached: You can only post once per day in this club's community."
            )
            return
        }

        val username = getDisplayName()
        viewModelScope.launch {
            val res = communityRepository.createClubPost(
                clubId = activeClub.clubId,
                content = content,
                username = username,
                hotTakeTag = hotTakeTag,
                pollOptions = pollOptions
            )
            res.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    postErrorMessage = error.message ?: "Failed to post message. Please try again."
                )
            }
        }
    }

    fun likePost(postId: String) {
        val activeClub = _uiState.value.activeClub ?: return
        viewModelScope.launch {
            communityRepository.toggleLikePost(
                clubId = activeClub.clubId,
                postId = postId
            )
        }
    }

    fun voteInPoll(postId: String, optionIndex: Int) {
        val activeClub = _uiState.value.activeClub ?: return
        viewModelScope.launch {
            communityRepository.voteInPoll(
                clubId = activeClub.clubId,
                postId = postId,
                optionIndex = optionIndex
            )
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
