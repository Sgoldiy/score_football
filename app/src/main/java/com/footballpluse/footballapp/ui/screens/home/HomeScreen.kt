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
import com.footballpluse.footballapp.ui.components.HeaderIcon
import com.footballpluse.footballapp.ui.components.LivePulse
import com.footballpluse.footballapp.viewmodel.HomeUiState
import com.footballpluse.footballapp.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

private fun leagueBadgeUrl(leagueId: Int, leagueName: String): String =
    "https://apiv3.apifootball.com/badges/logo_leagues/${leagueId}_${leagueName.lowercase().replace(' ', '-')}.png"

private fun getLeagueCardColor(leagueId: Int): Color {
    return when (leagueId) {
        28 -> Color(0xFF1A462B)
        3 -> Color(0xFF0F204C)
        4 -> Color(0xFF4C2A0F)
        1 -> Color(0xFF0B2E5E)
        848 -> Color(0xFF143F24)
        152 -> Color(0xFF2C0F3A)
        302 -> Color(0xFF4C1014)
        207 -> Color(0xFF10335C)
        175 -> Color(0xFF4C0E11)
        168 -> Color(0xFF403C10)
        88 -> Color(0xFF5E2A0B)
        94 -> Color(0xFF143B1A)
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
        is HomeUiState.Error   -> HomeErrorScreen(state.message) { viewModel.loadHomeData() }
        is HomeUiState.Success -> {
            val formMap by viewModel.formMap.collectAsStateWithLifecycle()
            HomeContent(
                state                  = state,
                formMap                = formMap,
                favouriteLeagueId      = state.favouriteLeagueId,
                favouriteLeagueName    = state.favouriteLeagueName,
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
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(Color(0xFF131620)))
}

@Composable
private fun HomeLoadingShimmer() {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F14)), contentPadding = PaddingValues(bottom = 56.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(Modifier.width(140.dp).height(24.dp))
                    ShimmerBox(Modifier.width(80.dp).height(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { ShimmerBox(Modifier.size(40.dp), 20.dp) } }
            }
        }
        item { Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { ShimmerBox(Modifier.width(120.dp).height(16.dp)) } }
        item { Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { ShimmerBox(Modifier.width(120.dp).height(76.dp), 14.dp) } } }
    }
}

