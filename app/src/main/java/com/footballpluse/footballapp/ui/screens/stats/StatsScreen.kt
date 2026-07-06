package com.footballpluse.footballapp.ui.screens.stats

import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Scoreboard
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.data.model.FixtureResponse
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.model.StandingRecord
import com.footballpluse.footballapp.ui.components.HeaderIcon
import com.footballpluse.footballapp.ui.components.MatchRowShimmer
import com.footballpluse.footballapp.viewmodel.ClubAttackDefence
import com.footballpluse.footballapp.viewmodel.ClubBigChanceConversion
import com.footballpluse.footballapp.viewmodel.ClubCleanSheet
import com.footballpluse.footballapp.viewmodel.ClubXgPerformance
import com.footballpluse.footballapp.viewmodel.ClubsStatsUiState
import com.footballpluse.footballapp.viewmodel.DisciplineUiState
import com.footballpluse.footballapp.viewmodel.FirstGoalAdvantageData
import com.footballpluse.footballapp.viewmodel.GoalTimingUiState
import com.footballpluse.footballapp.viewmodel.PlayerCardsStat
import com.footballpluse.footballapp.viewmodel.PlayerFoulsStat
import com.footballpluse.footballapp.viewmodel.PlayerShotAccuracy
import com.footballpluse.footballapp.viewmodel.PlayerXgPerformance
import com.footballpluse.footballapp.viewmodel.PlayersStatsUiState
import com.footballpluse.footballapp.viewmodel.StatsTab
import com.footballpluse.footballapp.viewmodel.StatsViewModel
import com.footballpluse.footballapp.viewmodel.TeamCardsStat
import com.footballpluse.footballapp.viewmodel.TeamGoalTiming
import com.footballpluse.footballapp.viewmodel.XGStatsUiState
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateToPlayerProfile: (Int) -> Unit = {},
    onNavigateToClubInfo: (Int, Int) -> Unit = { _, _ -> },
    viewModel: StatsViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedLeague by viewModel.selectedLeague.collectAsState()
    val availableLeagues = viewModel.availableLeagues

    val playersState by viewModel.playersState.collectAsState()
    val clubsState by viewModel.clubsState.collectAsState()
    val xgState by viewModel.xgState.collectAsState()
    val timingState by viewModel.timingState.collectAsState()
    val disciplineState by viewModel.disciplineState.collectAsState()

    var showLeagueSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
    ) {
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
                        text = "Stats & ",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Analytics",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
                Text(
                    text = "Deep football data",
                    fontSize = 11.sp,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderIcon(icon = Icons.Default.Search)
                HeaderIcon(icon = Icons.Default.Notifications)
            }
        }

        // 2. Scope tab row (horizontal scrollable)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            items(StatsTab.values()) { tab ->
                val isSelected = selectedTab == tab
                val label = when (tab) {
                    StatsTab.PLAYERS -> "Players"
                    StatsTab.CLUBS -> "Clubs"
                    StatsTab.XG_ADVANCED -> "xG & Advanced"
                    StatsTab.GOAL_TIMING -> "Goal Timing"
                    StatsTab.DISCIPLINE -> "Discipline"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF00E676) else Color(0xFF131620))
                        .clickable { viewModel.onTabSelected(tab) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF0D0F14) else Color(0xFF888888),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        // 3. League Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .background(Color(0xFF131620), RoundedCornerShape(12.dp))
                .border(0.5.dp, Color(0xFF1A1E2A), RoundedCornerShape(12.dp))
                .clickable { showLeagueSheet = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = selectedLeague.logoUrl,
                contentDescription = selectedLeague.name,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_placeholder)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedLeague.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedLeague.season}/${(selectedLeague.season + 1) % 100} season",
                    color = Color(0xFF555555),
                    fontSize = 10.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(16.dp)
            )
        }

        // 4. Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                StatsTab.PLAYERS -> PlayersTabContent(
                    playersState,
                    onNavigateToPlayerProfile,
                    viewModel
                )

                StatsTab.CLUBS -> ClubsTabContent(clubsState, onNavigateToClubInfo, viewModel)
                StatsTab.XG_ADVANCED -> XGAdvancedTabContent(xgState, viewModel)
                StatsTab.GOAL_TIMING -> GoalTimingTabContent(timingState, viewModel)
                StatsTab.DISCIPLINE -> DisciplineTabContent(disciplineState, viewModel)
            }
        }
    }

    // ModalBottomSheet for League Selection
    if (showLeagueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLeagueSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF131620),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFF2A2D35), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Competition",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableLeagues) { league ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedLeague.id == league.id) Color.White.copy(
                                        alpha = 0.05f
                                    ) else Color.Transparent
                                )
                                .clickable {
                                    viewModel.onLeagueSelected(league)
                                    showLeagueSheet = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = league.logoUrl,
                                contentDescription = league.name,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(3.dp),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_placeholder)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = league.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${league.season}/${(league.season + 1) % 100}",
                                    color = Color(0xFF555555),
                                    fontSize = 10.sp
                                )
                            }
                            if (selectedLeague.id == league.id) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 1 — PLAYERS
