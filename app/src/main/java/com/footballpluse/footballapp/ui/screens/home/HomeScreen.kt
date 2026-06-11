package com.footballpluse.footballapp.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.res.painterResource
import com.footballpluse.footballapp.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.data.model.LeagueResponse
import com.footballpluse.footballapp.ui.components.HeaderIcon
import com.footballpluse.footballapp.ui.theme.*
import com.footballpluse.footballapp.viewmodel.HomeViewModel
import com.footballpluse.footballapp.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.*

private data class Competition(
    val id: Int,
    val name: String,
    val leagueId: Int,
    val season: Int
)

private val INTERNATIONAL_COMPETITIONS = listOf(
    Competition(id = 1, name = "FIFA World Cup", leagueId = 1, season = 2026),
    Competition(id = 2, name = "Champions League", leagueId = 2, season = 2025),
    Competition(id = 3, name = "Europa League", leagueId = 3, season = 2025),
    Competition(id = 4, name = "UEFA Euros", leagueId = 4, season = 2024),
    Competition(id = 848, name = "Conf. League", leagueId = 848, season = 2025)
)

private val DOMESTIC_COMPETITIONS = listOf(
    Competition(id = 39, name = "Premier League", leagueId = 39, season = 2025),
    Competition(id = 140, name = "La Liga", leagueId = 140, season = 2025),
    Competition(id = 135, name = "Serie A", leagueId = 135, season = 2025),
    Competition(id = 78, name = "Bundesliga", leagueId = 78, season = 2025),
    Competition(id = 61, name = "Ligue 1", leagueId = 61, season = 2025),
    Competition(id = 88, name = "Eredivisie", leagueId = 88, season = 2025),
    Competition(id = 94, name = "Primeira Liga", leagueId = 94, season = 2025),
    Competition(id = 71, name = "Brasileirão", leagueId = 71, season = 2025),
    Competition(id = 253, name = "MLS", leagueId = 253, season = 2026),
    Competition(id = 262, name = "Liga MX", leagueId = 262, season = 2025)
)

private fun getLeagueCardColor(leagueId: Int): Color {
    return when (leagueId) {
        1 -> Color(0xFF1A462B)   // FIFA World Cup (Deep Green/Gold)
        2 -> Color(0xFF0F204C)   // Champions League (Midnight Navy)
        3 -> Color(0xFF4C2A0F)   // Europa League (Orange/Brown tint)
        4 -> Color(0xFF0B2E5E)   // Euros (Europe Blue)
        848 -> Color(0xFF143F24)  // Conference League (Teal/Green)
        39 -> Color(0xFF2C0F3A)   // Premier League (Deep Plum/Purple)
        140 -> Color(0xFF4C1014)  // La Liga (Crimson Red)
        135 -> Color(0xFF10335C)  // Serie A (Azure Blue)
        78 -> Color(0xFF4C0E11)   // Bundesliga (Dark Scarlet)
        61 -> Color(0xFF403C10)   // Ligue 1 (Muted Gold)
        88 -> Color(0xFF5E2A0B)   // Eredivisie (Orange-Brown)
        94 -> Color(0xFF143B1A)   // Primeira Liga (Forest Green)
        71 -> Color(0xFF2A4215)   // Brasileirão (Olive Green)
        253 -> Color(0xFF0B264F)  // MLS (Navy Blue)
        262 -> Color(0xFF0F3A30)  // Liga MX (Dark Teal)
        else -> Color(0xFF131620) // Default card color
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
    onNavigateToPlayerProfile: (Int) -> Unit = {},
    onNavigateToTopPlayers: () -> Unit = {},
    onNavigateToCompetitions: () -> Unit = {},
    onNavigateToClubInfo: (Int, Int) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is HomeUiState.Loading -> HomeLoadingShimmer()
        is HomeUiState.Error   -> HomeErrorScreen(state.message) { viewModel.loadHomeData() }
        is HomeUiState.Success -> {
            val formMap by viewModel.formMap.collectAsStateWithLifecycle()
            HomeContent(
                state                  = state,
                formMap                = formMap,
                onSearch               = onNavigateToSearch,
                onFavourites           = onNavigateToFavourites,
                onNotifications        = onNavigateToNotifications,
                onMatchClick           = onNavigateToMatchCenter,
                onNavigateToLeagues    = onNavigateToLeagues,
                onNavigateToLeagueDetail = onNavigateToLeagueDetail,
                onNavigateToFixtures   = onNavigateToFixtures,
                onRetry                = { viewModel.loadHomeData() },
                onFetchForm            = { teamId, leagueId, season ->
                    viewModel.fetchFormIfNeeded(teamId, leagueId, season)
                },
                onClubClick            = { teamId ->
                    onNavigateToClubInfo(teamId, 0)
                }
            )
        }
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF131620))
    )
}

