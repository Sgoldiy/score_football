package com.example.footballapp.domain.repository

import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.*
import kotlinx.coroutines.flow.Flow

interface FootballRepository {
    fun getFixturesByDate(date: String): Flow<ApiResult<List<Match>>>
    fun getLiveMatches(): Flow<ApiResult<List<Match>>>
    suspend fun getMatchDetail(fixtureId: Int): ApiResult<MatchDetail>
    suspend fun getTeamDetail(teamId: Int, leagueId: Int, season: Int): ApiResult<TeamDetail>
    suspend fun getPlayerDetail(playerId: Int, season: Int): ApiResult<PlayerDetail>
    fun getStandings(leagueId: Int, season: Int): Flow<ApiResult<List<StandingItem>>>
    suspend fun searchTeams(query: String): ApiResult<List<TeamInfo>>
    suspend fun getLeagues(): ApiResult<List<LeagueInfo>>
    fun getFixturesByLeagueSeason(leagueId: Int, season: Int): Flow<ApiResult<List<Match>>>
    // Add other necessary methods
}
