package com.footballpluse.footballapp.data.repository

import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.util.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixturesRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getFixturesByDate(date: String): ApiResult<List<FixtureResponse>> {
        return try {
            val response = apiService.getFixturesByDate(date)
            ApiResult.Success(response.response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getFixturesByLeagueSeason(leagueId: Int, season: Int): ApiResult<List<FixtureResponse>> {
        return try {
            val response = apiService.getFixturesByLeagueSeason(leagueId, season)
            ApiResult.Success(response.response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getFixtureById(fixtureId: Int) = safeApiCall { apiService.getFixtureById(fixtureId) }
    suspend fun getHeadToHead(teamsPair: String, last: Int = 5) = safeApiCall { apiService.getHeadToHead(teamsPair, last) }

    suspend fun getFixtureDetails(fixtureId: Int): ApiResult<FixtureDetailData> {
        // Parallel calls would be done in ViewModel or here
        // Repository can return a combined object
        return try {
            // Note: In a real app, you'd use async/await here if you want to combine them in repository
            // But requirement says ViewModel uses async/await
            ApiResult.Error("Use individual calls in ViewModel as per requirements")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getLineups(fixtureId: Int) = safeApiCall { apiService.getFixtureLineups(fixtureId) }
    suspend fun getEvents(fixtureId: Int) = safeApiCall { apiService.getFixtureEvents(fixtureId) }
    suspend fun getStatistics(fixtureId: Int) = safeApiCall { apiService.getFixtureStatistics(fixtureId) }
    suspend fun getPlayerStats(fixtureId: Int) = safeApiCall { apiService.getFixturePlayerStatistics(fixtureId) }
    suspend fun getPredictions(fixtureId: Int) = safeApiCall { apiService.getPredictions(fixtureId) }
    suspend fun getOdds(fixtureId: Int) = safeApiCall { apiService.getOdds(fixtureId) }
    suspend fun getInjuries(fixtureId: Int) = safeApiCall { apiService.getInjuries(fixtureId) }

    private suspend fun <T> safeApiCall(call: suspend () -> com.footballpluse.footballapp.data.remote.ApiResponse<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.errors?.isNotEmpty == true) {
                ApiResult.Error(response.errors.messages.joinToString())
            } else {
                ApiResult.Success(response.response)
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody?.contains("not subscribed") == true) {
                ApiResult.Error("API Subscription Required: Please subscribe to the API on RapidAPI.com")
            } else {
                ApiResult.Error(e.message ?: "Network Error")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}

data class FixtureDetailData(
    val fixture: FixtureResponse?,
    val lineups: List<FixtureLineup>,
    val events: List<FixtureEvent>,
    val statistics: List<FixtureTeamStatistics>,
    val playerStats: List<FixturePlayerStatisticsResponse>,
    val predictions: List<Prediction>,
    val odds: List<OddsResponse>,
    val injuries: List<Injury>,
    val headToHead: List<FixtureResponse>,
    val errors: Map<String, String>
)

@Singleton
class TeamRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getTeamInfo(teamId: Int) = safeApiCall { apiService.getTeamInfo(teamId) }
    suspend fun getTeamStatistics(teamId: Int, leagueId: Int, season: Int) = safeApiCall { apiService.getTeamStatistics(teamId, leagueId, season) }
    suspend fun getVenueById(venueId: Int) = safeApiCall { apiService.getVenueById(venueId) }
    suspend fun getSquad(teamId: Int) = safeApiCall { apiService.getTeamSquad(teamId) }
    suspend fun getCoaches(teamId: Int) = safeApiCall { apiService.getTeamCoaches(teamId) }
    suspend fun getTransfers(teamId: Int) = safeApiCall { apiService.getTeamTransfers(teamId) }

    private suspend fun <T> safeApiCall(call: suspend () -> com.footballpluse.footballapp.data.remote.ApiResponse<T>): ApiResult<T> {
        return try {
            ApiResult.Success(call().response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}

@Singleton
class PlayerRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getPlayerStats(playerId: Int, season: Int) = safeApiCall { apiService.getPlayerStats(playerId, season) }
    suspend fun getTrophies(playerId: Int) = safeApiCall { apiService.getPlayerTrophies(playerId) }
    suspend fun getSidelined(playerId: Int) = safeApiCall { apiService.getPlayerSidelined(playerId) }

    private suspend fun <T> safeApiCall(call: suspend () -> com.footballpluse.footballapp.data.remote.ApiResponse<T>): ApiResult<T> {
        return try {
            ApiResult.Success(call().response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}

@Singleton
class StandingsRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getStandings(leagueId: Int, season: Int) = safeApiCall { apiService.getStandings(leagueId, season) }

    private suspend fun <T> safeApiCall(call: suspend () -> com.footballpluse.footballapp.data.remote.ApiResponse<T>): ApiResult<T> {
        return try {
            ApiResult.Success(call().response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}

@Singleton
class LeagueRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getLeagues() = safeApiCall { apiService.getLeagues() }
    suspend fun getCountries() = safeApiCall { apiService.getCountries() }
    suspend fun getSeasons() = safeApiCall { apiService.getSeasons() }

    private suspend fun <T> safeApiCall(call: suspend () -> com.footballpluse.footballapp.data.remote.ApiResponse<T>): ApiResult<T> {
        return try {
            ApiResult.Success(call().response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}
