package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult
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

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = LeagueTab.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.leagueInfo?.name ?: "League Detail",
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F0F0F))
        ) {
            if (state.leagueInfo != null) {
                LeagueHeader(state.leagueInfo!!)
            }

            LeagueTabRow(
                tabs = tabs.map { it.label },
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(Modifier.height(8.dp))

            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(200),
                label = "tab-crossfade"
            ) { tabIndex ->
                when (tabs[tabIndex]) {
                    LeagueTab.STANDINGS -> StandingsTab(
                        standings = state.standings,
                        onTeamClick = onTeamClick
                    )
                    LeagueTab.FIXTURES -> FixturesTab(
                        fixtures = state.fixtures,
                        onMatchClick = onMatchClick
                    )
                    LeagueTab.PLAYER_STATS -> PlayerStatsTab(
                        topScorers = state.topScorers,
                        topAssists = state.topAssists,
                        topYellowCards = state.topYellowCards,
                        topRedCards = state.topRedCards
                    )
                    LeagueTab.SEASON_STATS -> SeasonStatsTab(
                        seasonStats = state.seasonStats
                    )
                    LeagueTab.H2H -> HeadToHeadTab(
                        teams = state.teams,
                        selectedTeamA = state.selectedTeamA,
                        selectedTeamB = state.selectedTeamB,
                        h2hData = state.h2hData,
                        onSelectTeamA = { viewModel.selectTeamA(it) },
                        onSelectTeamB = { viewModel.selectTeamB(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun LeagueTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFF0F0F0F),
        contentColor = Color(0xFF4ADE80),
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                height = 3.dp,
                color = Color(0xFF4ADE80)
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        title,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == index) Color.White else Color(0xFF555555)
                    )
                }
            )
        }
    }
}

@Composable
fun LeagueHeader(leagueInfo: com.footballpluse.footballapp.domain.model.LeagueInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            AsyncImage(
                model = leagueInfo.logo,
                contentDescription = leagueInfo.name,
                modifier = Modifier.size(40.dp).align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = leagueInfo.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        if (leagueInfo.country != null) {
            Text(
                text = "${leagueInfo.country} \u00B7 ${leagueInfo.season ?: ""}",
                fontSize = 11.sp,
                letterSpacing = 0.08.sp,
                color = Color(0xFFA0A0A0)
            )
        }
    }
}

// ─── STANDINGS TAB ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsTab(
    standings: ApiResult<List<StandingRowUiModel>>,
    onTeamClick: (Int) -> Unit
) {
    var selectedTeam by remember { mutableStateOf<StandingRowUiModel?>(null) }

    if (selectedTeam != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTeam = null },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            TeamDetailBottomSheet(selectedTeam!!)
        }
    }

    when (standings) {
        is ApiResult.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(5) {
                    StandingsRowShimmer()
                }
            }
        }
        is ApiResult.Success -> {
            if (standings.data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No standings available", color = Color(0xFFA0A0A0))
                }
            } else {
                val items = standings.data
                val totalRows = items.size
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item { StandingsHeaderRow() }
                    itemsIndexed(items) { index, item ->
                        StandingRow(
                            item = item,
                            isTopThree = item.rank <= 3,
                            isBottomThree = item.rank > totalRows - 3,
                            showPromotionDivider = item.rank == 3,
                            showRelegationDivider = item.rank == totalRows - 3,
                            onClick = { selectedTeam = item },
                            onTeamClick = { onTeamClick(item.team.id) }
                        )
                    }
                }
            }
        }
        is ApiResult.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(standings.message, color = Color(0xFFEF4444), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun StandingsHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", modifier = Modifier.width(24.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A0A0))
        Spacer(Modifier.width(4.dp))
        Text("Team", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A0A0))
        Text("P", modifier = Modifier.width(24.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFA0A0A0))
        Text("W", modifier = Modifier.width(24.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFA0A0A0))
        Text("D", modifier = Modifier.width(24.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFA0A0A0))
        Text("L", modifier = Modifier.width(24.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFA0A0A0))
        Text("GD", modifier = Modifier.width(28.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFA0A0A0))
        Text("Pts", modifier = Modifier.width(28.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFA0A0A0))
        Spacer(Modifier.width(4.dp))
        Text("Form", modifier = Modifier.width(52.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFA0A0A0))
    }
}

