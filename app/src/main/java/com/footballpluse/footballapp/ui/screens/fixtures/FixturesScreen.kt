package com.footballpluse.footballapp.ui.screens.fixtures

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.ui.components.MatchRow
import com.footballpluse.footballapp.ui.components.MatchRowShimmer
import com.footballpluse.footballapp.ui.theme.*
import com.footballpluse.footballapp.viewmodel.FixturesViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class FixtureFilter {
    ALL,
    LIVE,
    FINISHED,
    MY_CLUBS
}

@Composable
fun FixturesScreen(
    onNavigateToMatchCenter: (String) -> Unit,
    viewModel: FixturesViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    val fixturesState by viewModel.fixturesState.collectAsState()
    val dateMatchCounts by viewModel.dateMatchCounts.collectAsState()
    val liveCount by viewModel.liveCount.collectAsState()
    val formMap by viewModel.formMap.collectAsState()
    val favouriteClubIds by viewModel.favouriteClubIds.collectAsState()
    var currentFilter by remember { mutableStateOf(FixtureFilter.ALL) }

    LaunchedEffect(selectedDate) {
        viewModel.getFixturesByDate(selectedDate)
    }

    val finishedCount = remember(fixturesState) {
        (fixturesState as? ApiResult.Success)?.data?.count { it.status.short in listOf("FT", "AET", "PEN") } ?: 0
    }

    val myClubsLiveCount = remember(fixturesState, favouriteClubIds) {
        val rawMatches = (fixturesState as? ApiResult.Success)?.data.orEmpty()
        rawMatches.count { it.isLive && (it.homeTeam.id in favouriteClubIds || it.awayTeam.id in favouriteClubIds) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // Date Rail
        DateRail(
            selectedDate = selectedDate,
            dateMatchCounts = dateMatchCounts,
            onDateSelected = { selectedDate = it }
        )

        // Filter Pills Row (Horizontal scrolling for responsiveness)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterPill(
                text = "All Matches",
                isSelected = currentFilter == FixtureFilter.ALL,
                onClick = { currentFilter = FixtureFilter.ALL }
            )

            FilterPill(
                text = "Live Now · $liveCount",
                isSelected = currentFilter == FixtureFilter.LIVE,
                onClick = { currentFilter = FixtureFilter.LIVE },
                customColors = Triple(
                    Color(0xFF1A0A0A),
                    Color(0xFF3A1212),
                    Color(0xFFFF4444)
                ),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4444))
                    )
                }
            )

            FilterPill(
                text = "Finished · $finishedCount",
                isSelected = currentFilter == FixtureFilter.FINISHED,
                onClick = { currentFilter = FixtureFilter.FINISHED },
                customColors = if (currentFilter == FixtureFilter.FINISHED) {
                    Triple(Color(0xFF131620), Color(0xFF00E676), Color(0xFF00E676))
                } else {
                    Triple(Color(0xFF131620), Color(0xFF1A1E2A), Color(0xFF888888))
                }
            )

            FilterPill(
                text = "My Clubs · $myClubsLiveCount",
                isSelected = currentFilter == FixtureFilter.MY_CLUBS,
                onClick = { currentFilter = FixtureFilter.MY_CLUBS }
            )
        }

        when (val state = fixturesState) {
            is ApiResult.Loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) {
                        MatchRowShimmer()
                    }
                }
            }
            is ApiResult.Success -> {
                val rawMatches = state.data
                val matches = when (currentFilter) {
                    FixtureFilter.ALL -> rawMatches
                    FixtureFilter.LIVE -> rawMatches.filter { it.isLive }
                    FixtureFilter.FINISHED -> rawMatches.filter { it.status.short in listOf("FT", "AET", "PEN") }
                    FixtureFilter.MY_CLUBS -> rawMatches.filter { it.isLive && (it.homeTeam.id in favouriteClubIds || it.awayTeam.id in favouriteClubIds) }
                }
                
                if (matches.isEmpty()) {
                    EmptyFixtures(currentFilter)
                } else {
                    FixturesList(
                        matches = matches,
                        formMap = formMap,
                        onFetchForm = { teamId, leagueId, season ->
                            viewModel.fetchFormIfNeeded(teamId, leagueId, season)
                        },
                        onMatchClick = onNavigateToMatchCenter
                    )
                }
            }
            is ApiResult.Error -> {
                ErrorView(message = state.message) {
                    viewModel.getFixturesByDate(selectedDate)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customColors: Triple<Color, Color, Color>? = null, // background, border, text
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val backgroundColor = customColors?.first ?: if (isSelected) Color(0xFF00E676) else Color(0xFF131620)
    val borderColor = customColors?.second ?: if (isSelected) Color(0xFF00E676) else Color(0xFF1A1E2A)
    val textColor = customColors?.third ?: if (isSelected) Color(0xFF0D0F14) else Color(0xFF888888)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun DateRail(
    selectedDate: String,
    dateMatchCounts: Map<String, Int>,
    onDateSelected: (String) -> Unit
) {
    val dates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        repeat(14) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val daySdf = SimpleDateFormat("EEE", Locale.getDefault())
    val dateSdf = SimpleDateFormat("dd", Locale.getDefault())

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = dates) { date ->
            val dateStr = sdf.format(date)
            val isSelected = dateStr == selectedDate
            val count = dateMatchCounts[dateStr] ?: 0
            val hasMatches = count > 0
            
            DateItem(
                day = daySdf.format(date),
                date = dateSdf.format(date),
                isSelected = isSelected,
                hasMatches = hasMatches,
                onClick = { onDateSelected(dateStr) }
            )
        }
    }
}

