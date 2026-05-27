package com.example.footballapp.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
import com.example.footballapp.domain.model.LeagueInfo
import com.example.footballapp.domain.model.Match
import com.example.footballapp.ui.components.CompetitionCard

import com.example.footballapp.ui.components.StatPill
import com.example.footballapp.ui.theme.*
import com.example.footballapp.viewmodel.HomeViewModel
import com.example.footballapp.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.*

private data class Competition(
    val id: Int,
    val name: String,
    val leagueId: Int,
    val season: Int
)

private val HOME_COMPETITIONS = listOf(
    Competition(id = 39, name = "Premier League", leagueId = 39, season = 2024),
    Competition(id = 140, name = "La Liga", leagueId = 140, season = 2024),
    Competition(id = 78, name = "Bundesliga", leagueId = 78, season = 2024),
    Competition(id = 135, name = "Serie A", leagueId = 135, season = 2024),
    Competition(id = 61, name = "Ligue 1", leagueId = 61, season = 2024),
    Competition(id = 2, name = "Champions League", leagueId = 2, season = 2024),
    Competition(id = 1, name = "FIFA World Cup", leagueId = 1, season = 2026)
)

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToFavourites: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToMatchCenter: (String) -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToLeagueDetail: ((Int, Int) -> Unit)? = null,
    onNavigateToFixtures: () -> Unit = {},
    onNavigateToPlayerProfile: (Int) -> Unit,
    onNavigateToTopPlayers: () -> Unit = {},
    onNavigateToCompetitions: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is HomeUiState.Loading -> HomeLoadingShimmer()
        is HomeUiState.Error   -> HomeErrorScreen(state.message) { viewModel.loadHomeData() }
        is HomeUiState.Success -> HomeContent(
            state                  = state,
            onSearch               = onNavigateToSearch,
            onFavourites           = onNavigateToFavourites,
            onNotifications        = onNavigateToNotifications,
            onMatchClick           = onNavigateToMatchCenter,
            onExplorePlayers       = onNavigateToTopPlayers,
            onPlayerClick          = onNavigateToPlayerProfile,
            onNavigateToLeagues    = onNavigateToLeagues,
            onNavigateToLeagueDetail = onNavigateToLeagueDetail,
            onNavigateToFixtures   = onNavigateToFixtures
        )
    }
}

@Composable
private fun HomeLoadingShimmer() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ShimmerBox(Modifier.fillMaxWidth().height(60.dp), 16.dp) }
        item { ShimmerBox(Modifier.fillMaxWidth().height(40.dp), 10.dp) }
        item { ShimmerBox(Modifier.fillMaxWidth(0.45f).height(18.dp), 6.dp) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { repeat(5) { ShimmerBox(Modifier.width(110.dp).height(44.dp), 50.dp) } } }
        item { ShimmerBox(Modifier.fillMaxWidth(0.45f).height(18.dp), 6.dp) }
        repeat(4) { item { ShimmerBox(Modifier.fillMaxWidth().height(58.dp), 12.dp) } }
        item { ShimmerBox(Modifier.fillMaxWidth(0.45f).height(18.dp), 6.dp) }
        repeat(3) { item { ShimmerBox(Modifier.fillMaxWidth().height(76.dp), 16.dp) } }
    }
}

