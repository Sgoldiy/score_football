package com.footballpluse.footballapp.data.repository

import com.footballpluse.footballapp.data.mapper.*
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
            val events = apiService.getEvents(from = date, to = date)
            ApiResult.Success(events.toFixtureResponseList())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getFixturesByLeagueSeason(leagueId: Int, season: Int): ApiResult<List<FixtureResponse>> {
        return try {
            // get_events requires a date window (from/to); use the season span
            val from = if (season > 0) "$season-07-01" else "2025-07-01"
            val to = if (season > 0) "${season + 1}-06-30" else "2026-06-30"
            val events = try {
                apiService.getEvents(from = from, to = to, leagueId = leagueId.toString())
            } catch (e: Exception) {
                apiService.getEvents(
                    from = com.footballpluse.footballapp.data.repository.FootballRepositoryImpl.daysFromToday(-200),
                    to = com.footballpluse.footballapp.data.repository.FootballRepositoryImpl.daysFromToday(200),
                    leagueId = leagueId.toString()
                )
            }
            ApiResult.Success(events.toFixtureResponseList())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getFixtureById(fixtureId: Int): ApiResult<FixtureResponse> {
        return try {
            val events = apiService.getEventById(matchId = fixtureId.toString())
            val fixture = events.firstOrNull()?.toFixtureResponse()
                ?: return ApiResult.Error("Fixture not found")
            ApiResult.Success(fixture)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getNextFixtureForTeam(teamId: Int, next: Int = 1): ApiResult<FixtureResponse> {
        return try {
            // get_events requires a date window (from/to). Scan from today forward.
            val from = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, 60)
            val to = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(cal.time)
            val events = apiService.getEvents(from = from, to = to, teamId = teamId.toString())
            val upcoming = events.filter {
                (it.match_hometeam_id == teamId.toString() || it.match_awayteam_id == teamId.toString()) &&
                    it.match_status == "Not Started"
            }.sortedBy { com.footballpluse.footballapp.data.mapper.eventTimestamp(it.match_date, it.match_time) }
            val fixture = upcoming.firstOrNull()
                ?: return ApiResult.Error("No upcoming fixture found")
            ApiResult.Success(fixture.toFixtureResponse())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getHeadToHead(teamsPair: String, last: Int = 5): ApiResult<List<FixtureResponse>> {
        return try {
            val parts = teamsPair.split("-")
            if (parts.size != 2) return ApiResult.Error("Invalid team pair")
            val h2h = apiService.getHeadToHead(firstTeamId = parts[0], secondTeamId = parts[1])
            ApiResult.Success(h2h.allEvents().take(last).toFixtureResponseList())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getLineups(fixtureId: Int): ApiResult<List<FixtureLineup>> {
        return try {
            val lineupMap = apiService.getLineups(matchId = fixtureId.toString())
            val lineupWrapper = lineupMap.values.firstOrNull()?.lineup
            val events = apiService.getEventById(matchId = fixtureId.toString())
            val event = events.firstOrNull()
            val homeId = event?.match_hometeam_id.toIntOr(0)
            val awayId = event?.match_awayteam_id.toIntOr(0)
            ApiResult.Success(listOfNotNull(
                lineupWrapper?.home?.toFixtureLineup(homeId, event?.match_hometeam_name, event?.team_home_badge),
                lineupWrapper?.away?.toFixtureLineup(awayId, event?.match_awayteam_name, event?.team_away_badge)
            ))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getEvents(fixtureId: Int): ApiResult<List<FixtureEvent>> {
        return try {
            val events = apiService.getEventById(matchId = fixtureId.toString())
            val fixture = events.firstOrNull()?.toFixtureResponse()
            ApiResult.Success(fixture?.events ?: emptyList())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getStatistics(fixtureId: Int): ApiResult<List<FixtureTeamStatistics>> {
        return try {
            val statsMap = apiService.getMatchStatistics(matchId = fixtureId.toString())
            val stats = statsMap.values.firstOrNull()?.statistics ?: emptyList()
            val events = apiService.getEventById(matchId = fixtureId.toString())
            val event = events.firstOrNull()
            val homeId = event?.match_hometeam_id.toIntOr(0)
            val awayId = event?.match_awayteam_id.toIntOr(0)
            ApiResult.Success(stats.map { it.toFixtureTeamStatistics(homeId, awayId) })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getPredictions(fixtureId: Int): ApiResult<List<Prediction>> {
        return try {
            val predictions = apiService.getPredictions(matchId = fixtureId.toString())
            ApiResult.Success(predictions.map { it.toPrediction() })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getOdds(fixtureId: Int): ApiResult<List<OddsResponse>> {
        return try {
            val odds = apiService.getOdds(matchId = fixtureId.toString())
            ApiResult.Success(odds.map { it.toOddsResponse() })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getInjuries(fixtureId: Int): ApiResult<List<Injury>> {
        return ApiResult.Error("Injuries not available in current API version")
    }
}

@Singleton
class TeamRepository @Inject constructor(private val apiService: ApiService) {
    private val standingsCache = java.util.concurrent.ConcurrentHashMap<String, List<ApiStanding>>()

    // leagueId_season -> (teamId -> last-5 form string like "WWDLW")
    private val formCache = java.util.concurrent.ConcurrentHashMap<String, Map<Int, String>>()

    /**
     * The v3 get_standings response has NO form field (verified live), so recent form
     * must be derived from finished league fixtures. Computed once per league/season
     * for every team and cached.
     */
    private suspend fun computeFormsFromFixtures(leagueId: Int, season: Int): Map<Int, String> {
        formCache["${leagueId}_$season"]?.let { return it }
        val forms = try {
            val events = try {
                apiService.getEvents(from = "$season-07-01", to = "${season + 1}-06-30", leagueId = leagueId.toString())
            } catch (e: Exception) {
                apiService.getEvents(
                    from = com.footballpluse.footballapp.data.repository.FootballRepositoryImpl.daysFromToday(-200),
                    to = com.footballpluse.footballapp.data.repository.FootballRepositoryImpl.daysFromToday(200),
                    leagueId = leagueId.toString()
                )
            }
            val perTeam = mutableMapOf<Int, MutableList<Triple<Long, Int, Int>>>() // teamId -> (timestamp, goalsFor, goalsAgainst)
            // Upstream finished status is "FT" (mapped via mapStatus; the raw
            // feed also carries literal "Finished" in some payloads).
            events.filter { it.match_status == "FT" || it.match_status == "Finished" }.forEach { e ->
                val hId = e.match_hometeam_id?.toIntOrNull() ?: return@forEach
                val aId = e.match_awayteam_id?.toIntOrNull() ?: return@forEach
                val hG = e.match_hometeam_score?.trim()?.toIntOrNull() ?: return@forEach
                val aG = e.match_awayteam_score?.trim()?.toIntOrNull() ?: return@forEach
                val ts = com.footballpluse.footballapp.data.mapper.eventTimestamp(e.match_date, e.match_time)
                perTeam.getOrPut(hId) { mutableListOf() }.add(Triple(ts, hG, aG))
                perTeam.getOrPut(aId) { mutableListOf() }.add(Triple(ts, aG, hG))
            }
            perTeam.mapValues { (_, matches) ->
                matches.sortedByDescending { it.first }
                    .take(5)
                    .map { (ts, gf, ga) -> when { gf > ga -> 'W'; gf == ga -> 'D'; else -> 'L' } }
                    .joinToString("")
            }
        } catch (e: Exception) {
            emptyMap()
        }
        if (forms.isNotEmpty()) formCache["${leagueId}_$season"] = forms
        return forms
    }

    suspend fun getTeamInfo(teamId: Int): ApiResult<TeamInfoResponse> {
        return try {
            val teams = apiService.getTeams(teamId = teamId.toString())
            val info = teams.firstOrNull()?.toTeamInfoResponse()
                ?: return ApiResult.Error("Team not found")
            ApiResult.Success(info)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Last-5 form for EVERY team in the league with a single fixtures call
     * (shared with computeFormsFromFixtures' cache). Home screen calls this once per
     * league instead of once per match row.
     */
    suspend fun getTeamFormForLeague(leagueId: Int, season: Int): ApiResult<Map<Int, String>> {
        return try {
            ApiResult.Success(computeFormsFromFixtures(leagueId, season))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load form")
        }
    }

    suspend fun getTeamStatistics(teamId: Int, leagueId: Int, season: Int): ApiResult<TeamStatistics> {
        return try {
            val standings = standingsCache.getOrPut(leagueId.toString()) {
                apiService.getStandings(leagueId = leagueId.toString())
            }
            val teamStanding = standings.find { it.team_id == teamId.toString() }
            if (teamStanding != null) {
                val wins = teamStanding.standing_W?.toIntOrNull() ?: 0
                val draws = teamStanding.standing_D?.toIntOrNull() ?: 0
                val loses = teamStanding.standing_L?.toIntOrNull() ?: 0
                val played = teamStanding.standing_total?.toIntOrNull() ?: (wins + draws + loses)

                val stats = TeamStatistics(
                    league = null, team = null,
                    // v3 standings have no form field — derive from recent league fixtures
                    form = computeFormsFromFixtures(leagueId, season)[teamId] ?: teamStanding.overall_form,
                    fixtures = TeamFixturesStats(
                        played = FixtureCount(null, null, played),
                        wins = FixtureCount(null, null, wins),
                        draws = FixtureCount(null, null, draws),
                        loses = FixtureCount(null, null, loses)
                    ),
                    goals = TeamGoalsStats(
                        goalsFor = GoalStatsDetail(FixtureCount(null, null, teamStanding.overall_GF?.toIntOrNull()), null, null, null),
                        against = GoalStatsDetail(FixtureCount(null, null, teamStanding.overall_GA?.toIntOrNull()), null, null, null)
                    ),
                    biggest = null, clean_sheet = null, failed_to_score = null,
                    penalty = null, lineups = null, cards = null
                )
                ApiResult.Success(stats)
            } else {
                ApiResult.Error("Team statistics not found in standings")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getSquad(teamId: Int): ApiResult<List<SquadPlayer>> {
        return try {
            val teams = apiService.getTeams(teamId = teamId.toString())
            val players = teams.firstOrNull()?.players?.toSquadPlayers() ?: emptyList()
            ApiResult.Success(players)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getCoaches(teamId: Int): ApiResult<List<Coach>> {
        return try {
            val teams = apiService.getTeams(teamId = teamId.toString())
            val coaches = teams.firstOrNull()?.coaches?.toCoaches() ?: emptyList()
            ApiResult.Success(coaches)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getTransfers(teamId: Int): ApiResult<List<Transfer>> {
        return ApiResult.Error("Transfers not available in current API version")
    }
}

@Singleton
class PlayerRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getPlayerStats(playerId: Int, season: Int): ApiResult<List<PlayerProfileStatisticsResponse>> {
        return try {
            val players = apiService.getPlayers(playerId = playerId.toString())
            ApiResult.Success(players.map { it.toPlayerProfileStatisticsResponse() })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getTrophies(playerId: Int): ApiResult<List<PlayerTrophy>> {
        return ApiResult.Success(emptyList())
    }

    suspend fun getSidelined(playerId: Int): ApiResult<List<PlayerSidelined>> {
        return ApiResult.Success(emptyList())
    }
}

@Singleton
class StandingsRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getStandings(leagueId: Int, season: Int): ApiResult<Standing> {
        return try {
            val standings = apiService.getStandings(leagueId = leagueId.toString())
            ApiResult.Success(standings.toStanding())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}

@Singleton
class LeagueRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getLeagues(): ApiResult<List<LeagueResponse>> {
        return try {
            val leagues = apiService.getLeagues()
            ApiResult.Success(leagues.map { it.toLeagueResponse() })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getCountries(): ApiResult<List<ApiCountry>> {
        return try {
            ApiResult.Success(apiService.getCountries())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getSeasons(): ApiResult<List<Int>> {
        return ApiResult.Error("Seasons endpoint not available in current API version")
    }
}

@Singleton
class BillingRepository @Inject constructor() {
    private val _isPurchased = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPurchased: kotlinx.coroutines.flow.Flow<Boolean> = _isPurchased

    fun setPurchased(purchased: Boolean) {
        _isPurchased.value = purchased
    }
}