@Composable
fun DateItem(
    day: String,
    date: String,
    isSelected: Boolean,
    hasMatches: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.08f else 1.0f, label = "date-scale")

    val dayColor = when {
        isSelected -> Color(0xFF0D0F14)
        hasMatches -> Color(0xFF00E676)
        else -> Color(0xFF555555)
    }

    val numColor = when {
        isSelected -> Color(0xFF0D0F14)
        hasMatches -> Color(0xFFFFFFFF)
        else -> Color(0xFF666666)
    }

    Column(
        modifier = Modifier
            .width(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFF00E676) else Color(0xFF131620))
            .border(
                0.5.dp,
                if (isSelected) Color(0xFF00E676) else Color(0xFF1A1E2A),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = dayColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = date,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = numColor
        )
    }
}

@Composable
fun FixturesList(
    matches: List<Match>,
    formMap: Map<Int, String>,
    onFetchForm: (Int, Int, Int) -> Unit,
    onMatchClick: (String) -> Unit
) {
    val grouped = remember(matches) { matches.groupBy { it.league } }
    val expandedLeagues = remember { mutableStateMapOf<Int, Boolean>() }
    val listState = rememberLazyListState()

    LaunchedEffect(listState.firstVisibleItemIndex, grouped) {
        val visibleKeys = listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Int }
        val teamsToFetch = visibleKeys.flatMap { leagueId ->
            val leagueMatches = grouped.entries.firstOrNull { it.key.id == leagueId }?.value.orEmpty()
            leagueMatches.flatMap { match ->
                val season = match.league.season ?: 2025
                listOf(
                    Triple(match.homeTeam.id, match.league.id, season),
                    Triple(match.awayTeam.id, match.league.id, season)
                )
            }
        }.distinctBy { it.first }

        teamsToFetch.forEach { (teamId, leagueId, season) ->
            onFetchForm(teamId, leagueId, season)
        }
    }

    LaunchedEffect(matches) {
        matches.forEach { match ->
            val season = match.league.season ?: 2025
            onFetchForm(match.homeTeam.id, match.league.id, season)
            onFetchForm(match.awayTeam.id, match.league.id, season)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (league, leagueMatches) ->
            val isExpanded = expandedLeagues[league.id] ?: true
            item(key = league.id) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)),
                    border = BorderStroke(0.5.dp, Color(0xFF1A1E2A))
                ) {
                    Column {
                        // Header Row
                        val hasLive = leagueMatches.any { it.isLive }
                        val liveCount = leagueMatches.count { it.isLive }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF161A26))
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFF00E676),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .clickable { expandedLeagues[league.id] = !isExpanded }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = league.logo,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = league.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                                text = "${leagueMatches.size} matches",
                                                color = badgeTextColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = Color(0xFF444444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Matches List (Animated)
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                leagueMatches.forEachIndexed { index, match ->
                                    val homeForm = formMap[match.homeTeam.id] ?: ""
                                    val awayForm = formMap[match.awayTeam.id] ?: ""
                                    MatchRow(
                                        match = match,
                                        homeForm = homeForm,
                                        awayForm = awayForm,
                                        onClick = onMatchClick
                                    )
                                    if (index < leagueMatches.size - 1) {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = Color(0xFF1A1E2A)
                                        )
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
private fun EmptyFixtures(filter: FixtureFilter) {
    val message = when (filter) {
        FixtureFilter.LIVE -> "No live matches in play right now"
        FixtureFilter.FINISHED -> "No finished matches for this date"
        FixtureFilter.MY_CLUBS -> "No live matches for your favorite clubs right now"
        else -> "No matches scheduled for this date"
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = DangerRed, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = GlassGlowGreen, contentColor = DeepNavy)) {
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}
