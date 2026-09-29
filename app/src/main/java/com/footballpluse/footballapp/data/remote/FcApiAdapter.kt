package com.footballpluse.footballapp.data.remote

import com.footballpluse.footballapp.data.local.db.FixtureDao
import com.footballpluse.footballapp.data.mapper.eventTimestamp
import com.footballpluse.footballapp.data.model.ApiCountry
import com.footballpluse.footballapp.data.model.ApiEvent
import com.footballpluse.footballapp.data.model.ApiH2HResponse
import com.footballpluse.footballapp.data.model.ApiLeague
import com.footballpluse.footballapp.data.model.ApiLineupResponse
import com.footballpluse.footballapp.data.model.ApiMatchStatisticsResponse
import com.footballpluse.footballapp.data.model.ApiOdd
import com.footballpluse.footballapp.data.model.ApiPlayer
import com.footballpluse.footballapp.data.model.ApiPrediction
import com.footballpluse.footballapp.data.model.ApiStanding
import com.footballpluse.footballapp.data.model.ApiTeam
import com.footballpluse.footballapp.data.model.ApiTopScorer
import com.footballpluse.footballapp.data.remote.FcLeagueCatalog.seasonLabel
import com.footballpluse.footballapp.data.util.SeasonUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * The app's single data seam: implements the legacy [ApiService] interface entirely
 * on top of the FootballCharts API ([FcApiService]).
 *
 * Every value emitted here comes from live FC responses — nothing is simulated.
 * Features the FC source does not cover (players, top scorers, lineups, in-match
 * statistics, bookmaker odds) return honest empties and the UI shows its existing
 * empty states.
 *
 * FC identity model, bridged to the app's Int ids:
 *  - Leagues: FcLeagueCatalog appId <-> FC slug.
 *  - Teams: names only -> deterministic Int via FcTeamIds, logos via FcLogoIndex.
 *  - Matches: numeric FC id -> slug via FcMatchRegistry (built or captured).
 */