// ==========================================
@Composable
fun PlayersTabContent(
    state: PlayersStatsUiState,
    onPlayerClick: (Int) -> Unit,
    viewModel: StatsViewModel
) {
    when (state) {
        is PlayersStatsUiState.Idle, is PlayersStatsUiState.Loading -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(3) { MatchRowShimmer() }
            }
        }

        is PlayersStatsUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Section A — Top Scorer Hero Card
                Text(
                    text = "Top Scorer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TopScorerHeroCard(state.topScorer, onPlayerClick)

                Spacer(Modifier.height(20.dp))

                // Section B — Golden Boot Race Chart & Podium
                Text(
                    text = "Golden Boot Race",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                GoldenBootRaceChart(state.top8Scorers, onPlayerClick)

                Spacer(Modifier.height(20.dp))

                // Section C — Season Highlights Grid
                Text(
                    text = "Season Highlights",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SeasonHighlightsGrid(
                    state.totalGoals,
                    state.avgGoals,
                    state.penaltyGoalsPct,
                    state.avgXgPerMatch
                )

                Spacer(Modifier.height(20.dp))

                // Section D — Top Assists & Podium
                Text(
                    text = "Top Assists",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TopAssistsChart(state.top8Assists, onPlayerClick)

                Spacer(Modifier.height(20.dp))

                // Section E — Player Ratings Leaderboard
                Text(
                    text = "Player Ratings Leaderboard",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PlayerRatingsLeaderboard(state.ratingsLeaderboard, onPlayerClick)

                Spacer(Modifier.height(30.dp))
            }
        }

        is PlayersStatsUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TopScorerHeroCard(
    playerStats: PlayerProfileStatisticsResponse,
    onPlayerClick: (Int) -> Unit
) {
    val player = playerStats.player ?: return
    val stats = playerStats.statistics?.firstOrNull()
    val team = stats?.team

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayerClick(player.id) },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E2A4A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2E))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = player.photo,
                            contentDescription = player.name,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_placeholder)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    // Player Meta
                    Column {
                        Text(
                            text = player.name ?: "Unknown Player",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            AsyncImage(
                                model = team?.logo,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_placeholder)
                            )
                            Text(
                                text = team?.name ?: "",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "${stats?.games?.position ?: ""} · ${player.nationality ?: ""}",
                            color = Color(0xFF555555),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val goals = stats?.goals?.total ?: 0
                    val assists = stats?.goals?.assists ?: 0
                    val rating = stats?.games?.rating ?: "0.0"
                    val xg = 0.94f //Kane simulated goals/xg ratio or similar

                    MiniStatBox(
                        label = "Goals",
                        value = "$goals",
                        color = Color(0xFF00E676),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        label = "Assists",
                        value = "$assists",
                        color = Color(0xFF5B8DE8),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        label = "Rating",
                        value = rating.take(4),
                        color = Color(0xFFF0A500),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        label = "xG/90",
                        value = "$xg",
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Top Right Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                    .background(Color(0xFF00E676))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "#1 SCORER",
                    color = Color(0xFF0D0F14),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun MiniStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0F14).copy(alpha = 0.5f))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFF555555),
            fontSize = 9.sp
        )
    }
}

@Composable
fun GoldenBootRaceChart(
    scorers: List<PlayerProfileStatisticsResponse>,
    onPlayerClick: (Int) -> Unit = {}
) {
    PlayerStatLeaderboard(
        players = scorers,
        accentColor = Color(0xFF00E676),
        statType = "Goals",
        onPlayerClick = onPlayerClick
    )
}

@Composable
fun PlayerStatLeaderboard(
    players: List<PlayerProfileStatisticsResponse>,
    accentColor: Color,
    statType: String,
    onPlayerClick: (Int) -> Unit = {}
) {
    val displayPlayers = players.take(8)
    val maxVal = displayPlayers.firstOrNull()?.statistics?.firstOrNull()?.let { s ->
        if (statType == "Goals") s.goals?.total ?: 0 else s.goals?.assists ?: 0
    } ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            displayPlayers.forEachIndexed { index, playerStats ->
                val player = playerStats.player ?: return@forEachIndexed
                val stats = playerStats.statistics?.firstOrNull()
                val value =
                    if (statType == "Goals") stats?.goals?.total ?: 0 else stats?.goals?.assists
                        ?: 0
                val ratio = if (maxVal > 0) value.toFloat() / maxVal.coerceAtLeast(1) else 0f

                val rankColor = when (index) {
                    0 -> Color(0xFFFFD700)
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> Color(0xFF444455)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(player.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank badge
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(rankColor.copy(alpha = if (index < 3) 0.15f else 0.05f))
                            .border(
                                0.5.dp,
                                rankColor.copy(alpha = if (index < 3) 0.6f else 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (index < 3) rankColor else Color(0xFF555555),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Player photo
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433))
                            .border(
                                1.5.dp,
                                accentColor.copy(alpha = if (index == 0) 0.8f else 0.25f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = player.photo,
                            contentDescription = player.name,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_placeholder)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Name + team
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name?.split(" ")?.lastOrNull() ?: player.name ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            AsyncImage(
                                model = stats?.team?.logo,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = stats?.team?.name ?: "",
                                color = Color(0xFF555555),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Bar + count column
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.width(80.dp)
                    ) {
                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1E2A))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.5f),
                                                accentColor
                                            )
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$value",
                            color = if (index == 0) accentColor else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (index < displayPlayers.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = Color(0xFF1A1E2A),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}


@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, color = Color(0xFF888888), fontSize = 10.sp)
    }
}

