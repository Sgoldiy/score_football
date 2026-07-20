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
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.ui.components.BroadcastMatchCard
import com.footballpluse.footballapp.ui.components.FormDotsRow
import com.footballpluse.footballapp.ui.components.HeaderIcon
import com.footballpluse.footballapp.ui.components.LivePulse
import com.footballpluse.footballapp.viewmodel.HomeUiState
import com.footballpluse.footballapp.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

private fun leagueBadgeUrl(leagueId: Int, leagueName: String): String =
    "https://apiv3.apifootball.com/badges/logo_leagues/${leagueId}_${leagueName.lowercase().replace(' ', '-')}.png"

private val hardcodedDomesticLeagues = listOf(
    LeagueInfo(id = 152, name = "Premier League", logo = "https://apiv3.apifootball.com/badges/logo_leagues/152_premier-league.png", country = "England", flag = null, season = 2025),
    LeagueInfo(id = 302, name = "La Liga", logo = "https://apiv3.apifootball.com/badges/logo_leagues/302_la-liga.png", country = "Spain", flag = null, season = 2025),
    LeagueInfo(id = 207, name = "Serie A", logo = "https://apiv3.apifootball.com/badges/logo_leagues/207_serie-a.png", country = "Italy", flag = null, season = 2025),
    LeagueInfo(id = 175, name = "Bundesliga", logo = "https://apiv3.apifootball.com/badges/logo_leagues/175_bundesliga.png", country = "Germany", flag = null, season = 2025),
    LeagueInfo(id = 168, name = "Ligue 1", logo = "https://apiv3.apifootball.com/badges/logo_leagues/168_ligue-1.png", country = "France", flag = null, season = 2025),
    LeagueInfo(id = 88, name = "Eredivisie", logo = "https://apiv3.apifootball.com/badges/logo_leagues/88_eredivisie.png", country = "Netherlands", flag = null, season = 2025),
    LeagueInfo(id = 94, name = "Liga Portugal", logo = "https://apiv3.apifootball.com/badges/logo_leagues/94_liga-portugal.png", country = "Portugal", flag = null, season = 2025),
    LeagueInfo(id = 203, name = "Super Lig", logo = "https://apiv3.apifootball.com/badges/logo_leagues/203_super-lig.png", country = "Turkey", flag = null, season = 2025),
    LeagueInfo(id = 144, name = "Jupiler Pro League", logo = "https://apiv3.apifootball.com/badges/logo_leagues/144_jupiler-pro-league.png", country = "Belgium", flag = null, season = 2025),
    LeagueInfo(id = 187, name = "Liga MX", logo = "https://apiv3.apifootball.com/badges/logo_leagues/187_liga-mx.png", country = "Mexico", flag = null, season = 2025),
    LeagueInfo(id = 188, name = "Serie A", logo = "https://apiv3.apifootball.com/badges/logo_leagues/188_serie-a.png", country = "Brazil", flag = null, season = 2025),
    LeagueInfo(id = 169, name = "Championship", logo = "https://apiv3.apifootball.com/badges/logo_leagues/169_championship.png", country = "England", flag = null, season = 2025),
)

private val hardcodedInternationalLeagues = listOf(
    LeagueInfo(id = 3, name = "Champions League", logo = "https://apiv3.apifootball.com/badges/logo_leagues/3_uefa-champions-league.png", country = "World", flag = null, season = 2025),
    LeagueInfo(id = 4, name = "Europa League", logo = "https://apiv3.apifootball.com/badges/logo_leagues/4_uefa-europa-league.png", country = "World", flag = null, season = 2025),
    LeagueInfo(id = 848, name = "Conference League", logo = "https://apiv3.apifootball.com/badges/logo_leagues/848_uefa-conference-league.png", country = "World", flag = null, season = 2025),
    LeagueInfo(id = 28, name = "FIFA World Cup", logo = "https://apiv3.apifootball.com/badges/logo_leagues/28_world-cup.png", country = "World", flag = null, season = 2026),
    LeagueInfo(id = 1, name = "UEFA Euro", logo = "https://apiv3.apifootball.com/badges/logo_leagues/1_uefa-european-championship.png", country = "World", flag = null, season = 2028),
    LeagueInfo(id = 5, name = "Nations League", logo = "https://apiv3.apifootball.com/badges/logo_leagues/5_uefa-nations-league.png", country = "World", flag = null, season = 2025),
    LeagueInfo(id = 6, name = "Copa America", logo = "https://apiv3.apifootball.com/badges/logo_leagues/6_copa-america.png", country = "World", flag = null, season = 2024),
    LeagueInfo(id = 15, name = "Club World Cup", logo = "https://apiv3.apifootball.com/badges/logo_leagues/15_fifa-club-world-cup.png", country = "World", flag = null, season = 2025),
    LeagueInfo(id = 9, name = "Copa Libertadores", logo = "https://apiv3.apifootball.com/badges/logo_leagues/9_copa-libertadores.png", country = "World", flag = null, season = 2025),
    LeagueInfo(id = 17, name = "AFF Championship", logo = "https://apiv3.apifootball.com/badges/logo_leagues/17_aff-championship.png", country = "World", flag = null, season = 2025),
)

