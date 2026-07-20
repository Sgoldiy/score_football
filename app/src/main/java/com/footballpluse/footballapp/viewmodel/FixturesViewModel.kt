package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.domain.repository.FootballRepository
import com.footballpluse.footballapp.data.repository.TeamRepository
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FixturesViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val teamRepository: TeamRepository,
    private val favouriteRepository: FavouriteRepository
) : ViewModel() {

    private val _fixturesState = MutableStateFlow<ApiResult<List<Match>>>(ApiResult.Loading)
    val fixturesState: StateFlow<ApiResult<List<Match>>> = _fixturesState

    private val _formMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val formMap: StateFlow<Map<Int, String>> = _formMap.asStateFlow()

    private val _liveCount = MutableStateFlow(0)
    val liveCount: StateFlow<Int> = _liveCount.asStateFlow()

    private val _dateMatchCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dateMatchCounts: StateFlow<Map<String, Int>> = _dateMatchCounts.asStateFlow()

    private val _favouriteClubIds = MutableStateFlow<Set<Int>>(emptySet())
    val favouriteClubIds: StateFlow<Set<Int>> = _favouriteClubIds.asStateFlow()

    init {
        // Observe user's favorite clubs list
        viewModelScope.launch {
            favouriteRepository.getFavouriteClubs().collectLatest { clubs ->
                _favouriteClubIds.value = clubs.map { it.clubId }.toSet()
            }
        }

        // Observe live count
        viewModelScope.launch {
            repository.getLiveMatches().collectLatest { result ->
                if (result is ApiResult.Success) {
                    _liveCount.value = result.data.size
                }
            }
        }

        // Pre-fetch matches for all rail dates in the background to populate the match indicator
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val datesToFetch = (0 until 14).map {
            val d = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            d
        }

        viewModelScope.launch {
            // Load initial cached count
            val initialCounts = datesToFetch.associateWith { date ->
                repository.getFixtureCountByDate(date)
            }
            _dateMatchCounts.value = initialCounts

            // Pre-fetch from network if cache is empty, with a delay to avoid rate limits
            datesToFetch.forEach { dateStr ->
                val cachedCount = repository.getFixtureCountByDate(dateStr)
                if (cachedCount == 0) {
                    repository.getFixturesByDate(dateStr).collect { result ->
                        if (result is ApiResult.Success) {
                            val count = result.data.size
                            _dateMatchCounts.update { it + (dateStr to count) }
                        }
                    }
                    delay(500) // Small delay to avoid hammering the API
                }
            }
        }
    }

    fun fetchFormIfNeeded(teamId: Int, leagueId: Int, season: Int) {
        if (teamId == 0) return
        if (_formMap.value.containsKey(teamId)) return
        viewModelScope.launch {
            if (_formMap.value.containsKey(teamId)) return@launch
            
            // Temporary placeholder
            _formMap.update { it + (teamId to "") }
            
            val result = teamRepository.getTeamStatistics(teamId, leagueId, season)
            if (result is ApiResult.Success) {
                val form = result.data.form ?: ""
                if (form.isNotEmpty()) {
                    _formMap.update { it + (teamId to form) }
                } else {
                    _formMap.update { it - teamId }
                }
            } else {
                _formMap.update { it - teamId }
            }
        }
    }

    fun getFixturesByDate(date: String) {
        viewModelScope.launch {
            repository.getFixturesByDate(date).collectLatest {
                _fixturesState.value = it
            }
        }
    }

    fun getFixturesByLeagueSeason(leagueId: Int, season: Int) {
        viewModelScope.launch {
            repository.getFixturesByLeagueSeason(leagueId, season).collectLatest {
                _fixturesState.value = it
            }
        }
    }
}
