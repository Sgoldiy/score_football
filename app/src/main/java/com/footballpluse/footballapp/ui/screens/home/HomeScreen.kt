package com.footballpluse.footballapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.remote.uitslagen.UitslagenApiService
import com.footballpluse.footballapp.data.remote.uitslagen.UitslagenLeagues
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.ui.components.BroadcastMatchCard
import com.footballpluse.footballapp.ui.components.FormDotsRow
import com.footballpluse.footballapp.ui.components.HeaderIcon
import com.footballpluse.footballapp.ui.components.LivePulse
import com.footballpluse.footballapp.ui.components.ScoreFlashText
import com.footballpluse.footballapp.viewmodel.HomeUiState
import com.footballpluse.footballapp.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

private val currentSeason = SeasonUtils.currentSeasonStartYear()

private fun leagueBadgeUrl(leagueId: Int, leagueName: String): String =
    "https://apiv3.apifootball.com/badges/logo_leagues/${leagueId}_${leagueName.lowercase().replace(' ', '-')}.png"

/* Top competitions (cups & international tournaments). Loaded directly from the
 * upstream API icon endpoint (https://uitslagen.live/footapi/images/flags/...). */
private data class TopCompetition(
    val id: Int,
    val key: String,
    val name: String,
    val logoUrl: String,
    val country: String
)

private val topCompetitions = listOf(
    TopCompetition(28, "EurocupsUEFAChampionsLeague", "Champions League", "https://uitslagen.live/footapi/images/flags/eurocups.png", "Europe"),
    TopCompetition(3, "EurocupsUEFAEuropaLeague", "Europa League", "https://uitslagen.live/footapi/images/flags/eurocups.png", "Europe"),
    TopCompetition(848, "EurocupsUEFAEuropaConferenceLeague", "Conference League", "https://uitslagen.live/footapi/images/flags/eurocups.png", "Europe"),
    TopCompetition(5, "EurocupsUEFANationsLeagueLeagueA", "Nations League", "https://uitslagen.live/footapi/images/flags/europe.png", "Europe"),
    TopCompetition(1, "InternationalWorldWorldCup", "World Cup", "https://uitslagen.live/footapi/images/flags/world-cup.png", "World"),
    TopCompetition(4, "InternationalEuroEuroChampionship", "EURO", "https://uitslagen.live/footapi/images/flags/euro2024.png", "Europe"),
    TopCompetition(9, "InternationalAmericaCopaAmerica", "Copa América", "https://uitslagen.live/footapi/images/flags/south-america.png", "S. America"),
    TopCompetition(17, "InternationalAsiaAFCAsianCup", "Asian Cup", "https://uitslagen.live/footapi/images/flags/asia.png", "Asia")
)

/* Skeleton of FC-covered leagues, used as merge fallback until the live list
 * arrives. Names/countries match the uitslagen adapter league map so byName matching
 * works; ids are the legacy ids those leagues are persisted under. */
private val hardcodedDomesticLeagues = listOf(
    LeagueInfo(id = 152, name = "Premier League", logo = "https://apiv3.apifootball.com/badges/logo_leagues/152_premier-league.png", country = "England", flag = UitslagenApiService.flagUrl("England"), season = currentSeason),
    LeagueInfo(id = 302, name = "LaLiga", logo = "https://apiv3.apifootball.com/badges/logo_leagues/302_la-liga.png", country = "Spain", flag = UitslagenApiService.flagUrl("Spain"), season = currentSeason),
    LeagueInfo(id = 207, name = "Serie A", logo = "https://apiv3.apifootball.com/badges/logo_leagues/207_serie-a.png", country = "Italy", flag = UitslagenApiService.flagUrl("Italy"), season = currentSeason),
    LeagueInfo(id = 175, name = "Bundesliga", logo = "https://apiv3.apifootball.com/badges/logo_leagues/175_bundesliga.png", country = "Germany", flag = UitslagenApiService.flagUrl("Germany"), season = currentSeason),
    LeagueInfo(id = 168, name = "Ligue 1", logo = "https://apiv3.apifootball.com/badges/logo_leagues/168_ligue-1.png", country = "France", flag = UitslagenApiService.flagUrl("France"), season = currentSeason),
    LeagueInfo(id = 88, name = "Eredivisie", logo = UitslagenLeagues.logoUrlFor(88, "Eredivisie", "Netherlands"), country = "Netherlands", flag = UitslagenApiService.flagUrl("Netherlands"), season = currentSeason),
    LeagueInfo(id = 94, name = "Liga Portugal", logo = "https://apiv3.apifootball.com/badges/logo_leagues/94_liga-portugal.png", country = "Portugal", flag = UitslagenApiService.flagUrl("Portugal"), season = currentSeason),
    LeagueInfo(id = 144, name = "Jupiler Pro League", logo = UitslagenLeagues.logoUrlFor(144, "Jupiler Pro League", "Belgium"), country = "Belgium", flag = UitslagenApiService.flagUrl("Belgium"), season = currentSeason),
    LeagueInfo(id = 203, name = "Saudi Professional League", logo = UitslagenLeagues.logoUrlFor(203, "Saudi Professional League", "Saudi Arabia"), country = "Saudi Arabia", flag = UitslagenApiService.flagUrl("Saudi Arabia"), season = currentSeason),
    LeagueInfo(id = 187, name = "Super League", logo = UitslagenLeagues.logoUrlFor(187, "Super League", "Switzerland"), country = "Switzerland", flag = UitslagenApiService.flagUrl("Switzerland"), season = currentSeason),
)

