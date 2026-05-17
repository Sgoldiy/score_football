package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.repository.FixturesRepository
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.data.repository.FixtureDetailData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FixtureDetailViewModel @Inject constructor(
    private val repository: FixturesRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<ApiResult<FixtureDetailData>>(ApiResult.Loading)
    val detailState: StateFlow<ApiResult<FixtureDetailData>> = _detailState

    fun loadFixtureDetails(fixtureId: Int) {
        viewModelScope.launch {
            _detailState.value = ApiResult.Loading
            try {
                val fixtureDeferred = async { repository.getFixtureById(fixtureId) }
                val lineupsDeferred = async { repository.getLineups(fixtureId) }
                val eventsDeferred = async { repository.getEvents(fixtureId) }
                val statsDeferred = async { repository.getStatistics(fixtureId) }
                val playerStatsDeferred = async { repository.getPlayerStats(fixtureId) }
                val predictionsDeferred = async { repository.getPredictions(fixtureId) }
                val oddsDeferred = async { repository.getOdds(fixtureId) }
                val injuriesDeferred = async { repository.getInjuries(fixtureId) }

                val fixtureResult = fixtureDeferred.await()
                val fixture = (fixtureResult as? ApiResult.Success<List<FixtureResponse>>)?.data?.firstOrNull()

                val lineups = lineupsDeferred.await()
                val events = eventsDeferred.await()
                val stats = statsDeferred.await()
                val playerStats = playerStatsDeferred.await()
                val predictions = predictionsDeferred.await()
                val odds = oddsDeferred.await()
                val injuries = injuriesDeferred.await()

                val headToHeadResult = if (fixture != null) {
                    val homeTeamId = fixture.teams?.home?.id
                    val awayTeamId = fixture.teams?.away?.id
                    if (homeTeamId != null && awayTeamId != null) {
                        repository.getHeadToHead("$homeTeamId-$awayTeamId", last = 5)
                    } else {
                        ApiResult.Success(emptyList())
                    }
                } else {
                    ApiResult.Success(emptyList())
                }

                val errors = buildMap {
                    putIfError("fixture", fixtureResult)
                    putIfError("lineups", lineups)
                    putIfError("events", events)
                    putIfError("statistics", stats)
                    putIfError("playerStats", playerStats)
                    putIfError("predictions", predictions)
                    putIfError("odds", odds)
                    putIfError("injuries", injuries)
                    putIfError("headToHead", headToHeadResult)
                }

                val detailData = FixtureDetailData(
                    fixture = fixture,
                    lineups = (lineups as? ApiResult.Success<List<Any>>)?.data ?: emptyList(),
                    events = (events as? ApiResult.Success<List<Any>>)?.data ?: emptyList(),
                    statistics = (stats as? ApiResult.Success<List<Any>>)?.data ?: emptyList(),
                    playerStats = (playerStats as? ApiResult.Success<List<Any>>)?.data ?: emptyList(),
                    predictions = (predictions as? ApiResult.Success<List<com.example.footballapp.data.model.Prediction>>)?.data ?: emptyList(),
                    odds = (odds as? ApiResult.Success<List<com.example.footballapp.data.model.OddsResponse>>)?.data ?: emptyList(),
                    injuries = (injuries as? ApiResult.Success<List<com.example.footballapp.data.model.Injury>>)?.data ?: emptyList(),
                    headToHead = (headToHeadResult as? ApiResult.Success<List<FixtureResponse>>)?.data ?: emptyList(),
                    errors = errors
                )

                if (detailData.fixture == null && detailData.events.isEmpty() && detailData.statistics.isEmpty()) {
                    _detailState.value = ApiResult.Error(
                        errors["fixture"] ?: "Unable to load fixture details right now."
                    )
                } else {
                    _detailState.value = ApiResult.Success(detailData)
                }
            } catch (e: Exception) {
                _detailState.value = ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun MutableMap<String, String>.putIfError(key: String, result: ApiResult<*>) {
        if (result is ApiResult.Error) {
            this[key] = result.message
        }
    }
}