private fun getLeagueCardColor(leagueId: Int): Color {
    return when (leagueId) {
        28 -> Color(0xFF1A462B)
        3 -> Color(0xFF0F204C)
        4 -> Color(0xFF4C2A0F)
        1 -> Color(0xFF0B2E5E)
        9 -> Color(0xFF2A1A3A)
        848 -> Color(0xFF143F24)
        5 -> Color(0xFF2A1A0F)
        6 -> Color(0xFF0F2A1A)
        15 -> Color(0xFF1A3A1A)
        152 -> Color(0xFF2C0F3A)
        302 -> Color(0xFF4C1014)
        207 -> Color(0xFF10335C)
        175 -> Color(0xFF4C0E11)
        168 -> Color(0xFF403C10)
        88 -> Color(0xFF5E2A0B)
        94 -> Color(0xFF143B1A)
        203 -> Color(0xFF3A0F0F)
        144 -> Color(0xFF0F2A3A)
        187 -> Color(0xFF1A1A4C)
        188 -> Color(0xFF2A1A0F)
        169 -> Color(0xFF1A2A1A)
        17 -> Color(0xFF0F204C)
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
        AppHeader(onSearchClick = {}, onFavsClick = {}, onNotifsClick = {}, modifier = Modifier.height(60.dp).padding(horizontal = 16.dp))
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
    val allToday = remember(state.liveMatches, state.upcomingMatches, state.finishedMatches) {
        (state.liveMatches + state.upcomingMatches.sortedBy { it.timestamp } + state.finishedMatches).distinctBy { it.id }
    }
    val groupedMatches = remember(allToday, favouriteLeagueId) {
        allToday.groupBy { it.league.id }
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
                api != null -> api.copy(season = api.season ?: fallback.season)
                match != null -> match
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

    val internationalLeagues = remember(state.topLeagues, state.allApiLeagues) {
        val apiMap = state.allApiLeagues.associateBy { it.id }
        val matchMap = state.topLeagues.associateBy { it.id }
        hardcodedInternationalLeagues.map { fallback ->
            val api = apiMap[fallback.id]
            val match = matchMap[fallback.id]
            when {
                api != null -> api.copy(season = api.season ?: fallback.season)
                match != null -> match
                else -> fallback
            }
        }
    }

    val listState = rememberLazyListState()

    val leagueGroupMap = remember(groupedMatches) {
        groupedMatches.associateBy { "lg_${it.first().league.id}" }
    }

    LaunchedEffect(allToday) {
        allToday.forEach { match ->
            val season = match.league.season ?: 2025
            onFetchForm(match.homeTeam.id, match.league.id, season)
            onFetchForm(match.awayTeam.id, match.league.id, season)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleKeys = listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
        val teamsToFetch = visibleKeys.mapNotNull { key ->
            leagueGroupMap[key]?.flatMap { match ->
                val season = match.league.season ?: 2025
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
                                season = league.season ?: 2025,
                                hasLiveMatches = state.liveMatches.any { it.league.id == league.id },
                                onClick = { onNavigateToLeagueDetail?.invoke(league.id, league.season ?: 2025) ?: onNavigateToLeagues() }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (internationalLeagues.isNotEmpty()) {
                item(key = "international_header") {
                    LeagueSectionHeader("International & Cups")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(internationalLeagues, key = { "int_lg_${it.id}" }) { league ->
                            CompetitionCard(
                                leagueId = league.id,
                                leagueName = league.name,
                                logoUrl = league.logo ?: leagueBadgeUrl(league.id, league.name),
                                season = league.season ?: 2025,
                                hasLiveMatches = state.liveMatches.any { it.league.id == league.id },
                                onClick = { onNavigateToLeagueDetail?.invoke(league.id, league.season ?: 2025) ?: onNavigateToLeagues() }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (state.liveMatches.isNotEmpty()) {
                item(key = "live_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Live Now", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            LivePulse()
                        }
                        Text(
                            "See all",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onNavigateToFixtures() }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.liveMatches, key = { "live_${it.id}" }) { match ->
                            BroadcastMatchCard(
                                match = match,
                                modifier = Modifier.width(300.dp),
                                onClick = { onMatchClick(match.id.toString()) },
                                homeForm = formMap[match.homeTeam.id] ?: "",
                                awayForm = formMap[match.awayTeam.id] ?: ""
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            item(key = "my_teams") {
                MyTeamsStrip(onEditFavorites = onFavourites, onClubClick = onClubClick)
                Text(
                    "Match Schedule",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            if (groupedMatches.isEmpty()) {
                item(key = "empty") { EmptyMatchesState(onRetry = onRetry) }
            } else {
                groupedMatches.forEach { group ->
                    val league = group.first().league
                    item(key = "lg_${league.id}") {
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
        modifier = modifier,
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
    onClick: () -> Unit
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
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = leagueName,
                        modifier = Modifier.size(20.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_placeholder)
                    )
                }
                if (hasLiveMatches) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF4444)).padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Column {
                Text(leagueName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$season/${(season + 1) % 100}", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
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
            placeholder = painterResource(R.drawable.ic_placeholder),
            error = painterResource(R.drawable.ic_placeholder)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            league.name,
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
    val isFinished = match.status.short in listOf("FT", "AET", "PEN")
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
            Text(
                score?.toString() ?: "0",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