/* International competitions (UCL, World Cup, ...) are not covered by the
 * FootballCharts source, so there is no fallback rail for them anymore. */

private fun isToday(timestampMs: Long): Boolean {
    if (timestampMs <= 0L) return false
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    val today = fmt.format(cal.time)
    val day = fmt.format(Date(timestampMs))
    return day == today
}

private fun isTomorrow(timestampMs: Long): Boolean {
    if (timestampMs <= 0L) return false
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrow = fmt.format(cal.time)
    val day = fmt.format(Date(timestampMs))
    return day == tomorrow
}

private fun getLeagueCardColor(leagueId: Int): Color {
    return when (leagueId) {
        // Line 1: Top Domestic Leagues (Sharp, rich dark shades)
        152 -> Color(0xFF230D3A) // Premier League: Sharp Dark Purple / Violet
        302 -> Color(0xFF380812) // LaLiga: Sharp Deep Crimson Wine
        207 -> Color(0xFF0A2242) // Serie A: Sharp Dark Royal Navy
        175 -> Color(0xFF3A0D0E) // Bundesliga: Sharp Dark Maroon Brick
        168 -> Color(0xFF132238) // Ligue 1: Sharp Dark Slate Indigo
        88 -> Color(0xFF3E1E08)  // Eredivisie: Sharp Dark Amber
        94 -> Color(0xFF0B2C16)  // Liga Portugal: Sharp Dark Forest Green
        144 -> Color(0xFF0A262E) // Jupiler Pro League: Sharp Dark Teal
        203 -> Color(0xFF132B18) // Saudi Pro League: Sharp Dark Emerald
        187 -> Color(0xFF26102E) // Super League: Sharp Dark Midnight Plum

        // Line 2: Top Competitions (Sharp, distinct dark shades)
        28 -> Color(0xFF0A1838)  // Champions League: Sharp Deep Midnight Star Blue
        3 -> Color(0xFF341A06)   // Europa League: Sharp Deep Bronze / Amber
        848 -> Color(0xFF082E1E) // Conference League: Sharp Deep Emerald
        5 -> Color(0xFF121E36)   // Nations League: Sharp Deep Cobalt
        1 -> Color(0xFF330914)   // World Cup: Sharp Deep Crimson Burgundy
        4 -> Color(0xFF07213D)   // EURO: Sharp Deep Ocean Blue
        9 -> Color(0xFF0B2144)   // Copa América: Sharp Deep Sapphire
        17 -> Color(0xFF2C0A1B)  // Asian Cup: Sharp Deep Ruby / Claret

        else -> Color(0xFF131620)
    }
}

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToFavourites: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToMatchCenter: (String) -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToLeagueDetail: ((Int, Int) -> Unit)? = null,
    onNavigateToFixtures: () -> Unit = {},
    onNavigateToClubInfo: (Int, Int) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is HomeUiState.Loading -> HomeLoadingShimmer()
        is HomeUiState.Error -> HomeErrorScreen(state.message) { viewModel.loadHomeData() }
        is HomeUiState.Success -> {
            val formMap by viewModel.formMap.collectAsStateWithLifecycle()
            HomeContent(
                state = state,
                formMap = formMap,
                favouriteLeagueId = state.favouriteLeagueId,
                favouriteLeagueName = state.favouriteLeagueName,
                onSearch = onNavigateToSearch,
                onFavourites = onNavigateToFavourites,
                onNotifications = onNavigateToNotifications,
                onMatchClick = onNavigateToMatchCenter,
                onNavigateToLeagues = onNavigateToLeagues,
                onNavigateToLeagueDetail = onNavigateToLeagueDetail,
                onNavigateToFixtures = onNavigateToFixtures,
                onRetry = { viewModel.loadHomeData() },
                onFetchForm = { teamId, leagueId, season ->
                    viewModel.fetchFormIfNeeded(teamId, leagueId, season)
                },
                onClubClick = { teamId ->
                    onNavigateToClubInfo(teamId, 0)
                }
            )
        }
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(Color(0xFF131620)))
}