@Composable
fun StandingRow(
    item: StandingRowUiModel,
    isTopThree: Boolean,
    isBottomThree: Boolean,
    showPromotionDivider: Boolean,
    showRelegationDivider: Boolean,
    onClick: () -> Unit,
    onTeamClick: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = BorderStroke(0.5.dp, Color(0xFF242424))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(40.dp)
                        .background(
                            when {
                                isTopThree -> Color(0xFF4ADE80)
                                isBottomThree -> Color(0xFFEF4444)
                                else -> Color.Transparent
                            }
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.rank.toString(),
                        modifier = Modifier.width(24.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTopThree) Color(0xFF4ADE80) else Color.White
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = item.team.logo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.team.name,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.clickable { onTeamClick() }
                        )
                    }

                    Text(item.played.toString(), modifier = Modifier.width(24.dp), fontSize = 11.sp, color = Color(0xFFA0A0A0), textAlign = TextAlign.Center)
                    Text(item.win.toString(), modifier = Modifier.width(24.dp), fontSize = 11.sp, color = Color(0xFFA0A0A0), textAlign = TextAlign.Center)
                    Text(item.draw.toString(), modifier = Modifier.width(24.dp), fontSize = 11.sp, color = Color(0xFFA0A0A0), textAlign = TextAlign.Center)
                    Text(item.lose.toString(), modifier = Modifier.width(24.dp), fontSize = 11.sp, color = Color(0xFFA0A0A0), textAlign = TextAlign.Center)

                    Text(
                        text = if (item.goalsDiff >= 0) "+${item.goalsDiff}" else item.goalsDiff.toString(),
                        modifier = Modifier.width(28.dp),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = if (item.goalsDiff >= 0) Color(0xFF4ADE80) else Color(0xFFEF4444)
                    )

                    Text(
                        item.points.toString(),
                        modifier = Modifier.width(28.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.width(4.dp))

                    FormDots(
                        form = item.form ?: "UUUUU",
                        dotSize = 8.dp,
                        modifier = Modifier.width(52.dp)
                    )
                }
            }
        }

        if (showPromotionDivider) {
            DashedLine(color = Color(0xFF4ADE80).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
        }
        if (showRelegationDivider) {
            DashedLine(color = Color(0xFFEF4444).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}

@Composable
fun FormDots(
    form: String,
    dotSize: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val results = form.takeLast(5).padStart(5, 'U')
        results.forEach { result ->
            val color = when (result) {
                'W' -> Color(0xFF4ADE80)
                'D' -> Color(0xFF555555)
                'L' -> Color(0xFFEF4444)
                else -> Color(0xFF1E1E1E)
            }
            val isLoss = result == 'L'
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .then(
                        if (isLoss) Modifier.clip(CircleShape).border(1.5.dp, color, CircleShape)
                        else Modifier.clip(CircleShape).background(color)
                    )
            )
        }
    }
}

@Composable
fun DashedLine(
    color: Color = Color(0xFF4ADE80),
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.5f,
            pathEffect = pathEffect
        )
    }
}

@Composable
fun StandingsRowShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(24.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
            Box(Modifier.width(24.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
            Box(Modifier.width(24.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
            Box(Modifier.width(24.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
            Box(Modifier.width(28.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
            Box(Modifier.width(28.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
        }
    }
}

// ─── TEAM DETAIL BOTTOM SHEET ────────────────────────────────────────────────

@Composable
fun TeamDetailBottomSheet(team: StandingRowUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = team.team.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "RECENT FORM",
            fontSize = 11.sp,
            letterSpacing = 0.08.sp,
            color = Color(0xFFA0A0A0)
        )
        Spacer(Modifier.height(8.dp))
        FormDots(form = team.form ?: "UUUUU", dotSize = 10.dp)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF1E1E1E), thickness = 1.dp)

        StatRow(label = "Goals Scored", value = "${team.goalsFor}")
        StatRow(label = "Goals Conceded", value = "${team.goalsAgainst}")
        StatRow(label = "Goal Difference", value = if (team.goalsDiff >= 0) "+${team.goalsDiff}" else team.goalsDiff.toString())

        Spacer(Modifier.height(12.dp))
        Text(
            text = "HOME VS AWAY RECORD",
            fontSize = 11.sp,
            letterSpacing = 0.08.sp,
            color = Color(0xFFA0A0A0)
        )
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Home", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text("P${team.homePlayed} \u00B7 W${team.homeWon} D${team.homeDraw} L${team.homeLost}", fontSize = 11.sp, color = Color(0xFFA0A0A0))
                Text("GF ${team.homeGoalsFor} \u00B7 GA ${team.homeGoalsAgainst}", fontSize = 11.sp, color = Color(0xFF555555))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Away", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text("P${team.awayPlayed} \u00B7 W${team.awayWon} D${team.awayDraw} L${team.awayLost}", fontSize = 11.sp, color = Color(0xFFA0A0A0))
                Text("GF ${team.awayGoalsFor} \u00B7 GA ${team.awayGoalsAgainst}", fontSize = 11.sp, color = Color(0xFF555555))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFFA0A0A0))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}