@Composable
fun SeasonHighlightsGrid(
    totalGoals: Int,
    avgGoals: Float,
    penaltyGoalsPct: Float,
    avgXgPerMatch: Float
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            HighlightStatCard(
                icon = Icons.Rounded.EmojiEvents,
                value = "$totalGoals",
                label = "Total goals scored",
                trend = "+12% vs last season",
                trendPositive = true,
                modifier = Modifier.weight(1f)
            )
            HighlightStatCard(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                value = String.format("%.2f", avgGoals),
                label = "Avg goals per game",
                trend = "+0.14 vs last season",
                trendPositive = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            HighlightStatCard(
                icon = Icons.Rounded.Scoreboard,
                value = String.format("%.0f%%", penaltyGoalsPct),
                label = "Goals from penalties %",
                trend = "-3% vs last season",
                trendPositive = false,
                modifier = Modifier.weight(1f)
            )
            HighlightStatCard(
                icon = Icons.Rounded.QueryStats,
                value = String.format("%.2f", avgXgPerMatch),
                label = "Avg xG per match",
                trend = "+0.06 vs last season",
                trendPositive = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HighlightStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    trend: String,
    trendPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = Color(0xFF555555),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = trend,
                color = if (trendPositive) Color(0xFF00E676) else Color(0xFFFF4444),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TopAssistsChart(
    assists: List<PlayerProfileStatisticsResponse>,
    onPlayerClick: (Int) -> Unit = {}
) {
    PlayerStatLeaderboard(
        players = assists,
        accentColor = Color(0xFF5B8DE8),
        statType = "Assists",
        onPlayerClick = onPlayerClick
    )
}

@Composable
fun PlayerRatingsLeaderboard(
    players: List<PlayerProfileStatisticsResponse>,
    onPlayerClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            players.forEachIndexed { index, playerStats ->
                val player = playerStats.player ?: return@forEachIndexed
                val stats = playerStats.statistics?.firstOrNull()
                val rating = stats?.games?.rating ?: "0.0"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(player.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank
                    Text(
                        text = "${index + 1}",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )

                    // Photo
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = player.photo,
                            contentDescription = player.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Player Meta
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name ?: "Unknown Player",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AsyncImage(
                                model = stats?.team?.logo,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = stats?.team?.name ?: "",
                                color = Color(0xFF555555),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Rating Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF0A500))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = rating.take(4),
                            color = Color(0xFF0D0F14),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (index < players.size - 1) {
                    HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ==========================================
// TAB 2 — CLUBS
// ==========================================
@Composable
fun ClubsTabContent(
    state: ClubsStatsUiState,
    onClubClick: (Int, Int) -> Unit,
    viewModel: StatsViewModel
) {
    when (state) {
        is ClubsStatsUiState.Idle, is ClubsStatsUiState.Loading -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(3) { MatchRowShimmer() }
            }
        }

        is ClubsStatsUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Section A — Top Scoring Teams
                Text(
                    text = "Top Goal-Scoring Teams",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TopScoringTeamsList(state.standings)

                Spacer(Modifier.height(20.dp))

                // Section B — Best Defensive Teams
                Text(
                    text = "Best Defensive Teams",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BestDefenseTeamsList(state.standings)

                Spacer(Modifier.height(20.dp))

                // Section C — Win Rate Leaders
                Text(
                    text = "Win Rate Leaders",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                WinRateLeadersList(state.standings)

                Spacer(Modifier.height(20.dp))

                // Section D — Clean Sheet Leaders
                Text(
                    text = "Clean Sheet Leaders",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CleanSheetsBarChart(state.cleanSheetLeaders)

                Spacer(Modifier.height(20.dp))

                // Section E — Attack vs Defence Scatter
                Text(
                    text = "Attack vs Defence",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AttackDefenceScatterChart(state.attackDefenceList)

                Spacer(Modifier.height(20.dp))

                // Section F — Biggest Wins
                Text(
                    text = "Biggest Wins This Season",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BiggestWinsList(state.biggestWins)

                Spacer(Modifier.height(30.dp))
            }
        }

        is ClubsStatsUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}

// ==========================================
// CLUB STAT BAR ROW — reusable
// ==========================================
@Composable
fun ClubStatBarRow(
    rank: Int,
    logo: String?,
    name: String,
    value: Int,
    maxValue: Int,
    accentColor: Color,
    trailingLabel: String = "$value",
    modifier: Modifier = Modifier
) {
    val ratio = if (maxValue > 0) value.toFloat() / maxValue.coerceAtLeast(1) else 0f
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF444455)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = if (rank <= 3) 0.15f else 0.05f))
                    .border(
                        0.5.dp,
                        rankColor.copy(alpha = if (rank <= 3) 0.6f else 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = if (rank <= 3) rankColor else Color(0xFF555555),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(10.dp))

            // Team Logo
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2433)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = logo,
                    contentDescription = name,
                    modifier = Modifier.size(24.dp),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_placeholder)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Name
            Text(
                text = name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(10.dp))

            // Value badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trailingLabel,
                    color = if (rank == 1) accentColor else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(7.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .padding(start = 64.dp) // align under name, after rank+logo
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1E2A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.4f), accentColor)
                        )
                    )
            )
        }
    }
}