@Composable
private fun HomeLoadingShimmer() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F14))) {
        Spacer(Modifier.height(10.dp))
        AppHeader(
            onSearchClick = {},
            onFavsClick = {},
            onNotifsClick = {},
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 56.dp)) {
            item {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { ShimmerBox(Modifier.width(130.dp).height(88.dp), 16.dp) }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { ShimmerBox(Modifier.width(300.dp).height(140.dp), 14.dp) }
                }
            }
        }
    }
}

@Composable
private fun HomeErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F14)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("\u26BD", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(text = "Couldn't load data", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(text = message, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0D0F14)), shape = RoundedCornerShape(10.dp)) {
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyMatchesState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("\u26BD", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(text = "No matches scheduled for today", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0D0F14)), shape = RoundedCornerShape(8.dp)) {
                Text("Refresh", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

enum class HomeMatchTab(val label: String) {
    LIVE("Live"),
    FINISHED("Finished"),
    UPCOMING("Upcoming")
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    formMap: Map<Int, String>,
    favouriteLeagueId: Int,
    favouriteLeagueName: String,
    onSearch: () -> Unit,
    onFavourites: () -> Unit,
    onNotifications: () -> Unit,
    onMatchClick: (String) -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToLeagueDetail: ((Int, Int) -> Unit)?,
    onNavigateToFixtures: () -> Unit,
    onRetry: () -> Unit,
    onFetchForm: (Int, Int, Int) -> Unit,
    onClubClick: (Int) -> Unit
) {
    var selectedMatchTab by remember { mutableStateOf(HomeMatchTab.LIVE) }

    val allToday = remember(state.liveMatches, state.upcomingMatches, state.finishedMatches) {
        (state.liveMatches + state.upcomingMatches.sortedBy { it.timestamp } + state.finishedMatches).distinctBy { it.id }
    }
    val todayMatches = remember(allToday) { allToday.filter { isToday(it.timestamp * 1000L) } }
    val tomorrowMatches = remember(allToday) { allToday.filter { isTomorrow(it.timestamp * 1000L) } }

    val liveMatchesCount = remember(todayMatches) { todayMatches.count { it.isLive } }
    val finishedMatchesCount = remember(todayMatches) {
        todayMatches.count { !it.isLive && (it.status.short in listOf("FT", "AET", "PEN", "AP") || it.status.long.contains("Finish", ignoreCase = true)) }
    }
    val upcomingMatchesCount = remember(todayMatches) {
        todayMatches.count { !it.isLive && it.status.short !in listOf("FT", "AET", "PEN", "AP") && !it.status.long.contains("Finish", ignoreCase = true) }
    }

    val currentTabMatches = remember(todayMatches, selectedMatchTab) {
        when (selectedMatchTab) {
            HomeMatchTab.LIVE -> todayMatches.filter { it.isLive }
            HomeMatchTab.FINISHED -> todayMatches.filter {
                !it.isLive && (it.status.short in listOf("FT", "AET", "PEN", "AP") || it.status.long.contains("Finish", ignoreCase = true))
            }
            HomeMatchTab.UPCOMING -> todayMatches.filter {
                !it.isLive && it.status.short !in listOf("FT", "AET", "PEN", "AP") && !it.status.long.contains("Finish", ignoreCase = true)
            }
        }
    }

    val groupedMatches = remember(currentTabMatches, favouriteLeagueId) {
        // Group by league identity (id + name)
        currentTabMatches.groupBy { "${it.league.id}|${it.league.name}" }
            .map { (_, matches) -> matches }
            .sortedWith(
                compareByDescending<List<Match>> { group -> group.first().league.id == favouriteLeagueId }
                    .thenByDescending { group -> group.any { it.isLive } }
            )
    }

    val domesticLeagues = remember(state.topLeagues, state.allApiLeagues, favouriteLeagueId) {
        val apiMap = state.allApiLeagues.associateBy { it.id }
        val matchMap = state.topLeagues.associateBy { it.id }
        hardcodedDomesticLeagues.map { fallback ->
            val api = apiMap[fallback.id]
            val match = matchMap[fallback.id]
            when {
                api != null -> api.copy(
                    season = api.season ?: fallback.season,
                    logo = api.logo?.ifBlank { null } ?: fallback.logo
                )
                match != null -> match.copy(
                    season = match.season ?: fallback.season,
                    logo = match.logo?.ifBlank { null } ?: fallback.logo
                )
                else -> fallback
            }
        }.let { list ->
            if (favouriteLeagueId != 0 && list.none { it.id == favouriteLeagueId }) {
                val favFromApi = apiMap[favouriteLeagueId] ?: matchMap[favouriteLeagueId]
                if (favFromApi != null) listOf(favFromApi) + list else list
            } else {
                list
            }
        }
    }

    val listState = rememberLazyListState()

    val leagueGroupMap = remember(groupedMatches) {
        groupedMatches.associateBy { "lg_${it.first().league.id}" }
    }

    LaunchedEffect(allToday) {
        allToday.forEach { match ->
            val season = match.league.season ?: currentSeason
            onFetchForm(match.homeTeam.id, match.league.id, season)
            onFetchForm(match.awayTeam.id, match.league.id, season)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleKeys = listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
        val teamsToFetch = visibleKeys.mapNotNull { key ->
            leagueGroupMap[key]?.flatMap { match ->
                val season = match.league.season ?: currentSeason
                listOf(
                    Triple(match.homeTeam.id, match.league.id, season),
                    Triple(match.awayTeam.id, match.league.id, season)
                )
            }
        }.flatten().distinctBy { it.first }

        teamsToFetch.forEach { (teamId, leagueId, season) ->
            onFetchForm(teamId, leagueId, season)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F14))) {
        Spacer(Modifier.height(10.dp))
        AppHeader(
            onSearchClick = onSearch,
            onFavsClick = onFavourites,
            onNotifsClick = onNotifications,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 56.dp)
        ) {
            if (domesticLeagues.isNotEmpty()) {
                item(key = "domestic_header") {
                    LeagueSectionHeader("Top Leagues")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(domesticLeagues, key = { "dom_lg_${it.id}" }) { league ->
                            CompetitionCard(
                                leagueId = league.id,
                                leagueName = league.name,
                                logoUrl = league.logo ?: leagueBadgeUrl(league.id, league.name),
                                season = league.season ?: currentSeason,
                                hasLiveMatches = state.liveMatches.any { it.league.id == league.id },
                                onClick = { onNavigateToLeagueDetail?.invoke(league.id, league.season ?: currentSeason) ?: onNavigateToLeagues() }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            item(key = "top_competitions") {
                LeagueSectionHeader("Top Competitions")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(topCompetitions, key = { "comp_${it.key}" }) { comp ->
                        CompetitionCard(
                            leagueId = comp.id,
                            leagueName = comp.name,
                            logoUrl = comp.logoUrl,
                            season = currentSeason,
                            hasLiveMatches = state.liveMatches.any { it.league.id == comp.id || it.league.name.contains(comp.name, ignoreCase = true) },
                            emojiOverride = null,
                            subtitleOverride = comp.country,
                            onClick = { onNavigateToLeagueDetail?.invoke(comp.id, currentSeason) ?: onNavigateToLeagues() }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            item(key = "schedule_header") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        "Match Schedule",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HomeMatchTab.values().forEach { tab ->
                            val isSelected = selectedMatchTab == tab
                            val count = when (tab) {
                                HomeMatchTab.LIVE -> liveMatchesCount
                                HomeMatchTab.FINISHED -> finishedMatchesCount
                                HomeMatchTab.UPCOMING -> upcomingMatchesCount
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) {
                                            if (tab == HomeMatchTab.LIVE && count > 0) Color(0xFFFF4444).copy(alpha = 0.18f)
                                            else Color(0xFF00E676).copy(alpha = 0.18f)
                                        } else Color(0xFF131620)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) {
                                            if (tab == HomeMatchTab.LIVE && count > 0) Color(0xFFFF4444)
                                            else Color(0xFF00E676)
                                        } else Color(0xFF1A1E2A),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedMatchTab = tab }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (tab == HomeMatchTab.LIVE && count > 0) {
                                        LivePulse(color = Color(0xFFFF4444), dotSize = 6.dp)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = tab.label,
                                        color = if (isSelected) {
                                            if (tab == HomeMatchTab.LIVE && count > 0) Color(0xFFFF4444)
                                            else Color(0xFF00E676)
                                        } else Color(0xFF8B949E),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (count > 0) {
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = "($count)",
                                            color = if (isSelected) {
                                                if (tab == HomeMatchTab.LIVE && count > 0) Color(0xFFFF4444)
                                                else Color(0xFF00E676)
                                            } else Color(0xFF555555),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (groupedMatches.isEmpty()) {
                item(key = "tab_empty_${selectedMatchTab.name}") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (selectedMatchTab) {
                                    HomeMatchTab.LIVE -> "No matches currently live"
                                    HomeMatchTab.FINISHED -> "No finished matches today"
                                    HomeMatchTab.UPCOMING -> "No upcoming matches scheduled today"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = when (selectedMatchTab) {
                                    HomeMatchTab.LIVE -> "Select Finished or Upcoming to view fixtures"
                                    else -> "Check other tabs for match updates"
                                },
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                groupedMatches.forEachIndexed { groupIdx, group ->
                    val league = group.first().league
                    // Key by league ID + first match ID + index: guaranteed unique
                    item(key = "lg_${league.id}_${group.first().id}_$groupIdx") {
                        var isExpanded by remember { mutableStateOf(true) }
                        val isMyLeague = league.id == favouriteLeagueId
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)),
                            border = BorderStroke(0.5.dp, if (isMyLeague) Color(0xFF00E676).copy(alpha = 0.4f) else Color(0xFF1A1E2A))
                        ) {
                            Column {
                                LeagueGroupHeader(
                                    league = league,
                                    count = group.size,
                                    liveCount = group.count { it.isLive },
                                    hasLive = group.any { it.isLive },
                                    isMyLeague = isMyLeague,
                                    isExpanded = isExpanded,
                                    onToggleExpand = { isExpanded = !isExpanded }
                                )
                                AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                                    Column {
                                        group.forEachIndexed { idx, match ->
                                            MatchRow(
                                                match = match,
                                                homeForm = formMap[match.homeTeam.id] ?: "",
                                                awayForm = formMap[match.awayTeam.id] ?: "",
                                                onMatchClick = onMatchClick
                                            )
                                            if (idx < group.lastIndex) {
                                                HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedMatchTab == HomeMatchTab.UPCOMING && tomorrowMatches.isNotEmpty()) {
                item(key = "tomorrow_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tomorrow", color = Color(0xFF00E676), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                tomorrowMatches.forEachIndexed { idx, match ->
                    item(key = "tomorrow_match_${match.id}_$idx") {
                        MatchRow(
                            match = match,
                            homeForm = formMap[match.homeTeam.id] ?: "",
                            awayForm = formMap[match.awayTeam.id] ?: "",
                            onMatchClick = onMatchClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueSectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppHeader(
    onSearchClick: () -> Unit,
    onFavsClick: () -> Unit,
    onNotifsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Football ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Plus", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderIcon(Icons.Default.Search, onClick = onSearchClick)
            HeaderIcon(Icons.Default.FavoriteBorder, onClick = onFavsClick)
            HeaderIcon(Icons.Default.Notifications, onClick = onNotifsClick)
        }
    }
}

@Composable
private fun CompetitionCard(
    leagueId: Int,
    leagueName: String,
    logoUrl: String?,
    season: Int,
    hasLiveMatches: Boolean,
    onClick: () -> Unit,
    /** Emoji mark shown when no logo url exists (competitions). */
    emojiOverride: String? = null,
    /** Replaces the season line when set (e.g. competition region). */
    subtitleOverride: String? = null
) {
    Card(
        modifier = Modifier.width(130.dp).height(88.dp).clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = getLeagueCardColor(leagueId)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoUrl != null) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = leagueName,
                            modifier = Modifier.size(20.dp),
                            placeholder = painterResource(R.drawable.ic_league_placeholder),
                            error = painterResource(R.drawable.ic_league_placeholder)
                        )
                    } else {
                        Text(text = emojiOverride ?: "", fontSize = 14.sp)
                    }
                }
                if (hasLiveMatches) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF4444)).padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePulse(color = Color.White, dotSize = 4.dp)
                            Spacer(Modifier.width(3.dp))
                            Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Column {
                Text(leagueName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitleOverride ?: "$season/${(season + 1) % 100}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun LeagueGroupHeader(
    league: LeagueInfo,
    count: Int,
    liveCount: Int,
    hasLive: Boolean,
    isMyLeague: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val leagueLogo = league.logo ?: leagueBadgeUrl(league.id, league.name)
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (isMyLeague) Color(0xFF0D1A0D) else Color(0xFF161A26))
            .drawBehind {
                drawRect(
                    color = Color(0xFF00E676),
                    size = Size(if (isMyLeague) 4.dp.toPx() else 3.dp.toPx(), size.height)
                )
            }
            .clickable { onToggleExpand() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = leagueLogo,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            placeholder = painterResource(R.drawable.ic_league_placeholder),
            error = painterResource(R.drawable.ic_league_placeholder)
        )
        Spacer(Modifier.width(8.dp))
        val canonicalCountry = remember(league.id, league.name, league.country) {
            com.footballpluse.footballapp.data.remote.uitslagen.UitslagenLeagues.countryFor(league.id, league.name, league.country)
        }
        val flagUrl = remember(canonicalCountry) {
            com.footballpluse.footballapp.data.remote.uitslagen.UitslagenApiService.flagUrl(canonicalCountry)
        }
        if (!flagUrl.isNullOrBlank()) {
            AsyncImage(
                model = flagUrl,
                contentDescription = canonicalCountry,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(width = 14.dp, height = 10.dp)
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_placeholder)
            )
        }
        Text(
            if (canonicalCountry.isNotBlank() && canonicalCountry != "World" && canonicalCountry != "Europe") {
                "$canonicalCountry · ${league.name}"
            } else {
                league.name
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val badgeColor = if (hasLive) Color(0xFFFF4444) else Color(0xFF555555)
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .background(if (hasLive) Color(0xFF1A0A0A) else Color(0xFF1E2230))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                if (hasLive) "$liveCount Live" else "$count matches",
                color = badgeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFF555555),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MatchRow(
    match: Match,
    homeForm: String,
    awayForm: String,
    onMatchClick: (String) -> Unit
) {
    val isFinished = match.status.short in listOf("FT", "AET", "PEN", "AP")
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable { onMatchClick(match.id.toString()) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            if (match.isLive) {
                Text(
                    "${match.elapsed ?: 0}'",
                    fontSize = 11.sp,
                    color = Color(0xFFFF4444),
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    try {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(match.timestamp * 1000L))
                    } catch (e: Exception) { "--:--" },
                    fontSize = 11.sp,
                    color = Color(0xFF555555)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            TeamRowItem(
                name = match.homeTeam.name,
                logo = match.homeTeam.logo,
                score = match.homeScore,
                form = homeForm,
                showScore = match.isLive || isFinished
            )
            Spacer(Modifier.height(8.dp))
            TeamRowItem(
                name = match.awayTeam.name,
                logo = match.awayTeam.logo,
                score = match.awayScore,
                form = awayForm,
                showScore = match.isLive || isFinished
            )
        }
    }
}

@Composable
private fun TeamRowItem(
    name: String,
    logo: String?,
    score: Int?,
    form: String,
    showScore: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = logo,
            contentDescription = null,
            modifier = Modifier.size(22.dp).clip(CircleShape),
            placeholder = painterResource(R.drawable.ic_team_placeholder)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            FormDotsRow(
                form = form,
                dotSize = 7.dp,
                gap = 4.dp,
                modifier = Modifier
            )
        }
        if (showScore) {
            // Goal-change flash on background refresh, matching the live list rows.
            ScoreFlashText(
                text = score?.toString() ?: "0",
                fontSize = 14.sp,
                baseColor = Color.White
            )
        }
    }
}

@Composable
private fun MyTeamsStrip(
    onEditFavorites: () -> Unit,
    onClubClick: (Int) -> Unit,
    viewModel: com.footballpluse.footballapp.viewmodel.FavoritesViewModel = hiltViewModel()
) {
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    if (favourites.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Teams", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Edit",
                    color = Color(0xFF00E676),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onEditFavorites() }
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favourites) { fav ->
                    Card(
                        modifier = Modifier.height(56.dp).clickable { onClubClick(fav.teamId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = fav.badgeUrl,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                fav.teamName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
