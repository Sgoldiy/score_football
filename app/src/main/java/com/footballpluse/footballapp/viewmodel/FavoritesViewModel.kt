package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import com.footballpluse.footballapp.data.repository.FixturesRepository
import com.footballpluse.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamWithNextFixture(
    val teamId: Int,
    val teamName: String,
    val badgeUrl: String,
    val opponentName: String?,
    val kickoffTime: String?,
    val isLive: Boolean,
    val liveMinute: String?
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favouriteRepository: FavouriteRepository,
    private val fixturesRepository: FixturesRepository
) : ViewModel() {

    private val _favourites = MutableStateFlow<List<TeamWithNextFixture>>(emptyList())
    val favourites: StateFlow<List<TeamWithNextFixture>> = _favourites.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            favouriteRepository.getFavouriteClubs()
                .collectLatest { clubs ->
                    if (clubs.isEmpty()) {
                        _favourites.value = emptyList()
                        return@collectLatest
                    }
                    
                    try {
                        val teamWithNextFixtures = clubs.map { club ->
                            async {
                                val result = fixturesRepository.getNextFixtureForTeam(club.clubId, 1)
                                val nextFixture = when (result) {
                                    is ApiResult.Success -> result.data
                                    else -> null
                                }
                                
                                val isLive = nextFixture?.fixture?.status?.short in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
                                val opponentName = if (nextFixture != null) {
                                    if (nextFixture.teams?.home?.id == club.clubId) {
                                        nextFixture.teams.away?.name
                                    } else {
                                        nextFixture.teams?.home?.name
                                    }
                                } else null
                                
                                val kickoffTime = nextFixture?.fixture?.timestamp?.let { ts ->
                                    try {
                                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                            .format(java.util.Date(ts * 1000L))
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                
                                val liveMinute = nextFixture?.fixture?.status?.elapsed?.toString()
                                
                                TeamWithNextFixture(
                                    teamId = club.clubId,
                                    teamName = club.clubName,
                                    badgeUrl = club.logoUrl,
                                    opponentName = opponentName,
                                    kickoffTime = kickoffTime,
                                    isLive = isLive,
                                    liveMinute = liveMinute
                                )
                            }
                        }.awaitAll()
                        
                        _favourites.value = teamWithNextFixtures
                    } catch (e: Exception) {
                        _favourites.value = clubs.map {
                            TeamWithNextFixture(
                                teamId = it.clubId,
                                teamName = it.clubName,
                                badgeUrl = it.logoUrl,
                                opponentName = null,
                                kickoffTime = null,
                                isLive = false,
                                liveMinute = null
                            )
                        }
                    }
                }
        }
    }
}