@Composable
private fun HomeErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(DeepNavy), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚽", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Couldn't load matches", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = GlassGlowGreen, contentColor = DeepNavy), shape = RoundedCornerShape(12.dp)) {
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onSearch: () -> Unit,
    onFavourites: () -> Unit,
    onNotifications: () -> Unit,
    onMatchClick: (String) -> Unit,
    onExplorePlayers: () -> Unit,
    onPlayerClick: (Int) -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToLeagueDetail: ((Int, Int) -> Unit)? = null,
    onNavigateToFixtures: () -> Unit = {}
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

    val liveCount = state.liveMatches.size
    val totalCount = allToday.size
    val finishedCount = state.finishedMatches.size

    val leagueLogoMap = remember(state.topLeagues) {
        val apiLogos = state.topLeagues.associate { it.id to it.logo }
        val baseLeagueUrl = state.topLeagues.firstNotNullOfOrNull { it.logo }
            ?.let { url -> url.substringBeforeLast("/") }
        HOME_COMPETITIONS.associate { comp ->
            val logo = apiLogos[comp.leagueId]
            if (logo != null) {
                comp.leagueId to logo
            } else if (baseLeagueUrl != null) {
                comp.leagueId to "$baseLeagueUrl/${comp.leagueId}.png"
            } else {
                comp.leagueId to null
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ① Smart Header
        item {
            SmartHeader(
                liveCount     = liveCount,
                onSearch      = onSearch,
                onFavourites  = onFavourites,
                onNotifications = onNotifications
            )
        }

        // ② Stats Ticker — pill strip
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                item {
                    StatPill(
                        icon = "●",
                        label = "$liveCount Live Now",
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                        contentColor = Color(0xFFEF4444)
                    )
                }
                item {
                    StatPill(
                        icon = "⚽",
                        label = "$totalCount Matches Today",
                        containerColor = Color(0xFF22C55E).copy(alpha = 0.15f),
                        contentColor = Color(0xFF22C55E)
                    )
                }
                item {
                    StatPill(
                        icon = "✓",
                        label = "$finishedCount Finished",
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        contentColor = Color(0xFF3B82F6)
                    )
                }
            }
        }

        // ③ Top Competitions — horizontal swipe row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .background(Color(0xFF22C55E), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Top Competitions",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "See All →",
                    color = Color(0xFF22C55E),
                    fontSize = 13.sp
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(HOME_COMPETITIONS, key = { it.leagueId }) { competition ->
                    CompetitionCard(
                        leagueId = competition.leagueId,
                        leagueName = competition.name,
                        logoUrl = leagueLogoMap[competition.leagueId],
                        season = competition.season,
                        onClick = {
                            onNavigateToLeagueDetail?.invoke(competition.leagueId, competition.season)
                                ?: onNavigateToLeagues()
                        }
                    )
                }
            }
        }

        // ⑤ Today's Matches grouped by league
        if (groupedMatches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .background(Color(0xFF22C55E), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Today's Matches",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "All Fixtures →",
                        color = Color(0xFF22C55E),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onNavigateToFixtures() }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            groupedMatches.forEach { group ->
                val league = group.first().league
                item(key = "league_header_${league.id}") {
                    LeagueGroupHeader(league = league, count = group.size)
                }
                group.forEach { match ->
                    item(key = "match_${match.id}") {
                        TodayMatchRow(
                            match        = match,
                            onMatchClick = onMatchClick
                        )
                    }
                }
                item(key = "league_spacer_${league.id}") {
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }

        // ⑥ Top Performers — ranking cards
        if (state.topScorers.isNotEmpty()) {
            item {
                SectionLabel(
                    title           = "Top Performers",
                    trailing        = "Full Stats →",
                    onTrailingClick = onExplorePlayers
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Top 5 European Leagues",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(14.dp))
            }
            itemsIndexed(state.topScorers.take(10)) { index, entry ->
                TopPerformerCard(rank = index + 1, entry = entry, onPlayerClick = onPlayerClick)
                Spacer(Modifier.height(8.dp))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        // ⑦ Fan Zone Poll
        item {
            SectionLabel("Fan Zone")
            Spacer(Modifier.height(12.dp))
            HomeFanZonePoll()
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ① SMART HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SmartHeader(
    liveCount: Int,
    onSearch: () -> Unit,
    onFavourites: () -> Unit,
    onNotifications: () -> Unit
) {
    val today = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Football",
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = Color.White
                )
                Text(
                    text = "Plus",
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = GlassGlowGreen
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = today, fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
                if (liveCount > 0) {
                    LiveCountChip(liveCount)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconBtn(Icons.Rounded.Search, "Search", onSearch)
            GlassIconBtn(Icons.Rounded.FavoriteBorder, "Favourites", onFavourites)
            Box {
                GlassIconBtn(Icons.Rounded.Notifications, "Notifications", onNotifications)
                if (liveCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336))
                            .border(2.dp, DeepNavy, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCountChip(count: Int) {
    val pulse by rememberInfiniteTransition(label = "live-chip")
        .animateFloat(0.5f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "lc")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF44336).copy(alpha = 0.16f))
            .border(1.dp, Color(0xFFF44336).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFF44336).copy(alpha = pulse)))
        Text(text = "$count LIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF44336), letterSpacing = 0.5.sp)
    }
}

@Composable
private fun GlassIconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION LABEL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(title: String, trailing: String? = null, onTrailingClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp).height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassGlowGreen)
            )
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
        }
        if (trailing != null) {
            Text(
                text = trailing,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = GlassGlowGreen.copy(alpha = 0.8f),
                modifier = Modifier.clickable { onTrailingClick?.invoke() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TODAY'S MATCHES — league group header + match row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeagueGroupHeader(league: LeagueInfo, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(model = league.logo, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = league.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(GlassGlowGreen.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(text = "$count", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GlassGlowGreen)
        }
    }
}

@Composable
private fun TodayMatchRow(match: Match, onMatchClick: (String) -> Unit) {
    val isFinished = match.status.short in listOf("FT", "AET", "PEN")
    val showScore = isFinished || match.isLive

    val borderColor = when {
        match.isLive -> GlassGlowGreen.copy(alpha = 0.5f)
        else         -> Color.White.copy(alpha = 0.05f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clickable { onMatchClick(match.id.toString()) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (match.isLive) GlassGlowGreen.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.03f)
        ),
        border = BorderStroke(if (match.isLive) 1.dp else 0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = match.homeTeam.name,
                    fontSize = 13.sp,
                    fontWeight = if (match.isLive) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                AsyncImage(model = match.homeTeam.logo, contentDescription = null, modifier = Modifier.size(26.dp))
            }

            // Centre
            Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
                if (showScore) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${match.homeScore ?: 0}  –  ${match.awayScore ?: 0}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (match.isLive) GlassGlowGreen else Color.White,
                            textAlign = TextAlign.Center
                        )
                        if (match.isLive) {
                            val pulse by rememberInfiniteTransition(label = "match-live-${match.id}")
                                .animateFloat(0.5f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "mlp")
                            Text(
                                text = "● ${match.elapsed ?: 0}'",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GlassGlowGreen.copy(alpha = pulse)
                            )
                        } else {
                            Text(
                                text = if (match.status.short == "AET") "AET" else "FT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                } else {
                    val timeLabel = remember(match.timestamp) {
                        try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(match.timestamp * 1000L)) }
                        catch (e: Exception) { "--:--" }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = timeLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.75f))
                        Text(text = "vs", fontSize = 9.sp, color = Color.White.copy(alpha = 0.25f))
                    }
                }
            }

            // Away
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                AsyncImage(model = match.awayTeam.logo, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = match.awayTeam.name,
                    fontSize = 13.sp,
                    fontWeight = if (match.isLive) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP PERFORMERS — ranking cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopPerformerCard(rank: Int, entry: PlayerProfileStatisticsResponse, onPlayerClick: (Int) -> Unit) {
    val player = entry.player ?: return
    val stats  = entry.statistics?.firstOrNull()
    val goals  = stats?.goals?.total ?: 0
    val assists = stats?.goals?.assists ?: 0
    val rating  = stats?.games?.rating?.let { "%.1f".format(it.toFloatOrNull() ?: 0f) } ?: "-"
    val leagueName = stats?.league?.name ?: ""
    val teamName   = stats?.team?.name ?: ""

    val (rankColor, rankBg) = when (rank) {
        1    -> Color(0xFFFFD700) to Color(0xFFFFD700).copy(alpha = 0.15f) // Gold
        2    -> Color(0xFFC0C0C0) to Color(0xFFC0C0C0).copy(alpha = 0.12f) // Silver
        3    -> Color(0xFFCD7F32) to Color(0xFFCD7F32).copy(alpha = 0.12f) // Bronze
        else -> Color.White.copy(alpha = 0.45f) to Color.White.copy(alpha = 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (rank <= 3)
                    Brush.horizontalGradient(listOf(rankBg, Color.Transparent))
                else
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.Transparent))
            )
            .border(
                width = if (rank <= 3) 1.dp else 0.5.dp,
                color = if (rank <= 3) rankColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onPlayerClick(player.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(rankBg),
            contentAlignment = Alignment.Center
        ) {
            if (rank == 1) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = rankColor, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    text = "#$rank",
                    fontSize = if (rank < 10) 12.sp else 10.sp,
                    fontWeight = FontWeight.Black,
                    color = rankColor
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Player photo
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.5.dp, if (rank <= 3) rankColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = player.photo,
                contentDescription = player.name,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name ?: "Player",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$teamName  ·  $leagueName",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // Stats column
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚽", fontSize = 11.sp)
                Text(
                    text = "$goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (rank <= 3) rankColor else GlassGlowGreen
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "A:$assists", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IceBlue)
                Text(text = "⭐$rating", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SignalAmber)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHIMMER BOX
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 8.dp) {
    val alpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        0.05f, 0.14f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "sa"
    )
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(Color.White.copy(alpha = alpha)))
}

// ─────────────────────────────────────────────────────────────────────────────
// FAN ZONE POLL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HomeFanZonePoll() {
    var selectedOption by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, GlassGlowGreen.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GlassGlowGreen))
                Spacer(Modifier.width(8.dp))
                Text("DAILY POLL", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = GlassGlowGreen, letterSpacing = 1.2.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Who wins the Champions League this season?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(16.dp))

            val options = listOf("Real Madrid" to 38, "Man City" to 27, "Arsenal" to 20, "Bayern Munich" to 15)
            options.forEachIndexed { index, (optionText, percentage) ->
                val isSelected = selectedOption == index
                val hasVoted  = selectedOption != null

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) GlassGlowGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                        .border(1.dp, if (isSelected) GlassGlowGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                        .clickable { if (selectedOption == null) selectedOption = index }
                ) {
                    if (hasVoted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage / 100f)
                                .height(44.dp)
                                .background(if (isSelected) GlassGlowGreen.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = GlassGlowGreen, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = optionText,
                                color = if (isSelected) GlassGlowGreen else Color.White.copy(alpha = 0.85f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                        if (hasVoted) {
                            Text("$percentage%", color = if (isSelected) GlassGlowGreen else Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
