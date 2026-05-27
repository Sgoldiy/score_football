package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.domain.model.StandingItem
import com.footballpluse.footballapp.ui.components.MatchRow
import com.footballpluse.footballapp.ui.components.MatchRowShimmer
import com.footballpluse.footballapp.ui.components.SectionHeader
import com.footballpluse.footballapp.ui.theme.*
import com.footballpluse.footballapp.viewmodel.LeagueDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueDetailScreen(
    leagueId: Int,
    season: Int = 2025,
    onBackClick: () -> Unit,
    onMatchClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit,
    viewModel: LeagueDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(leagueId, season) {
        viewModel.load(leagueId, season)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Standings", "Fixtures", "Player Stats")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.leagueInfo?.name ?: "League Detail",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PitchBlack
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DeepNavy)
        ) {
            if (state.leagueInfo != null) {
                LeagueHeader(state.leagueInfo!!)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PitchBlack,
                contentColor = LiveGreen,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = LiveGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> StandingsTab(state.standings, onTeamClick)
                1 -> FixturesTab(state.fixtures, onMatchClick)
                2 -> PlayerStatsTab(state.topScorers, state.topAssists)
            }
        }
    }
}

@Composable
fun LeagueHeader(leagueInfo: com.footballpluse.footballapp.domain.model.LeagueInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = leagueInfo.logo,
                contentDescription = leagueInfo.name,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = leagueInfo.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        if (leagueInfo.country != null) {
            Text(
                text = leagueInfo.country,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
}

@Composable
fun StandingsTab(standingsResult: ApiResult<List<StandingItem>>, onTeamClick: (Int) -> Unit) {
    when (standingsResult) {
        is ApiResult.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { MatchRowShimmer() }
            }
        }
        is ApiResult.Success -> {
            val standings = standingsResult.data
            if (standings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No standings available", color = Color.White.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        StandingsHeader()
                    }
                    items(standings) { item -> StandingRow(item, onTeamClick) }
                }
            }
        }
        is ApiResult.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    standingsResult.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun StandingsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f))
        Text("Team", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f))
        Text("P", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.4f))
        Text("W", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.4f))
        Text("D", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.4f))
        Text("L", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.4f))
        Text("GD", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.4f))
        Text("Pts", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.4f))
    }
}

@Composable
fun StandingRow(item: StandingItem, onTeamClick: (Int) -> Unit) {
    val zoneColor = when {
        item.rank <= 4 -> LiveGreen // Champions League
        item.rank == 5 -> Color(0xFFFF9800) // Europa League
        item.rank >= 18 -> Color(0xFFF44336) // Relegation
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onTeamClick(item.team.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Qualification stripe indicator
            if (zoneColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(38.dp)
                        .background(zoneColor)
                )
            } else {
                Spacer(Modifier.width(4.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.rank.toString(),
                    modifier = Modifier.width(28.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.rank <= 4) LiveGreen else Color.White
                )
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = item.team.logo,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.team.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                Text(item.played.toString(), modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                Text(item.win.toString(), modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                Text(item.draw.toString(), modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                Text(item.lose.toString(), modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                Text(
                    text = if (item.goalsDiff >= 0) "+${item.goalsDiff}" else item.goalsDiff.toString(),
                    modifier = Modifier.width(32.dp),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (item.goalsDiff >= 0) LiveGreen else DangerRed
                )
                Text(
                    item.points.toString(),
                    modifier = Modifier.width(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FixturesTab(
    fixturesResult: ApiResult<List<Match>>,
    onMatchClick: (Int) -> Unit
) {
    when (fixturesResult) {
        is ApiResult.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { MatchRowShimmer() }
            }
        }
        is ApiResult.Success -> {
            val matches = fixturesResult.data
            if (matches.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No fixtures available", color = Color.White.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matches) { match ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            MatchRow(match = match, onClick = { idStr -> onMatchClick(idStr.toInt()) })
                        }
                    }
                }
            }
        }
        is ApiResult.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    fixturesResult.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// ─── PLAYER STATS LEAGUE TAB ────────────────────────────────────────────────

@Composable
fun PlayerStatsTab(
    scorersState: ApiResult<List<PlayerProfileStatisticsResponse>>,
    assistsState: ApiResult<List<PlayerProfileStatisticsResponse>>
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = { selectedSubTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedSubTab == 0) LiveGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Text("Top Goals", color = if (selectedSubTab == 0) LiveGreen else Color.White, fontWeight = FontWeight.Bold)
            }
            
            TextButton(
                onClick = { selectedSubTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedSubTab == 1) LiveGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Text("Top Assists", color = if (selectedSubTab == 1) LiveGreen else Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        val activeResult = if (selectedSubTab == 0) scorersState else assistsState
        when (activeResult) {
            is ApiResult.Loading -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LiveGreen)
                }
            }
            is ApiResult.Success -> {
                val players = activeResult.data
                if (players.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No statistical records found", color = Color.White.copy(alpha = 0.4f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(players) { index, entry ->
                            val player = entry.player
                            val stats = entry.statistics?.firstOrNull()
                            val value = if (selectedSubTab == 0) {
                                stats?.goals?.total?.toString() ?: "0"
                            } else {
                                stats?.goals?.assists?.toString() ?: "0"
                            }
                            if (player != null) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.Black,
                                            color = LiveGreen,
                                            fontSize = 14.sp,
                                            modifier = Modifier.width(32.dp)
                                        )
                                        AsyncImage(
                                            model = player.photo,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(player.name ?: "Player", fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(stats?.team?.name ?: "", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                        }
                                        Text(
                                            text = value,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is ApiResult.Error -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(activeResult.message, color = DangerRed)
                }
            }
        }
    }
}