@Composable
private fun HomeErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F14)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚽", fontSize = 48.sp)
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
            Text("⚽", fontSize = 36.sp)
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
        allToday.groupBy { it.league.id }.map { (_, matches) -> matches }.sortedWith(compareByDescending<List<Match>> { group -> group.first().league.id == favouriteLeagueId }.thenByDescending { group -> group.any { it.isLive } })
    }
    
    val domesticLeagues = remember(state.topLeagues, favouriteLeagueId) {
        val domesticIds = setOf(152, 302, 207, 175, 168, 88, 94)
        state.topLeagues.filter { it.id in domesticIds || it.id == favouriteLeagueId }
            .sortedByDescending { it.id == favouriteLeagueId }
            .take(10)
    }

    val internationalLeagues = remember(state.topLeagues) {
        val intIds = setOf(3, 4, 28, 1, 848)
        state.topLeagues.filter { it.id in intIds }
    }

    val listState = rememberLazyListState()

    val leagueGroupMap = remember(groupedMatches) {
        groupedMatches.associateBy { "lg_${it.first().league.id}" }
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

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F14)), contentPadding = PaddingValues(bottom = 56.dp)) {
        item {
            AppHeader(favouriteLeagueName = favouriteLeagueName, onSearchClick = onSearch, onFavsClick = onFavourites, onNotifsClick = onNotifications)
            Spacer(Modifier.height(16.dp))
        }

        item {
            val liveLeagueIds = state.liveMatches.map { it.league.id }.toSet()
            
            if (domesticLeagues.isNotEmpty()) {
                LeagueSectionHeader("Top Leagues", "Major")
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
                            hasLiveMatches = league.id in liveLeagueIds,
                            onClick = { onNavigateToLeagueDetail?.invoke(league.id, league.season ?: 2025) ?: onNavigateToLeagues() }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (internationalLeagues.isNotEmpty()) {
                LeagueSectionHeader("International & Cups", null)
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
                            hasLiveMatches = league.id in liveLeagueIds,
                            onClick = { onNavigateToLeagueDetail?.invoke(league.id, league.season ?: 2025) ?: onNavigateToLeagues() }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (state.liveMatches.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Live Now", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        LivePulse()
                    }
                    Text("See all", color = Color(0xFF00E676), fontSize = 12.sp, modifier = Modifier.clickable { onNavigateToFixtures() })
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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

        item {
            MyTeamsStrip(onEditFavorites = onFavourites, onClubClick = onClubClick)
            Text("Match Schedule", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(10.dp))
        }

        if (groupedMatches.isEmpty()) {
            item { EmptyMatchesState(onRetry = onRetry) }
        } else {
            groupedMatches.forEach { group ->
                val league = group.first().league
                item(key = "lg_${league.id}") {
                    var isExpanded by remember { mutableStateOf(true) }
                    val isMyLeague = league.id == favouriteLeagueId
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)), border = BorderStroke(0.5.dp, if (isMyLeague) Color(0xFF00E676).copy(alpha = 0.4f) else Color(0xFF1A1E2A))) {
                        Column {
                            LeagueGroupHeader(league = league, count = group.size, liveCount = group.count { it.isLive }, hasLive = group.any { it.isLive }, isMyLeague = isMyLeague, isExpanded = isExpanded, onToggleExpand = { isExpanded = !isExpanded })
                            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                                Column {
                                    group.forEachIndexed { idx, match ->
                                        MatchRow(match = match, homeForm = formMap[match.homeTeam.id] ?: "", awayForm = formMap[match.awayTeam.id] ?: "", onMatchClick = onMatchClick)
                                        if (idx < group.lastIndex) HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
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
private fun LeagueSectionHeader(title: String, badge: String?) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF00E676).copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(badge, color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AppHeader(favouriteLeagueName: String, onSearchClick: () -> Unit, onFavsClick: () -> Unit, onNotifsClick: () -> Unit) {
    val today = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Row {
                Text("Football ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Plus", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
            }
            Text(today, fontSize = 11.sp, color = Color(0xFF555555))
            Text(favouriteLeagueName, fontSize = 11.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Medium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderIcon(Icons.Default.Search, onClick = onSearchClick)
            HeaderIcon(Icons.Default.FavoriteBorder, onClick = onFavsClick)
            HeaderIcon(Icons.Default.Notifications, onClick = onNotifsClick)
        }
    }
}

@Composable
private fun CompetitionCard(leagueId: Int, leagueName: String, logoUrl: String?, season: Int, hasLiveMatches: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.width(130.dp).height(88.dp).clickable(onClick = onClick, interactionSource = remember { MutableInteractionSource() }, indication = null), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = getLeagueCardColor(leagueId)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White).padding(4.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(model = logoUrl, contentDescription = leagueName, modifier = Modifier.size(20.dp), placeholder = painterResource(R.drawable.ic_placeholder), error = painterResource(R.drawable.ic_placeholder))
                }
                if (hasLiveMatches) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF4444)).padding(horizontal = 5.dp, vertical = 2.dp)) {
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
private fun LeagueGroupHeader(league: LeagueInfo, count: Int, liveCount: Int, hasLive: Boolean, isMyLeague: Boolean, isExpanded: Boolean, onToggleExpand: () -> Unit) {
    val leagueLogo = league.logo ?: leagueBadgeUrl(league.id, league.name)
    Row(
        modifier = Modifier.fillMaxWidth().background(if (isMyLeague) Color(0xFF0D1A0D) else Color(0xFF161A26)).drawBehind {
            drawRect(color = Color(0xFF00E676), size = Size(if (isMyLeague) 4.dp.toPx() else 3.dp.toPx(), size.height))
        }.clickable { onToggleExpand() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(model = leagueLogo, contentDescription = null, modifier = Modifier.size(18.dp), placeholder = painterResource(R.drawable.ic_placeholder), error = painterResource(R.drawable.ic_placeholder))
        Spacer(Modifier.width(8.dp))
        Text(league.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        val badgeColor = if (hasLive) Color(0xFFFF4444) else Color(0xFF555555)
        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (hasLive) Color(0xFF1A0A0A) else Color(0xFF1E2230)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(if (hasLive) "$liveCount Live" else "$count matches", color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MatchRow(match: Match, homeForm: String, awayForm: String, onMatchClick: (String) -> Unit) {
    val isFinished = match.status.short in listOf("FT", "AET", "PEN")
    Row(modifier = Modifier.fillMaxWidth().clickable { onMatchClick(match.id.toString()) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(42.dp)) {
            if (match.isLive) {
                Text("${match.elapsed ?: 0}'", fontSize = 11.sp, color = Color(0xFFFF4444), fontWeight = FontWeight.Medium)
            } else {
                Text(try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(match.timestamp * 1000L)) } catch (e: Exception) { "--:--" }, fontSize = 11.sp, color = Color(0xFF555555))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            TeamRowItem(match.homeTeam.name, match.homeTeam.logo, match.homeScore, homeForm, match.isLive || isFinished)
            Spacer(Modifier.height(8.dp))
            TeamRowItem(match.awayTeam.name, match.awayTeam.logo, match.awayScore, awayForm, match.isLive || isFinished)
        }
    }
}

@Composable
private fun TeamRowItem(name: String, logo: String?, score: Int?, form: String, showScore: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = logo, contentDescription = null, modifier = Modifier.size(22.dp).clip(CircleShape), placeholder = painterResource(R.drawable.ic_team_placeholder))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (form.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                com.footballpluse.footballapp.ui.components.FormDotsRow(form = form, dotSize = 7.dp, gap = 4.dp, modifier = Modifier)
            }
        }
        if (showScore) {
            Text(score?.toString() ?: "0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun MyTeamsStrip(onEditFavorites: () -> Unit, onClubClick: (Int) -> Unit, viewModel: com.footballpluse.footballapp.viewmodel.FavoritesViewModel = hiltViewModel()) {
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    if (favourites.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Teams", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Edit", color = Color(0xFF00E676), fontSize = 13.sp, modifier = Modifier.clickable { onEditFavorites() })
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favourites) { fav ->
                    Card(modifier = Modifier.height(56.dp).clickable { onClubClick(fav.teamId) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = fav.badgeUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(fav.teamName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