@Composable
fun TopScoringTeamsList(standings: List<StandingRecord>) {
    val sorted = standings
        .sortedByDescending { it.all?.goals?.goalsFor ?: 0 }
        .take(6)
    val maxGoals = sorted.firstOrNull()?.all?.goals?.goalsFor ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            sorted.forEachIndexed { index, record ->
                ClubStatBarRow(
                    rank = index + 1,
                    logo = record.team?.logo,
                    name = record.team?.name ?: "Unknown",
                    value = record.all?.goals?.goalsFor ?: 0,
                    maxValue = maxGoals,
                    accentColor = Color(0xFF00E676),
                    trailingLabel = "${record.all?.goals?.goalsFor ?: 0} GF"
                )
                if (index < sorted.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = Color(0xFF1A1E2A),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun BestDefenseTeamsList(standings: List<StandingRecord>) {
    val sorted = standings
        .filter { (it.all?.played ?: 0) > 0 }
        .sortedBy { it.all?.goals?.against ?: Int.MAX_VALUE }
        .take(6)
    val maxGA = sorted.lastOrNull()?.all?.goals?.against ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            sorted.forEachIndexed { index, record ->
                val ga = record.all?.goals?.against ?: 0
                ClubStatBarRow(
                    rank = index + 1,
                    logo = record.team?.logo,
                    name = record.team?.name ?: "Unknown",
                    value = ga,
                    maxValue = (maxGA + 1).coerceAtLeast(1),
                    accentColor = Color(0xFF5B8DE8),
                    trailingLabel = "$ga GA"
                )
                if (index < sorted.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = Color(0xFF1A1E2A),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun WinRateLeadersList(standings: List<StandingRecord>) {
    data class WinRateEntry(
        val record: StandingRecord,
        val winRate: Float,
        val wins: Int,
        val played: Int
    )

    val sorted = standings
        .filter { (it.all?.played ?: 0) >= 5 }
        .map { r ->
            val played = r.all?.played ?: 1
            val wins = r.all?.win ?: 0
            WinRateEntry(r, wins.toFloat() / played * 100f, wins, played)
        }
        .sortedByDescending { it.winRate }
        .take(6)


    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            sorted.forEachIndexed { index, entry ->
                ClubStatBarRow(
                    rank = index + 1,
                    logo = entry.record.team?.logo,
                    name = entry.record.team?.name ?: "Unknown",
                    value = entry.wins,
                    maxValue = (entry.played).coerceAtLeast(1),
                    accentColor = Color(0xFFF0A500),
                    trailingLabel = String.format("%.0f%%", entry.winRate)
                )
                if (index < sorted.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = Color(0xFF1A1E2A),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun StandingsTable(
    standings: List<StandingRecord>,
    onClubClick: (Int, Int) -> Unit,
    viewModel: StatsViewModel
) {
    val selectedLeague = viewModel.selectedLeague.collectAsState().value
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pos",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    "Team Name",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "P",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Form",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(110.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Pts",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)

            val displayStandings = standings

            displayStandings.forEachIndexed { index, record ->
                val team = record.team ?: return@forEachIndexed
                val rank = record.rank
                val played = record.all?.played ?: 0
                val points = record.points ?: 0
                val form = record.form ?: ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClubClick(team.id, selectedLeague.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position
                    val posColor = when {
                        rank <= 3 -> Color(0xFF00E676)
                        rank >= standings.size - 2 -> Color(0xFFFF4444)
                        else -> Color.White
                    }
                    Text(
                        text = "$rank",
                        color = posColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )

                    // Team Meta
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                            model = team.logo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_placeholder)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = team.name ?: "Unknown",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Played
                    Text(
                        text = "$played",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )

                    // Form Squares
                    Row(
                        modifier = Modifier.width(110.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display 5 squares (last result rightmost)
                        val last5 = form.takeLast(5).padEnd(5, ' ')
                        last5.forEach { char ->
                            FormSquare(char)
                        }
                    }

                    // Points
                    Text(
                        text = "$points",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun FormSquare(char: Char) {
    val bg = when (char) {
        'W' -> Color(0xFF00E676)
        'D' -> Color(0xFF444444)
        'L' -> Color(0xFF2A1010)
        else -> Color.Transparent
    }
    val text = when (char) {
        'W' -> Color(0xFF0D0F14)
        'D' -> Color.White
        'L' -> Color(0xFFFF4444)
        else -> Color.Transparent
    }
    val border = if (char == 'L') BorderStroke(0.5.dp, Color(0xFF3A1212)) else null

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .then(
                if (border != null) Modifier.border(
                    border,
                    RoundedCornerShape(3.dp)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (char != ' ') {
            Text(
                text = "$char",
                color = text,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AttackDefenceScatterChart(attackDefence: List<ClubAttackDefence>) {
    // Classify teams into 4 quadrants based on goals scored/conceded per game
    // Elite: GF > 1.5 AND GA < 1.2
    // Solid Defense: GF <= 1.5 AND GA < 1.2
    // Attacking Leaky: GF > 1.5 AND GA >= 1.2
    // Struggling: GF <= 1.5 AND GA >= 1.2
    val elite = attackDefence.filter { it.goalsScored > 1.5f && it.goalsConceded < 1.2f }
        .sortedByDescending { it.goalsScored - it.goalsConceded }.take(4)
    val solidDefense = attackDefence.filter { it.goalsScored <= 1.5f && it.goalsConceded < 1.2f }
        .sortedBy { it.goalsConceded }.take(4)
    val attackingLeaky = attackDefence.filter { it.goalsScored > 1.5f && it.goalsConceded >= 1.2f }
        .sortedByDescending { it.goalsScored }.take(4)
    val struggling = attackDefence.filter { it.goalsScored <= 1.5f && it.goalsConceded >= 1.2f }
        .sortedByDescending { it.goalsConceded }.take(4)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AttackDefenceQuadrant(
                title = "Elite",
                subtitle = "High GF · Low GA",
                teams = elite,
                bgColor = Color(0xFF00E676).copy(alpha = 0.08f),
                borderColor = Color(0xFF00E676).copy(alpha = 0.3f),
                titleColor = Color(0xFF00E676),
                emoji = "⚡",
                modifier = Modifier.weight(1f)
            )
            AttackDefenceQuadrant(
                title = "Solid Defense",
                subtitle = "Low GF · Low GA",
                teams = solidDefense,
                bgColor = Color(0xFF5B8DE8).copy(alpha = 0.08f),
                borderColor = Color(0xFF5B8DE8).copy(alpha = 0.3f),
                titleColor = Color(0xFF5B8DE8),
                emoji = "🛡️",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AttackDefenceQuadrant(
                title = "Leaky",
                subtitle = "High GF · High GA",
                teams = attackingLeaky,
                bgColor = Color(0xFFF0A500).copy(alpha = 0.08f),
                borderColor = Color(0xFFF0A500).copy(alpha = 0.3f),
                titleColor = Color(0xFFF0A500),
                emoji = "⚽",
                modifier = Modifier.weight(1f)
            )
            AttackDefenceQuadrant(
                title = "Struggling",
                subtitle = "Low GF · High GA",
                teams = struggling,
                bgColor = Color(0xFFFF4444).copy(alpha = 0.08f),
                borderColor = Color(0xFFFF4444).copy(alpha = 0.3f),
                titleColor = Color(0xFFFF4444),
                emoji = "📉",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AttackDefenceQuadrant(
    title: String,
    subtitle: String,
    teams: List<ClubAttackDefence>,
    bgColor: Color,
    borderColor: Color,
    titleColor: Color,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(emoji, fontSize = 14.sp)
                Column {
                    Text(
                        text = title,
                        color = titleColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = Color(0xFF555555),
                        fontSize = 8.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (teams.isEmpty()) {
                Text(
                    text = "No teams",
                    color = Color(0xFF444455),
                    fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    teams.forEach { team ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Dot indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(titleColor)
                            )
                            Text(
                                text = team.teamName.split(" ").lastOrNull() ?: team.teamName,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format("%.1f", team.goalsScored),
                                color = Color(0xFF00E676),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanSheetsBarChart(leaders: List<ClubCleanSheet>) {
    val maxCS = leaders.firstOrNull()?.cleanSheets ?: 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            if (leaders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available", color = Color(0xFF555555), fontSize = 11.sp)
                }
            } else {
                leaders.forEachIndexed { index, team ->
                    ClubStatBarRow(
                        rank = index + 1,
                        logo = team.teamLogo,
                        name = team.teamName,
                        value = team.cleanSheets,
                        maxValue = maxCS.coerceAtLeast(1),
                        accentColor = Color(0xFF00E676),
                        trailingLabel = "${team.cleanSheets} CS"
                    )
                    if (index < leaders.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = Color(0xFF1A1E2A),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BiggestWinsList(wins: List<FixtureResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (wins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No wins by 4+ goals yet.", color = Color(0xFF555555), fontSize = 11.sp)
                }
            } else {
                wins.forEachIndexed { index, win ->
                    val teamHome = win.teams?.home
                    val teamAway = win.teams?.away
                    val goalsHome = win.goals?.home ?: 0
                    val goalsAway = win.goals?.away ?: 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home Team (name + logo, right-aligned)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = teamHome?.name ?: "",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(5.dp))
                            AsyncImage(
                                model = teamHome?.logo,
                                contentDescription = teamHome?.name,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_placeholder)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Score Block
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0D0F14))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$goalsHome - $goalsAway",
                                color = Color(0xFF00E676),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Away Team (logo + name, left-aligned)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = teamAway?.logo,
                                contentDescription = teamAway?.name,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_placeholder)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = teamAway?.name ?: "",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (index < wins.size - 1) {
                        HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3 — xG & ADVANCED
// ==========================================
@Composable
fun XGAdvancedTabContent(
    state: XGStatsUiState,
    viewModel: StatsViewModel
) {
    when (state) {
        is XGStatsUiState.Idle, is XGStatsUiState.Loading -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(3) { MatchRowShimmer() }
            }
        }

        is XGStatsUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Section A — Clinical Finishers Leaderboard
                Text(
                    text = "Clinical Finishers Leaderboard (Goals vs xG)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ClinicalFinishersLeaderboard(state.playerXgPerformers)

                Spacer(Modifier.height(20.dp))

                // Section B — Club xG Table
                Text(
                    text = "Club xG Table (Goals vs xG)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ClubXgPerformanceTable(state.clubXgTable)

                Spacer(Modifier.height(20.dp))

                // Section C — Big Chance Conversion
                Text(
                    text = "Big Chance Conversion Rate",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BigChanceConversionList(state.bigChanceConversion)

                Spacer(Modifier.height(20.dp))

                // Section D — Shot Accuracy
                Text(
                    text = "Shot Accuracy Leaders (Goals / SoT %)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ShotAccuracyList(state.shotAccuracyLeaders)

                Spacer(Modifier.height(30.dp))
            }
        }

        is XGStatsUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}


@Composable
fun ClubXgPerformanceTable(clubs: List<ClubXgPerformance>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Team",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Goals",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "xG",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Diff",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)

            clubs.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                            model = item.teamLogo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.teamName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        "${item.goals}",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        String.format("%.1f", item.xg),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )

                    val diffText = if (item.diff >= 0) "+${
                        String.format(
                            "%.1f",
                            item.diff
                        )
                    }" else String.format("%.1f", item.diff)
                    val diffColor = if (item.diff >= 0) Color(0xFF00E676) else Color(0xFF5B8DE8)

                    Text(
                        diffText,
                        color = diffColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.End
                    )
                }
                if (index < clubs.size - 1) {
                    HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun BigChanceConversionList(conversion: List<ClubBigChanceConversion>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            conversion.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                            model = item.teamLogo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.teamName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "${item.converted}/${item.created} converted",
                        color = Color(0xFF555555),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .border(
                                0.5.dp,
                                Color(0xFF00E676).copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format("%.1f%%", item.pct),
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (index < conversion.size - 1) {
                    HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun ShotAccuracyList(leaders: List<PlayerShotAccuracy>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            leaders.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank number
                    Text(
                        text = "${index + 1}",
                        color = Color(0xFF555555),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp)
                    )

                    // Player photo
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433))
                            .border(1.5.dp, Color(0xFF5B8DE8).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = item.playerPhoto,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_placeholder)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Name + team
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AsyncImage(
                                model = item.teamLogo,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_placeholder)
                            )
                            Text(
                                text = "${item.goals}G / ${item.shotsOnTarget} SoT",
                                color = Color(0xFF555555),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Accuracy badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF5B8DE8).copy(alpha = 0.15f))
                            .border(
                                0.5.dp,
                                Color(0xFF5B8DE8).copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format("%.1f%%", item.ratio),
                            color = Color(0xFF5B8DE8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (index < leaders.size - 1) {
                    HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ==========================================
// TAB 4 — GOAL TIMING
// ==========================================
@Composable
fun GoalTimingTabContent(
    state: GoalTimingUiState,
    viewModel: StatsViewModel
) {
    when (state) {
        is GoalTimingUiState.Idle, is GoalTimingUiState.Loading -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(3) { MatchRowShimmer() }
            }
        }

        is GoalTimingUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Section A — Timing Heatmap
                Text(
                    text = "Goal Timing Heatmap",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                GoalTimingHeatmapChart(state.leagueTimingHeatmap, viewModel)

                Spacer(Modifier.height(20.dp))

                // Section B — Team timing overlay
                Text(
                    text = "Team Specific Goal Timing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TeamTimingOverlayBlock(state.allTeams, state.teamSpecificTiming)

                Spacer(Modifier.height(20.dp))

                // Section C — First Goal Advantage
                Text(
                    text = "First Goal Advantage Stat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FirstGoalAdvantageCard(state.firstGoalAdvantage, viewModel)

                Spacer(Modifier.height(30.dp))
            }
        }

        is GoalTimingUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun GoalTimingHeatmapChart(heatmap: List<Int>, viewModel: StatsViewModel) {
    val selectedLeague = viewModel.selectedLeague.collectAsState().value
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Goal timing heatmap — ${selectedLeague.name}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Darker green = more goals in that 15-min window",
                color = Color(0xFF555555),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    com.github.mikephil.charting.charts.BarChart(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        setDrawBorders(false)
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = false
                        setScaleEnabled(false)

                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setDrawGridLines(false)
                            setDrawAxisLine(false)
                            textColor = AndroidColor.parseColor("#555555")
                            textSize = 9f
                            granularity = 1f
                            isGranularityEnabled = true
                            valueFormatter = IndexAxisValueFormatter(
                                listOf(
                                    "0-15",
                                    "16-30",
                                    "31-45",
                                    "45+",
                                    "46-60",
                                    "61-75",
                                    "76-90",
                                    "90+"
                                )
                            )
                        }

                        axisLeft.apply {
                            setDrawGridLines(false)
                            setDrawAxisLine(false)
                            textColor = AndroidColor.parseColor("#555555")
                            textSize = 9f
                            axisMinimum = 0f
                            valueFormatter =
                                object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return value.toInt().toString()
                                    }
                                }
                        }

                        axisRight.isEnabled = false
                    }
                },
                update = { chart ->
                    if (heatmap.isEmpty()) {
                        chart.clear()
                    } else {
                        val entries = ArrayList<BarEntry>()
                        heatmap.forEachIndexed { index, valGoal ->
                            entries.add(BarEntry(index.toFloat(), valGoal.toFloat()))
                        }

                        val maxVal = heatmap.maxOrNull() ?: 1
                        if (maxVal > 0) {
                            val colorsList = heatmap.map { valGoal ->
                                val ratio = valGoal.toFloat() / maxVal
                                val alpha = Math.max(70, (ratio * 255).toInt())
                                AndroidColor.argb(alpha, 0, 230, 118)
                            }

                            val intFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return value.toInt().toString()
                                }
                            }
                            val dataSet = BarDataSet(entries, "Goals Timing").apply {
                                colors = colorsList
                                setDrawValues(true)
                                valueTextColor = AndroidColor.WHITE
                                valueTextSize = 9f
                                valueFormatter = intFormatter
                            }
                            chart.data = BarData(dataSet)
                            chart.animateY(800)
                            chart.invalidate()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun TeamTimingOverlayBlock(
    teams: List<Triple<Int, String, String?>>,
    teamTiming: Map<Int, TeamGoalTiming>
) {
    var selectedTeamId by remember(teams) { mutableStateOf(teams.firstOrNull()?.first ?: 0) }
    val selectedTeam = remember(selectedTeamId, teams) {
        teams.find { it.first == selectedTeamId }
    }
    val selectedTeamName = selectedTeam?.second ?: "Select Team"
    val selectedTeamLogo = selectedTeam?.third

    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Dropdown Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D0F14))
                    .border(0.5.dp, Color(0xFF1A1E2A), RoundedCornerShape(10.dp))
                    .clickable { dropdownExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Team logo circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedTeamLogo,
                            contentDescription = selectedTeamName,
                            modifier = Modifier.size(20.dp),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_placeholder)
                        )
                    }
                    Text(
                        text = selectedTeamName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF00E676)
                )
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color(0xFF131620))
            ) {
                teams.forEach { (id, name, logo) ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2433)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = logo,
                                        contentDescription = name,
                                        modifier = Modifier.size(16.dp),
                                        placeholder = painterResource(R.drawable.ic_placeholder),
                                        error = painterResource(R.drawable.ic_placeholder)
                                    )
                                }
                                Text(
                                    text = name,
                                    color = if (id == selectedTeamId) Color(0xFF00E676) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (id == selectedTeamId) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            selectedTeamId = id
                            dropdownExpanded = false
                        }
                    )
                }
            }


            Spacer(Modifier.height(14.dp))

            val currentTiming = teamTiming[selectedTeamId]
            if (currentTiming != null) {
                // Legends
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    LegendIndicator(color = Color(0xFF00E676), label = "Goals Scored")
                    LegendIndicator(color = Color(0xFFFF4444), label = "Goals Conceded")
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    factory = { ctx ->
                        com.github.mikephil.charting.charts.BarChart(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            setDrawBorders(false)
                            description.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = false
                            setScaleEnabled(false)

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                textColor = AndroidColor.parseColor("#555555")
                                textSize = 9f
                                granularity = 1f
                                isGranularityEnabled = true
                                valueFormatter = IndexAxisValueFormatter(
                                    listOf(
                                        "0-15",
                                        "16-30",
                                        "31-45",
                                        "45+",
                                        "46-60",
                                        "61-75",
                                        "76-90",
                                        "90+"
                                    )
                                )
                            }

                            axisLeft.apply {
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                textColor = AndroidColor.parseColor("#555555")
                                textSize = 9f
                                axisMinimum = 0f
                                valueFormatter = object :
                                    com.github.mikephil.charting.formatter.ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return value.toInt().toString()
                                    }
                                }
                            }

                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val currentTiming = teamTiming[selectedTeamId]
                        if (currentTiming == null || currentTiming.scoredTiming.isEmpty()) {
                            chart.clear()
                        } else {
                            val entriesScored = ArrayList<BarEntry>()
                            val entriesConceded = ArrayList<BarEntry>()

                            currentTiming.scoredTiming.forEachIndexed { idx, scored ->
                                entriesScored.add(BarEntry(idx.toFloat(), scored.toFloat()))
                            }
                            currentTiming.concededTiming.forEachIndexed { idx, conceded ->
                                entriesConceded.add(BarEntry(idx.toFloat(), conceded.toFloat()))
                            }

                            val intFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return value.toInt().toString()
                                }
                            }
                            val setScored = BarDataSet(entriesScored, "Scored").apply {
                                color = AndroidColor.parseColor("#00E676")
                                setDrawValues(true)
                                valueTextColor = AndroidColor.WHITE
                                valueTextSize = 8f
                                valueFormatter = intFormatter
                            }

                            val setConceded = BarDataSet(entriesConceded, "Conceded").apply {
                                color = AndroidColor.parseColor("#FF4444")
                                setDrawValues(true)
                                valueTextColor = AndroidColor.WHITE
                                valueTextSize = 8f
                                valueFormatter = intFormatter
                            }

                            val data = BarData(setScored, setConceded)
                            data.barWidth = 0.35f
                            chart.data = data

                            val groupSpace = 0.15f
                            val barSpace = 0.05f
                            chart.groupBars(-0.5f, groupSpace, barSpace)
                            chart.xAxis.axisMaximum = 7.5f
                            chart.xAxis.axisMinimum = -0.5f

                            chart.animateY(800)
                            chart.invalidate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FirstGoalAdvantageCard(
    data: FirstGoalAdvantageData,
    viewModel: StatsViewModel
) {
    val selectedLeague = viewModel.selectedLeague.collectAsState().value
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%.0f%%", data.firstGoalWinsPct),
                    color = Color(0xFF00E676),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "First Goal Advantage",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Teams that score first win ${
                        String.format(
                            "%.0f%%",
                            data.firstGoalWinsPct
                        )
                    } of the time in the ${selectedLeague.name} this season (${data.sampleCount} matches analyzed).",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ==========================================
// TAB 5 — DISCIPLINE
// ==========================================
@Composable
fun DisciplineTabContent(
    state: DisciplineUiState,
    viewModel: StatsViewModel
) {
    when (state) {
        is DisciplineUiState.Idle, is DisciplineUiState.Loading -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(3) { MatchRowShimmer() }
            }
        }

        is DisciplineUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Section A — Most Carded Players
                Text(
                    text = "Most Carded Players",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                MostCardedPlayersList(state.mostCardedPlayers)

                Spacer(Modifier.height(20.dp))

                // Section B — Dirtiest Teams Chart
                Text(
                    text = "Dirtiest Teams (Yellow & Red Cards)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DirtiestTeamsChart(state.dirtiestTeams)

                Spacer(Modifier.height(20.dp))

                // Section C — Foul leaders vs Most Fouled
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fouls Committed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555555),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FoulsStatList(state.foulLeaders, Color(0xFFFF4444))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fouls Suffered / Drawn",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555555),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FoulsStatList(state.mostFouledPlayers, Color(0xFF00E676))
                    }
                }

                Spacer(Modifier.height(30.dp))
            }
        }

        is DisciplineUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MostCardedPlayersList(players: List<PlayerCardsStat>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            players.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = Color(0xFF555555),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )

                    // Photo
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = item.playerPhoto,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_placeholder)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (item.teamLogo != null) {
                                AsyncImage(
                                    model = item.teamLogo,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = item.teamName ?: "",
                                color = Color(0xFF555555),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Card Count Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Yellow Card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 12.dp)
                                    .background(Color(0xFFF0A500), RoundedCornerShape(1.dp))
                            )
                            Text(
                                text = "${item.yellowCount}",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        // Red Card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 12.dp)
                                    .background(Color(0xFFFF4444), RoundedCornerShape(1.dp))
                            )
                            Text(text = "${item.redCount}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                if (index < players.size - 1) {
                    HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun DirtiestTeamsChart(teams: List<TeamCardsStat>) {
    val maxTotal = teams.maxOfOrNull { it.yellowCount + it.redCount } ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            // Legend header
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp, 12.dp)
                            .background(Color(0xFFF0A500), RoundedCornerShape(1.dp))
                    )
                    Text("Yellow", color = Color(0xFF888888), fontSize = 9.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp, 12.dp)
                            .background(Color(0xFFFF4444), RoundedCornerShape(1.dp))
                    )
                    Text("Red", color = Color(0xFF888888), fontSize = 9.sp)
                }
            }
            HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)

            teams.forEachIndexed { index, team ->
                val total = team.yellowCount + team.redCount
                val yellowRatio = if (total > 0) team.yellowCount.toFloat() / total else 0f
                val totalRatio =
                    if (maxTotal > 0) total.toFloat() / maxTotal.coerceAtLeast(1) else 0f

                val rankColor = when (index) {
                    0 -> Color(0xFFFFD700)
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> Color(0xFF444455)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Rank badge
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(rankColor.copy(alpha = if (index < 3) 0.15f else 0.05f))
                                .border(
                                    0.5.dp,
                                    rankColor.copy(alpha = if (index < 3) 0.5f else 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = if (index < 3) rankColor else Color(0xFF555555),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Team logo
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2433)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = team.teamLogo,
                                contentDescription = team.teamName,
                                modifier = Modifier.size(22.dp),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_placeholder)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Team name
                        Text(
                            text = team.teamName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.width(10.dp))

                        // Yellow card count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp, 10.dp)
                                    .background(Color(0xFFF0A500), RoundedCornerShape(1.dp))
                            )
                            Text(
                                text = "${team.yellowCount}",
                                color = Color(0xFFF0A500),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Red card count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp, 10.dp)
                                    .background(Color(0xFFFF4444), RoundedCornerShape(1.dp))
                            )
                            Text(
                                text = "${team.redCount}",
                                color = Color(0xFFFF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(7.dp))

                    // Stacked progress bar
                    Box(
                        modifier = Modifier
                            .padding(start = 58.dp)
                            .fillMaxWidth(totalRatio.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                    ) {
                        // Full background (red portion)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFF4444))
                        )
                        // Yellow portion on top
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(yellowRatio.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Color(0xFFF0A500))
                        )
                    }
                }

                if (index < teams.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = Color(0xFF1A1E2A),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}


@Composable
fun FoulsStatList(
    players: List<PlayerFoulsStat>,
    barColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            players.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Player photo
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2433))
                                .border(1.dp, barColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = item.playerPhoto,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_placeholder)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // Name + team logo
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                AsyncImage(
                                    model = item.teamLogo,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = item.teamName ?: "",
                                    color = Color(0xFF555555),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = "${item.count}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Progress bar
                    val maxVal = players.firstOrNull()?.count ?: 1
                    val ratio = item.count.toFloat() / maxVal.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                }
                if (index < players.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = Color(0xFF1A1E2A),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun TopThreePodium(
    players: List<PlayerProfileStatisticsResponse>,
    statType: String, // "Goals" or "Assists"
    onPlayerClick: (Int) -> Unit
) {
    if (players.size < 3) return

    val p1 = players.getOrNull(0) ?: return
    val p2 = players.getOrNull(1) ?: return
    val p3 = players.getOrNull(2) ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // #2 Place (Left)
        PodiumCol(
            player = p2,
            rank = 2,
            statType = statType,
            borderColor = Color(0xFFC0C0C0), // Silver
            onPlayerClick = onPlayerClick,
            modifier = Modifier.weight(1f)
        )

        // #1 Place (Center)
        PodiumCol(
            player = p1,
            rank = 1,
            statType = statType,
            borderColor = Color(0xFFFFD700), // Gold
            onPlayerClick = onPlayerClick,
            modifier = Modifier.weight(1.1f)
        )

        // #3 Place (Right)
        PodiumCol(
            player = p3,
            rank = 3,
            statType = statType,
            borderColor = Color(0xFFCD7F32), // Bronze
            onPlayerClick = onPlayerClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PodiumCol(
    player: PlayerProfileStatisticsResponse,
    rank: Int,
    statType: String,
    borderColor: Color,
    onPlayerClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = player.statistics?.firstOrNull()
    val count = if (statType == "Goals") {
        stats?.goals?.total ?: 0
    } else {
        stats?.goals?.assists ?: 0
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131620))
            .border(0.5.dp, Color(0xFF1A1E2A), RoundedCornerShape(16.dp))
            .clickable { player.player?.id?.let { onPlayerClick(it) } }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Player Photo with glowing rank border
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(if (rank == 1) 56.dp else 46.dp)
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape)
                    .background(Color(0xFF1E2433)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = player.player?.photo,
                    contentDescription = player.player?.name,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_placeholder)
                )
            }
            // Rank Badge
            Box(
                modifier = Modifier
                    .offset(y = 4.dp)
                    .clip(CircleShape)
                    .background(borderColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    color = Color(0xFF0D0F14),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = player.player?.name?.split(" ")?.lastOrNull() ?: "Player",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AsyncImage(
                model = stats?.team?.logo,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_placeholder)
            )
            Text(
                text = "$count $statType",
                color = if (rank == 1) Color(0xFF00E676) else Color(0xFF888888),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ClinicalFinishersLeaderboard(performers: List<PlayerXgPerformance>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1E2A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131620))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // Header explanation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Player",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Goals / xG",
                        color = Color(0xFF555555),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Diff",
                        color = Color(0xFF555555),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)

            performers.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank
                    Text(
                        text = "${index + 1}",
                        color = if (item.diff >= 0) Color(0xFF00E676) else Color(0xFFFF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp)
                    )

                    // Photo
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = item.playerPhoto,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_placeholder)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Player Meta
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (item.teamLogo != null) {
                                AsyncImage(
                                    model = item.teamLogo,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = if (item.diff >= 0) "Outperforming xG" else "Underperforming xG",
                                color = if (item.diff >= 0) Color(0xFF00E676).copy(alpha = 0.6f) else Color(
                                    0xFFFF4444
                                ).copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Goals / xG
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = "${item.goals}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%.2f xG", item.xg),
                            color = Color(0xFF888888),
                            fontSize = 9.sp
                        )
                    }

                    // Difference Badge
                    val diffText = if (item.diff >= 0) "+${
                        String.format(
                            "%.2f",
                            item.diff
                        )
                    }" else String.format("%.2f", item.diff)
                    val diffBg =
                        if (item.diff >= 0) Color(0xFF00E676).copy(alpha = 0.15f) else Color(
                            0xFFFF4444
                        ).copy(alpha = 0.15f)
                    val diffColor = if (item.diff >= 0) Color(0xFF00E676) else Color(0xFFFF4444)
                    val diffBorder =
                        if (item.diff >= 0) Color(0xFF00E676).copy(alpha = 0.3f) else Color(
                            0xFFFF4444
                        ).copy(alpha = 0.3f)

                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(diffBg)
                            .border(0.5.dp, diffBorder, RoundedCornerShape(6.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diffText,
                            color = diffColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (index < performers.size - 1) {
                    HorizontalDivider(color = Color(0xFF1A1E2A), thickness = 0.5.dp)
                }
            }
        }
    }
}