@Singleton
class FcApiAdapter @Inject constructor(
    private val fcApi: FcApiService,
    private val fixtureDao: FixtureDao
) : ApiService {

    companion object {
        /** Old-API league ids that may still be persisted in DataStore/Room. */
        private val LEGACY_LEAGUE_SLUGS = mapOf(
            152 to "premier", 302 to "spain1", 207 to "italy1",
            175 to "germany1", 168 to "france1", 88 to "holland1",
            94 to "portugal1", 144 to "belgium1", 203 to "saudi1", 187 to "swiss1"
        )

        private const val RESULTS_TTL_MS = 5 * 60_000L
        private const val FIXTURES_TTL_MS = 10 * 60_000L
        private const val TEAMS_TTL_MS = 30 * 60_000L
        private const val LIVE_WINDOW_MS = 3 * 60 * 60 * 1000L
    }

    private val gate = Semaphore(4)
    private val resultsCache = FcLeagueDataCache<FcMatchesResponse>()
    private val fixturesCache = FcLeagueDataCache<FcMatchesResponse>()
    private val teamsCache = FcLeagueDataCache<FcTeamsResponse>()

    private suspend fun <T> gated(block: suspend () -> T): T = gate.withPermit { block() }

    // ─── League/season helpers ────────────────────────────────────────────────

    private fun leagueById(idStr: String?): FcLeague? {
        val id = idStr?.toIntOrNull() ?: return idStr?.let { FcLeagueCatalog.bySlug(it) }
        return FcLeagueCatalog.byId(id)
            ?: LEGACY_LEAGUE_SLUGS[id]?.let { FcLeagueCatalog.bySlug(it) }
    }

    private fun currentSeasonFor(lg: FcLeague): String =
        seasonLabel(lg, SeasonUtils.currentSeasonStartYear())

    private fun seasonForWindow(lg: FcLeague, from: String?): String {
        val y = from?.take(4)?.toIntOrNull() ?: SeasonUtils.currentSeasonStartYear()
        return seasonLabel(lg, y)
    }

    private fun inWindow(date: String?, from: String?, to: String?): Boolean {
        if (date.isNullOrBlank()) return false
        if (from.isNullOrBlank() || to.isNullOrBlank()) return true
        return date >= from && date <= to
    }

    // ─── Countries & leagues ──────────────────────────────────────────────────

    override suspend fun getCountries(): List<ApiCountry> =
        FcLeagueCatalog.ALL.map { it.country }.distinct().sorted().map { country ->
            ApiCountry(
                country_id = country.lowercase().replace(" ", "-"),
                country_name = country,
                country_logo = null
            )
        }

    override suspend fun getLeagues(countryId: String?): List<ApiLeague> {
        val countryFilter = countryId
            ?.let { c -> FcLeagueCatalog.ALL.firstOrNull { it.country.equals(c, true) }?.country }
        return FcLeagueCatalog.ALL
            .filter { countryFilter == null || it.country == countryFilter }
            .map { lg ->
                ApiLeague(
                    country_id = lg.country.lowercase().replace(" ", "-"),
                    country_name = lg.country,
                    league_id = lg.appId.toString(),
                    league_name = lg.name,
                    league_season = currentSeasonFor(lg),
                    league_logo = null,
                    country_logo = null
                )
            }
    }

    // ─── Teams ────────────────────────────────────────────────────────────────

    override suspend fun getTeams(leagueId: String?, teamId: String?): List<ApiTeam> {
        return try {
            when {
                !leagueId.isNullOrBlank() -> teamsForLeague(leagueId)
                !teamId.isNullOrBlank() -> teamById(teamId)
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun teamsForLeague(leagueId: String): List<ApiTeam> {
        val lg = leagueById(leagueId) ?: return emptyList()
        val season = currentSeasonFor(lg)
        val resp = try {
            teamsCache.getOrFetch("t|${lg.slug}|$season", TEAMS_TTL_MS) {
                gated { fcApi.getTeams(lg.slug, season) }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        resp.teams.forEach { t ->
            FcLogoIndex.put(lg.slug, t.team, t.logoUrl)
            FcTeamLeagueIndex.register(t.team, lg.slug)
            FcTeamIds.id(t.team)
        }
        return resp.teams.map { t ->
            ApiTeam(
                team_key = FcTeamIds.id(t.team).toString(),
                team_name = t.team,
                team_country = lg.country,
                team_founded = null,
                team_badge = t.logoUrl ?: FcLogoIndex.forName(t.team),
                venue = null,
                players = null,
                coaches = null
            )
        }
    }

    private suspend fun teamById(teamId: String): List<ApiTeam> {
        val id = teamId.toIntOrNull() ?: return emptyList()
        val name = FcTeamIds.name(id) ?: return emptyList()
        var leagues = FcTeamLeagueIndex.leaguesOf(name)
        if (leagues.isEmpty()) leagues = discoverTeamLeagues(name)
        val out = mutableListOf<ApiTeam>()
        for (slug in leagues.take(2)) {
            val lg = FcLeagueCatalog.bySlug(slug) ?: continue
            val page = try {
                gated {
                    fcApi.getTeamPage(lg.slug, ApiConfig.teamSlug(name) ?: name.lowercase(), currentSeasonFor(lg))
                }
            } catch (_: Exception) {
                continue
            }
            FcLogoIndex.put(lg.slug, page.team, page.logoUrl)
            FcTeamLeagueIndex.register(page.team, lg.slug)
            out += ApiTeam(
                team_key = id.toString(),
                team_name = page.team,
                team_country = FcLeagueCatalog.bySlug(page.league ?: lg.slug)?.country ?: lg.country,
                team_founded = null,
                team_badge = page.logoUrl ?: FcLogoIndex.forName(page.team),
                venue = null,
                players = null,
                coaches = null
            )
        }
        return out.distinctBy { it.team_name }
    }

    /** Scan popular leagues' team lists to find which league(s) a team belongs to. */
    private suspend fun discoverTeamLeagues(teamName: String): List<String> = coroutineScope {
        FcPopularLeagues.SLUGS.map { slug ->
            async {
                try {
                    val lg = FcLeagueCatalog.bySlug(slug) ?: return@async null
                    val resp = teamsCache.getOrFetch("t|${lg.slug}|${currentSeasonFor(lg)}", TEAMS_TTL_MS) {
                        gated { fcApi.getTeams(lg.slug, currentSeasonFor(lg)) }
                    }
                    resp.teams.forEach { t ->
                        FcLogoIndex.put(lg.slug, t.team, t.logoUrl)
                        FcTeamLeagueIndex.register(t.team, lg.slug)
                        FcTeamIds.id(t.team)
                    }
                    if (resp.teams.any { it.team.equals(teamName, true) }) lg.slug else null
                } catch (_: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    // ─── Players / scorers / odds / lineups / stats: NOT covered by FC ───────

    override suspend fun getPlayers(playerId: String?, playerName: String?): List<ApiPlayer> = emptyList()

    override suspend fun getTopScorers(leagueId: String): List<ApiTopScorer> = emptyList()

    override suspend fun getOdds(from: String?, to: String?, matchId: String?): List<ApiOdd> = emptyList()

    override suspend fun getLineups(matchId: String): Map<String, ApiLineupResponse> = emptyMap()

    override suspend fun getMatchStatistics(matchId: String): Map<String, ApiMatchStatisticsResponse> = emptyMap()

    // ─── Standings ────────────────────────────────────────────────────────────

    override suspend fun getStandings(leagueId: String): List<ApiStanding> {
        val lg = leagueById(leagueId) ?: return emptyList()
        val season = currentSeasonFor(lg)
        // Seed team logos + league membership first (cheap, also cached).
        try {
            teamsForLeague(lg.appId.toString())
        } catch (_: Exception) {
        }
        val table = try {
            gated { fcApi.getLeagueTable(lg.slug, season, view = null) }
        } catch (_: Exception) {
            return emptyList()
        }
        return table.table.map { row ->
            FcTeamIds.id(row.team)
            FcTeamLeagueIndex.register(row.team, lg.slug)
            ApiStanding(
                country_name = lg.country,
                league_id = lg.appId.toString(),
                league_name = lg.name,
                team_id = FcTeamIds.id(row.team).toString(),
                team_name = row.team,
                standing_place = row.position.toString(),
                standing_place_type = null,
                standing_group = null,
                standing_home = null,
                standing_away = null,
                standing_total = row.played.toString(),
                standing_D = row.drawn?.toString(),
                standing_W = row.won.toString(),
                standing_L = row.lost.toString(),
                standing_PE = null,
                standing_PTS = row.points.toString(),
                overall_form = row.last5Form,
                overall_GF = row.goalsFor?.toString(),
                overall_GA = row.goalsAgainst?.toString(),
                team_badge = FcLogoIndex.forName(row.team),
                league_logo = null,
                fk_stage_key = null,
                stage_name = null,
                overallPromotion = null
            )
        }
    }

    // ─── Events (fixtures/results) ────────────────────────────────────────────

    override suspend fun getEvents(
        from: String,
        to: String,
        leagueId: String?,
        teamId: String?,
        matchId: String?
    ): List<ApiEvent> {
        return try {
            when {
                !matchId.isNullOrBlank() -> getEventById(matchId)
                !leagueId.isNullOrBlank() -> leagueEvents(leagueId, from, to, teamId)
                !teamId.isNullOrBlank() -> teamEventsAcrossLeagues(teamId, from, to)
                else -> popularEvents(from, to)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun leagueEvents(
        leagueId: String,
        from: String,
        to: String,
        teamId: String?
    ): List<ApiEvent> {
        val lg = leagueById(leagueId) ?: return emptyList()
        // Calendar-year leagues (season "2026") ignore the July–June window shape.
        var effFrom = from
        var effTo = to
        if (from.isNotBlank() && to.isNotBlank() &&
            !lg.currentSeason.contains("-") && from.endsWith("-07-01")
        ) {
            effFrom = "${from.take(4)}-01-01"
            effTo = "${from.take(4)}-12-31"
        }
        val season = seasonForWindow(lg, effFrom)
        val (results, fixtures) = loadLeagueMatches(lg, season)
        val teamName = teamId?.toIntOrNull()?.let { FcTeamIds.name(it) }
        return (results + fixtures)
            .map { matchDtoToApiEvent(it, lg, season) }
            .filter { inWindow(it.match_date, effFrom, effTo) }
            .filter { teamName == null || it.match_hometeam_name.equals(teamName, true) || it.match_awayteam_name.equals(teamName, true) }
            .sortedWith(compareByDescending<ApiEvent> { it.match_date ?: "" }.thenByDescending { it.match_time ?: "" })
    }

    private suspend fun teamEventsAcrossLeagues(teamId: String, from: String, to: String): List<ApiEvent> {
        val id = teamId.toIntOrNull() ?: return emptyList()
        val name = FcTeamIds.name(id) ?: return emptyList()
        var leagues = FcTeamLeagueIndex.leaguesOf(name)
        if (leagues.isEmpty()) leagues = discoverTeamLeagues(name)
        val events = mutableListOf<ApiEvent>()
        for (slug in leagues.take(2)) {
            val lg = FcLeagueCatalog.bySlug(slug) ?: continue
            val season = currentSeasonFor(lg)
            val (results, fixtures) = loadLeagueMatches(lg, season)
            events += (results + fixtures)
                .map { matchDtoToApiEvent(it, lg, season) }
                .filter { it.match_hometeam_name.equals(name, true) || it.match_awayteam_name.equals(name, true) }
                .filter { inWindow(it.match_date, from, to) }
        }
        return events.distinctBy { it.match_id }
            .sortedByDescending { it.match_date ?: "" }
    }

    private suspend fun popularEvents(from: String, to: String): List<ApiEvent> = coroutineScope {
        FcPopularLeagues.SLUGS.map { slug ->
            async {
                try {
                    val lg = FcLeagueCatalog.bySlug(slug) ?: return@async emptyList<ApiEvent>()
                    val season = currentSeasonFor(lg)
                    val (results, fixtures) = loadLeagueMatches(lg, season)
                    (results + fixtures)
                        .map { matchDtoToApiEvent(it, lg, season) }
                        .filter { inWindow(it.match_date, from, to) }
                } catch (_: Exception) {
                    emptyList<ApiEvent>()
                }
            }
        }.awaitAll().flatten()
            .distinctBy { it.match_id }
            .sortedWith(compareBy<ApiEvent> { it.match_date ?: "" }.thenBy { it.match_time ?: "" })
    }

    private suspend fun loadLeagueMatches(
        lg: FcLeague,
        season: String
    ): Pair<List<FcMatchDto>, List<FcMatchDto>> = coroutineScope {
        val results = async {
            try {
                resultsCache.getOrFetch("r|${lg.slug}|$season", RESULTS_TTL_MS) {
                    gated { fcApi.getResults(lg.slug, season) }
                }
            } catch (_: Exception) {
                FcMatchesResponse()
            }
        }
        val fixtures = async {
            try {
                fixturesCache.getOrFetch("f|${lg.slug}|$season", FIXTURES_TTL_MS) {
                    gated { fcApi.getFixtures(lg.slug, season) }
                }
            } catch (_: Exception) {
                FcMatchesResponse()
            }
        }
        results.await().matches to fixtures.await().matches
    }

    // ─── Single match ─────────────────────────────────────────────────────────

    override suspend fun getEventById(matchId: String): List<ApiEvent> {
        val id = matchId.toIntOrNull() ?: return emptyList()
        val slug = slugForMatch(id) ?: return emptyList()
        val detail = try {
            gated { fcApi.getMatch(slug) }.match
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        return listOf(detailToApiEvent(detail))
    }

    private suspend fun slugForMatch(id: Int): String? {
        val snapshot = FcMatchRegistry.get(id)
        if (snapshot != null) {
            val realName = snapshot.leagueSlug?.let { FcLeagueCatalog.bySlug(it)?.name }
            FcMatchRegistry.slugFor(snapshot, realName)?.let { return it }
        }
        // Fallback: rebuild from the Room cache (works after a process restart).
        val entity = try {
            fixtureDao.getFixtureById(id)
        } catch (_: Exception) {
            null
        }
        if (entity != null) {
            val lg = leagueById(entity.leagueId.toString())
            if (lg != null) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(entity.timestamp * 1000))
                val built = FcMatchRegistry.Snapshot(id, lg.slug, entity.homeTeamName, entity.awayTeamName, date, null)
                    .let { FcMatchRegistry.slugFor(it, lg.name) }
                if (built != null) return built
            }
        }
        // Slug-only fixture rows (no numeric id): the id was derived from the slug.
        return FcMatchRegistry.slugForId(id)
    }

    private fun detailToApiEvent(d: FcMatchDetailDto): ApiEvent {
        val hasScore = d.homeScore != null || d.awayScore != null
        val rawStatus = d.matchStatus?.lowercase()?.trim()
        val status = when (rawStatus) {
            "ft", "finished", "full_time" -> "Finished"
            "ht", "halftime" -> "Halftime"
            "aet" -> "After ET"
            "pen", "ap" -> "After Penalties"
            "postponed", "ppt" -> "Postponed"
            "cancelled", "canceled", "canc" -> "Cancelled"
            "suspended", "susp" -> "Suspended"
            "abandoned", "abd" -> "Abandoned"
            "ns", "scheduled", "not_started", "timed", null, "" -> if (hasScore) "Finished" else "Not Started"
            else -> if (rawStatus?.firstOrNull()?.isDigit() == true) "In Play"
            else if (hasScore) "Finished" else "Not Started"
        }
        FcMatchRegistry.put(d.id, d.league, d.homeTeam, d.awayTeam, d.matchDate, d.slug)
        d.league?.let { lgSlug ->
            FcLogoIndex.put(lgSlug, d.homeTeam, d.homeTeamLogo)
            FcLogoIndex.put(lgSlug, d.awayTeam, d.awayTeamLogo)
            FcTeamLeagueIndex.register(d.homeTeam, lgSlug)
            FcTeamLeagueIndex.register(d.awayTeam, lgSlug)
            FcTeamIds.id(d.homeTeam ?: "")
            FcTeamIds.id(d.awayTeam ?: "")
        }
        val lg = d.league?.let { FcLeagueCatalog.bySlug(it) }
        val liveMinute = d.matchMinute
        val matchStatusOut = when {
            status == "In Play" && liveMinute != null -> "$liveMinute'"
            else -> status
        }
        return ApiEvent(
            match_id = d.id?.toString() ?: d.slug,
            country_name = d.country ?: lg?.country,
            league_id = lg?.appId?.toString() ?: d.league,
            league_name = d.realLeagueName ?: lg?.name ?: d.league,
            match_date = d.matchDate,
            match_time = berlinTime(d.matchDatetime, d.time),
            match_status = matchStatusOut,
            match_hometeam_id = d.homeTeam?.let { FcTeamIds.id(it).toString() },
            match_hometeam_name = d.homeTeam,
            match_hometeam_score = d.homeScore?.toString(),
            match_awayteam_id = d.awayTeam?.let { FcTeamIds.id(it).toString() },
            match_awayteam_name = d.awayTeam,
            match_awayteam_score = d.awayScore?.toString(),
            match_hometeam_halftime_score = parseScore(d.htResult)?.first?.toString(),
            match_awayteam_halftime_score = parseScore(d.htResult)?.second?.toString(),
            match_hometeam_ft_score = d.homeScore?.toString(),
            match_awayteam_ft_score = d.awayScore?.toString(),
            league_year = d.season,
            team_home_badge = d.homeTeamLogo,
            team_away_badge = d.awayTeamLogo,
            goalscorer = null,
            cards = null,
            substitutions = null,
            statistics = null,
            lineup = null
        )
    }

    // ─── Livescore (FC has no live feed; inferred from real kickoff times) ────

    override suspend fun getLivescore(matchId: String?, leagueId: String?): List<ApiEvent> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val now = System.currentTimeMillis()
        val slugs = if (!leagueId.isNullOrBlank()) {
            listOfNotNull(leagueById(leagueId)?.slug)
        } else {
            FcPopularLeagues.SLUGS
        }
        val out = mutableListOf<ApiEvent>()
        for (slug in slugs) {
            val lg = FcLeagueCatalog.bySlug(slug) ?: continue
            val season = currentSeasonFor(lg)
            val (results, fixtures) = try {
                loadLeagueMatches(lg, season)
            } catch (_: Exception) {
                continue
            }
            val todayEvents = (results + fixtures).map { matchDtoToApiEvent(it, lg, season) }
                .filter { it.match_date == today }
            for (ev in todayEvents) {
                val hs = ev.match_hometeam_score?.toIntOrNull()
                val as_ = ev.match_awayteam_score?.toIntOrNull()
                if (hs != null || as_ != null) continue // already finished
                val kickoffMs = eventTimestamp(ev.match_date, ev.match_time) * 1000L
                if (kickoffMs in (now - LIVE_WINDOW_MS)..now) {
                    out += ev.copy(match_live = "1", match_status = "In Play")
                }
            }
        }
        return out.distinctBy { it.match_id }
    }

    // ─── Head-to-head (derived from real league results) ──────────────────────

    override suspend fun getHeadToHead(firstTeamId: String, secondTeamId: String): ApiH2HResponse {
        val a = FcTeamIds.name(firstTeamId.toIntOrNull() ?: 0)
        val b = FcTeamIds.name(secondTeamId.toIntOrNull() ?: 0)
        if (a.isNullOrBlank() || b.isNullOrBlank()) {
            return ApiH2HResponse(null, null, null)
        }
        var shared = FcTeamLeagueIndex.leaguesOf(a)
            .intersect(FcTeamLeagueIndex.leaguesOf(b).toSet())
            .toList()
        if (shared.isEmpty()) {
            val aLeagues = discoverTeamLeagues(a).toSet()
            shared = aLeagues.intersect(discoverTeamLeagues(b).toSet()).toList()
        }
        val first = mutableListOf<ApiEvent>()
        val second = mutableListOf<ApiEvent>()
        val mutual = mutableListOf<ApiEvent>()
        for (slug in shared.take(2)) {
            val lg = FcLeagueCatalog.bySlug(slug) ?: continue
            val season = currentSeasonFor(lg)
            val (results, fixtures) = loadLeagueMatches(lg, season)
            val events = (results + fixtures).map { matchDtoToApiEvent(it, lg, season) }
            first += events.filter { it.match_hometeam_name.equals(a, true) || it.match_awayteam_name.equals(a, true) }
            second += events.filter { it.match_hometeam_name.equals(b, true) || it.match_awayteam_name.equals(b, true) }
            mutual += events.filter {
                (it.match_hometeam_name.equals(a, true) && it.match_awayteam_name.equals(b, true)) ||
                    (it.match_hometeam_name.equals(b, true) && it.match_awayteam_name.equals(a, true))
            }
        }
        return ApiH2HResponse(
            firstTeam_lastResults = first.distinctBy { it.match_id }.take(10),
            secondTeam_lastResults = second.distinctBy { it.match_id }.take(10),
            firstTeam_VS_secondTeam = mutual.distinctBy { it.match_id }.take(10)
        )
    }

    // ─── Model predictions (real FC probabilities) ────────────────────────────

    /**
     * Real dc_v2 model output for the match (calibrated when present, raw as
     * fallback). Surfaces the FULL probability set: 1X2, BTTS, the over/under
     * 0.5–4.5 ladder, HT lines, expected goals, plus the model's own freshness
     * status (stale flag / matches behind / effective sample sizes).
     */
    override suspend fun getPredictions(matchId: String): List<ApiPrediction> {
        val id = matchId.toIntOrNull() ?: return emptyList()
        val slug = slugForMatch(id) ?: return emptyList()
        val detail = try {
            gated { fcApi.getMatch(slug) }.match
        } catch (_: Exception) {
            return emptyList()
        } ?: return emptyList()
        val model = detail.modelPredictions?.dcV2 ?: return emptyList()
        val probs = model.calibrated ?: model.raw ?: return emptyList()
        val home = probs.home
        val away = probs.away
        val draw = if (home != null && away != null) (1.0 - home - away).coerceIn(0.0, 1.0) else null
        val advice = FcAdvice.forMatch(
            homePct = home?.times(100),
            drawPct = draw?.times(100),
            awayPct = away?.times(100),
            bttsPct = probs.bttsYes?.times(100),
            over25Pct = probs.over25?.times(100)
        )
        val stale = model.dataStatus?.stale == true
        val staleNote = if (stale) "model data ${model.dataStatus?.matchesBehind ?: 0} matches behind" else null
        val effective = model.effectiveMatches
        val sampleNote = if (effective?.home != null && effective.away != null) {
            "effective sample: ${effective.home}/${effective.away}"
        } else null
        val overLadder = listOfNotNull(
            probs.over05?.let { "O0.5 ${pct(it)}%" },
            probs.over15?.let { "O1.5 ${pct(it)}%" },
            probs.over25?.let { "O2.5 ${pct(it)}%" },
            probs.over35?.let { "O3.5 ${pct(it)}%" },
            probs.over45?.let { "O4.5 ${pct(it)}%" }
        ).joinToString(" ")
        val htLine = listOfNotNull(
            probs.htOver05?.let { "HT O0.5 ${pct(it)}%" },
            probs.htOver15?.let { "HT O1.5 ${pct(it)}%" }
        ).joinToString(" ")
        val xgLine = listOfNotNull(
            probs.expectedHomeGoals?.let { "xG ${fmt1(it)}-${fmt1(probs.expectedAwayGoals ?: 0.0)}" }
        ).joinToString(" ")
        return listOf(
            ApiPrediction(
                homeWin = pct(home),
                draw = pct(draw),
                awayWin = pct(away),
                advice = advice,
                btts_yes = pct(probs.bttsYes),
                over_25 = pct(probs.over25),
                under_25 = probs.over25?.let { pct(1.0 - it) },
                over_05 = pct(probs.over05),
                over_15 = pct(probs.over15),
                over_35 = pct(probs.over35),
                over_45 = pct(probs.over45),
                ht_over_05 = pct(probs.htOver05),
                ht_over_15 = pct(probs.htOver15),
                expected_home_goals = probs.expectedHomeGoals,   // raw double on purpose
                expected_away_goals = probs.expectedAwayGoals,
                overLadder = overLadder.ifBlank { null },
                htLines = htLine.ifBlank { null },
                xgSummary = xgLine.ifBlank { null },
                modelNote = listOfNotNull(staleNote, sampleNote).joinToString(" · ").ifBlank { null },
                goalsSummary = FcAdvice.goalsSummary(
                    over25Pct = probs.over25?.times(100),
                    bttsPct = probs.bttsYes?.times(100)
                )
            )
        )
    }

    private fun fmt1(v: Double): String = ((v * 10).roundToInt() / 10.0).toString()

    // ─── Mapping helpers ──────────────────────────────────────────────────────

    private fun matchDtoToApiEvent(dto: FcMatchDto, lg: FcLeague, season: String): ApiEvent {
        // Handles both live payload shapes: results rows (homeTeam/score) and
        // fixtures rows (home_team/match_date/slug/status — verified 2026-09-28).
        val home = dto.homeTeam ?: dto.home ?: dto.homeTeamAlt
        val away = dto.awayTeam ?: dto.away ?: dto.awayTeamAlt
        val (hs, as_) = parseScore(dto.score)
        val (hHt, aHt) = parseScore(dto.htResult ?: dto.ht)
        val finished = dto.score != null || dto.goalless == true
        FcMatchRegistry.put(dto.id, lg.slug, home, away, dto.date ?: dto.matchDate, dto.matchSlug ?: dto.slug)
        FcTeamLeagueIndex.register(home, lg.slug)
        FcTeamLeagueIndex.register(away, lg.slug)
        // Warm the logo index with the canonical FC CDN logo for every team seen.
        listOfNotNull(home, away).forEach { name ->
            val ts = ApiConfig.teamSlug(name) ?: return@forEach
            FcLogoIndex.put(lg.slug, name, ApiConfig.teamLogoUrl(lg.country, lg.slug, ts))
        }
        // Fixtures rows carry date in match_date and status words like
        // "scheduled"/"finished"; results rows carry a score. Normalize both.
        val date = dto.date ?: dto.matchDate
        val time = dto.time?.takeIf { it.isNotBlank() }?.let { t -> if (t.length >= 5) t.take(5) else t }
        val rawStatus = dto.status?.lowercase()?.trim()
        val status = when {
            finished -> "Finished"
            rawStatus == "finished" || rawStatus == "ft" -> "Finished"
            rawStatus == "postponed" || rawStatus == "ppt" -> "Postponed"
            rawStatus == "cancelled" || rawStatus == "canceled" || rawStatus == "canc" -> "Cancelled"
            rawStatus == "suspended" || rawStatus == "susp" -> "Suspended"
            rawStatus != null && rawStatus.firstOrNull()?.isDigit() == true -> "In Play"
            else -> "Not Started"
        }
        val liveMinute = rawStatus?.takeWhile { it.isDigit() }?.toIntOrNull()
            ?.takeIf { status == "In Play" }
        val matchStatusOut = when {
            status == "In Play" && liveMinute != null -> "$liveMinute'"
            else -> status
        }
        return ApiEvent(
            match_id = dto.id?.toString() ?: dto.slug?.let { FcMatchRegistry.idForSlug(it) },
            country_name = lg.country,
            league_id = lg.appId.toString(),
            league_name = lg.name,
            match_date = date,
            match_time = time,
            match_status = matchStatusOut,
            match_hometeam_id = home?.let { FcTeamIds.id(it).toString() },
            match_hometeam_name = home,
            match_hometeam_score = hs?.toString(),
            match_awayteam_id = away?.let { FcTeamIds.id(it).toString() },
            match_awayteam_name = away,
            match_awayteam_score = as_?.toString(),
            match_hometeam_halftime_score = hHt?.toString(),
            match_awayteam_halftime_score = aHt?.toString(),
            match_hometeam_ft_score = if (finished) hs?.toString() else null,
            match_awayteam_ft_score = if (finished) as_?.toString() else null,
            league_year = season,
            team_home_badge = FcLogoIndex.forName(home),
            team_away_badge = FcLogoIndex.forName(away),
            goalscorer = null,
            cards = null,
            substitutions = null,
            statistics = null,
            lineup = null
        )
    }

    /** "2:1" / "2-1" -> (2, 1) */
    private fun parseScore(raw: String?): Pair<Int?, Int?> {
        if (raw.isNullOrBlank()) return null to null
        val cleaned = raw.replace("–", ":").replace("—", ":")
        val parts = if (cleaned.contains(":")) cleaned.split(":") else cleaned.split("-")
        return parts.getOrNull(0)?.trim()?.toIntOrNull() to parts.getOrNull(1)?.trim()?.toIntOrNull()
    }

    /** FC match_datetime is UTC ISO; the legacy mapper expects Europe/Berlin local time. */
    private fun berlinTime(isoUtc: String?, fallback: String?): String? {
        if (isoUtc.isNullOrBlank()) return fallback
        return try {
            val zoned = java.time.Instant.parse(isoUtc).atZone(java.time.ZoneId.of("Europe/Berlin"))
            String.format(Locale.US, "%02d:%02d", zoned.hour, zoned.minute)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun pct(v: Double?): String? = v?.let { ((it * 100).coerceIn(0.0, 100.0) * 10).roundToInt() / 10.0 }?.toString()
}