@Composable
private fun HomeLoadingShimmer() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14)),
        contentPadding = PaddingValues(bottom = 56.dp)
    ) {
        // App Header Shimmer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(Modifier.width(140.dp).height(24.dp))
                    ShimmerBox(Modifier.width(80.dp).height(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { ShimmerBox(Modifier.size(40.dp), 20.dp) }
                }
            }
        }

        // Section Title Shimmer
        item {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ShimmerBox(Modifier.width(120.dp).height(16.dp))
            }
        }

        // Top Competitions Row Shimmer
        item {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    ShimmerBox(Modifier.width(120.dp).height(76.dp), 14.dp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Section Title Shimmer
        item {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ShimmerBox(Modifier.width(110.dp).height(16.dp))
            }
        }

        // Group Header and Match Shimmer
        repeat(3) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1E2A))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ShimmerBox(Modifier.width(150.dp).height(16.dp))
                }
            }
            repeat(2) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBox(Modifier.width(40.dp).height(14.dp))
                        Spacer(Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            ShimmerBox(Modifier.width(100.dp).height(12.dp))
                            ShimmerBox(Modifier.width(120.dp).height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚽", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Couldn't load matches",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF0D0F14)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyMatchesState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("⚽", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No matches scheduled for today",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF0D0F14)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Refresh", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    formMap: Map<Int, String>,
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
        (state.liveMatches + state.upcomingMatches.sortedBy { it.timestamp } + state.finishedMatches)
            .distinctBy { it.id }
    }

    val groupedMatches = remember(allToday) {
        allToday.groupBy { it.league.id }
            .map { (_, matches) -> matches }
            .sortedByDescending { group -> group.any { it.isLive } }
    }

    val leagueLogoMap = remember(state.topLeagues) {
        val apiLogos = state.topLeagues.associate { it.id to it.logo }
        val baseLeagueUrl = state.topLeagues.firstNotNullOfOrNull { it.logo }
            ?.let { url -> url.substringBeforeLast("/") }
        val allComps = INTERNATIONAL_COMPETITIONS + DOMESTIC_COMPETITIONS
        allComps.associate { comp ->
            val logo = apiLogos[comp.leagueId]
            val finalLogo = logo ?: if (baseLeagueUrl != null) {
                "$baseLeagueUrl/${comp.leagueId}.png"
            } else {
                "https://media.api-sports.io/football/leagues/${comp.leagueId}.png"
            }
            comp.leagueId to finalLogo
        }
    }

    val listState = rememberLazyListState()

    val leagueGroupMap = remember(groupedMatches) {
        groupedMatches.associateBy { "league_group_${it.first().league.id}" }
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

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14)),
        contentPadding = PaddingValues(bottom = 56.dp)
    ) {
        // ① APP HEADER
        item {
            AppHeader(
                onSearchClick = onSearch,
                onFavsClick = onFavourites,
                onNotifsClick = onNotifications
            )
            Spacer(Modifier.height(16.dp))
        }

        // ② TOP COMPETITIONS
        item {
            val liveLeagueIds = remember(state.liveMatches) {
                state.liveMatches.map { it.league.id }.toSet()
            }

            // Cups & International
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cups & International",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(INTERNATIONAL_COMPETITIONS, key = { it.leagueId }) { competition ->
                    val hasLive = competition.leagueId in liveLeagueIds
                    CompetitionCard(
                        leagueId = competition.leagueId,
                        leagueName = competition.name,
                        logoUrl = leagueLogoMap[competition.leagueId],
                        season = competition.season,
                        hasLiveMatches = hasLive,
                        onClick = {
                            onNavigateToLeagueDetail?.invoke(competition.leagueId, competition.season)
                                ?: onNavigateToLeagues()
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Domestic Leagues
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Domestic Leagues",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DOMESTIC_COMPETITIONS, key = { it.leagueId }) { competition ->
                    val hasLive = competition.leagueId in liveLeagueIds
                    CompetitionCard(
                        leagueId = competition.leagueId,
                        leagueName = competition.name,
                        logoUrl = leagueLogoMap[competition.leagueId],
                        season = competition.season,
                        hasLiveMatches = hasLive,
                        onClick = {
                            onNavigateToLeagueDetail?.invoke(competition.leagueId, competition.season)
                                ?: onNavigateToLeagues()
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ADD-ON 1: FAVORITES STRIP
        item {
            MyTeamsStrip(
                onEditFavorites = onFavourites,
                onClubClick = onClubClick
            )
        }

        // ③ TODAY'S MATCHES Group Header label
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Matches",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ③ Grouped Matches List
        if (groupedMatches.isEmpty()) {
            item {
                EmptyMatchesState(onRetry = onRetry)
            }
        } else {
            groupedMatches.forEach { group ->
                val league = group.first().league
                item(key = "league_group_${league.id}") {
                    var isExpanded by remember { mutableStateOf(true) }
                    Column {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)),
                            border = BorderStroke(0.5.dp, Color(0xFF1A1E2A))
                        ) {
                            Column {
                                val hasLive = group.any { it.isLive }
                                val liveCount = group.count { it.isLive }
                                LeagueGroupHeader(
                                    league = league,
                                    count = group.size,
                                    liveCount = liveCount,
                                    hasLive = hasLive,
                                    isExpanded = isExpanded,
                                    onToggleExpand = { isExpanded = !isExpanded }
                                )
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(animationSpec = tween(200)),
                                    exit = shrinkVertically(animationSpec = tween(200))
                                ) {
                                    Column {
                                        group.forEachIndexed { index, match ->
                                            val homeForm = formMap[match.homeTeam.id] ?: ""
                                            val awayForm = formMap[match.awayTeam.id] ?: ""
                                            MatchRow(
                                                match = match,
                                                homeForm = homeForm,
                                                awayForm = awayForm,
                                                onMatchClick = onMatchClick
                                            )
                                            if (index < group.lastIndex) {
                                                HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ① APP HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppHeader(
    onSearchClick: () -> Unit,
    onFavsClick: () -> Unit,
    onNotifsClick: () -> Unit
) {
    val today = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) }
    val unreadNotificationCount = 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row {
                Text(
                    text = "Football ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Plus",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }
            Text(
                text = today,
                fontSize = 11.sp,
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderIcon(
                icon = Icons.Default.Search,
                onClick = onSearchClick
            )
            HeaderIcon(
                icon = Icons.Default.FavoriteBorder,
                onClick = onFavsClick
            )
            HeaderIcon(
                icon = Icons.Default.Notifications,
                badgeCount = unreadNotificationCount,
                onClick = onNotifsClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ② TOP COMPETITIONS
// ─────────────────────────────────────────────────────────────────────────────
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
        modifier = Modifier
            .width(130.dp)
            .height(88.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = getLeagueCardColor(leagueId)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular White Container to ensure logo is clearly visible regardless of background color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = leagueName,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (hasLiveMatches) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF4444))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = leagueName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$season/${(season + 1) % 100}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ③ TODAY'S MATCHES — league group header + match row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeagueGroupHeader(
    league: LeagueInfo,
    count: Int,
    liveCount: Int,
    hasLive: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161A26))
            .drawBehind {
                // Draw green left accent border on the outer edge
                drawRect(
                    color = Color(0xFF00E676),
                    size = Size(3.dp.toPx(), size.height)
                )
            }
            .clickable { onToggleExpand() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = league.logo,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = league.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Win dot + label
                LegendDot(color = Color(0xFF00E676), label = "Win")
                // Draw dot + label
                LegendDot(color = Color(0xFF555555), label = "Draw")
                // Loss dot + label (outlined)
                LossLegendDot(label = "Loss")
                // trailing description
                Text(
                    text = "· last 5",
                    fontSize = 9.sp,
                    color = Color(0xFF333333)
                )
            }
        }
        val badgeBgColor = if (hasLive) Color(0xFF1A0A0A) else Color(0xFF1E2230)
        val badgeTextColor = if (hasLive) Color(0xFFFF4444) else Color(0xFF555555)
        val badgeBorderModifier = if (hasLive) Modifier.border(0.5.dp, Color(0xFF3A1212), RoundedCornerShape(4.dp)) else Modifier
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(badgeBgColor)
                .then(badgeBorderModifier)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasLive) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4444))
                    )
                    Text(
                        text = "$liveCount Live",
                        color = badgeTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "$count matches",
                        color = badgeTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (isExpanded)
                Icons.Default.KeyboardArrowUp
            else
                Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = Color(0xFF555555),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 9.sp, color = Color(0xFF555555))
    }
}

@Composable
fun LossLegendDot(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(1.dp, Color(0xFF555555), CircleShape)
        )
        Text(text = label, fontSize = 9.sp, color = Color(0xFF555555))
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
    val isLive = match.isLive
    val homeScore = match.homeScore ?: 0
    val awayScore = match.awayScore ?: 0
    val homeWinning = (isLive || isFinished) && homeScore > awayScore
    val awayWinning = (isLive || isFinished) && awayScore > homeScore

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onMatchClick(match.id.toString()) },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // LEFT — time or live minute, fixed width 42dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            if (isLive) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4444))
                    )
                    Text(
                        text = "${match.elapsed ?: 0}'",
                        fontSize = 11.sp,
                        color = Color(0xFFFF4444),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val timeLabel = remember(match.timestamp) {
                    try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(match.timestamp * 1000L)) }
                    catch (e: Exception) { "--:--" }
                }
                Text(
                    text = timeLabel,
                    fontSize = 11.sp,
                    color = Color(0xFF555555)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // CENTER — both teams stacked, weight 1f
        Column(modifier = Modifier.weight(1f)) {
            // HOME TEAM ROW
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = match.homeTeam.logo,
                    contentDescription = match.homeTeam.name,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(R.drawable.ic_team_placeholder),
                    error = painterResource(R.drawable.ic_team_placeholder)
                )
                Text(
                    text = match.homeTeam.name,
                    fontSize = 13.sp,
                    color = if (homeWinning) Color.White else Color(0xFFCCCCCC),
                    fontWeight = if (homeWinning) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // HOME FORM DOTS — 5 dots, all visible, no lock
            FormDotsRow(form = homeForm)

            Spacer(modifier = Modifier.height(6.dp))

            // AWAY TEAM ROW
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = match.awayTeam.logo,
                    contentDescription = match.awayTeam.name,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(R.drawable.ic_team_placeholder),
                    error = painterResource(R.drawable.ic_team_placeholder)
                )
                Text(
                    text = match.awayTeam.name,
                    fontSize = 13.sp,
                    color = if (awayWinning) Color.White else Color(0xFFCCCCCC),
                    fontWeight = if (awayWinning) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // AWAY FORM DOTS — 5 dots, all visible, no lock
            FormDotsRow(form = awayForm)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // RIGHT — score column, fixed width 32dp, end aligned
        if (isLive || isFinished) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(32.dp)
            ) {
                Text(
                    text = homeScore.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = awayScore.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ④ FAVORITES STRIP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MyTeamsStrip(
    onEditFavorites: () -> Unit,
    onClubClick: (Int) -> Unit,
    viewModel: com.footballpluse.footballapp.viewmodel.FavoritesViewModel = hiltViewModel()
) {
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()

    androidx.compose.animation.AnimatedVisibility(
        visible = favourites.isNotEmpty(),
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Teams",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable(
                            onClick = onEditFavorites,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Edit",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(favourites, key = { it.teamId }) { fav ->
                    FavoriteTeamCard(fav = fav, onClubClick = onClubClick)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FavoriteTeamCard(
    fav: com.footballpluse.footballapp.viewmodel.TeamWithNextFixture,
    onClubClick: (Int) -> Unit
) {
    var isImageError by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .height(56.dp)
            .wrapContentWidth()
            .clickable(
                onClick = { onClubClick(fav.teamId) },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620)),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Badge
            if (fav.badgeUrl.isBlank() || isImageError) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2433)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fav.teamName.take(1).uppercase(),
                        color = Color(0xFF00E676),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                AsyncImage(
                    model = fav.badgeUrl,
                    contentDescription = fav.teamName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    onError = { isImageError = true }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // RIGHT: Text Column
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = fav.teamName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 130.dp)
                )
                if (fav.isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4444))
                        )
                        Text(
                            text = "LIVE ${fav.liveMinute ?: 0}'",
                            color = Color(0xFFFF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                } else if (fav.opponentName != null && fav.kickoffTime != null) {
                    Text(
                        text = "vs ${fav.opponentName} · ${fav.kickoffTime}",
                        color = Color(0xFFCCCCCC),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                } else {
                    Text(
                        text = "No match today",
                        color = Color(0xFF555555),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesStripShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(Modifier.width(80.dp).height(14.dp))
            ShimmerBox(Modifier.width(40.dp).height(12.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(5) {
                Box(
                    modifier = Modifier
                        .width(170.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131620))
                        .border(0.5.dp, Color(0xFF1A1E2A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ShimmerBox(Modifier.size(36.dp), 18.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ShimmerBox(Modifier.width(80.dp).height(10.dp))
                            ShimmerBox(Modifier.width(60.dp).height(8.dp))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ⑤ FORM DOTS ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FormDotsRow(
    form: String,
    modifier: Modifier = Modifier.padding(start = 30.dp),
    dotSize: androidx.compose.ui.unit.Dp = 9.dp,
    gap: androidx.compose.ui.unit.Dp = 5.dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val results = form.takeLast(5).padStart(5, 'U')
        results.forEach { result ->
            when (result) {
                'W' -> Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                'D' -> Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color(0xFF555555))
                )
                'L' -> Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(1.dp, Color(0xFF555555), CircleShape)
                )
                else -> Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1E2A))
                )
            }
        }
    }
}
