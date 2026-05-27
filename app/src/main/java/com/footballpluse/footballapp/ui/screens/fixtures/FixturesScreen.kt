package com.footballpluse.footballapp.ui.screens.fixtures

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.ui.components.MatchRow
import com.footballpluse.footballapp.ui.components.MatchRowShimmer
import com.footballpluse.footballapp.ui.components.SectionHeader
import com.footballpluse.footballapp.ui.theme.*
import com.footballpluse.footballapp.viewmodel.FixturesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixturesScreen(
    onNavigateToMatchCenter: (String) -> Unit,
    viewModel: FixturesViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    val fixturesState by viewModel.fixturesState.collectAsState()
    var liveOnly by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        viewModel.getFixturesByDate(selectedDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // Date Rail
        DateRail(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        // Live Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = !liveOnly,
                onClick = { liveOnly = false },
                label = { Text("All Matches", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LiveGreen,
                    selectedLabelColor = DeepNavy,
                    containerColor = Color.White.copy(alpha = 0.05f),
                    labelColor = Color.White.copy(alpha = 0.6f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = !liveOnly,
                    borderColor = Color.White.copy(alpha = 0.08f),
                    selectedBorderColor = LiveGreen,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                ),
                shape = RoundedCornerShape(50.dp)
            )

            FilterChip(
                selected = liveOnly,
                onClick = { liveOnly = true },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (liveOnly) DeepNavy else ScoreGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Live Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ScoreGreen,
                    selectedLabelColor = DeepNavy,
                    containerColor = Color.White.copy(alpha = 0.05f),
                    labelColor = Color.White.copy(alpha = 0.6f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = liveOnly,
                    borderColor = Color.White.copy(alpha = 0.08f),
                    selectedBorderColor = ScoreGreen,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                ),
                shape = RoundedCornerShape(50.dp)
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
                val matches = if (liveOnly) rawMatches.filter { it.isLive } else rawMatches
                
                if (matches.isEmpty()) {
                    EmptyFixtures(liveOnly)
                } else {
                    FixturesList(matches, onNavigateToMatchCenter)
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
fun DateRail(selectedDate: String, onDateSelected: (String) -> Unit) {
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
            
            DateItem(
                day = daySdf.format(date),
                date = dateSdf.format(date),
                isSelected = isSelected,
                onClick = { onDateSelected(dateStr) }
            )
        }
    }
}

@Composable
fun DateItem(day: String, date: String, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.08f else 1.0f, label = "date-scale")
    Column(
        modifier = Modifier
            .width(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    Brush.verticalGradient(listOf(GlassGlowGreen, LiveGreen))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                }
            )
            .border(
                1.dp,
                if (isSelected) GlassGlowGreen.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) DeepNavy else Color.White.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = date,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) DeepNavy else Color.White
        )
    }
}

@Composable
fun FixturesList(matches: List<Match>, onMatchClick: (String) -> Unit) {
    val grouped = matches.groupBy { it.league }
    val expandedLeagues = remember { mutableStateMapOf<Int, Boolean>() }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (league, leagueMatches) ->
            val isExpanded = expandedLeagues[league.id] ?: true
            item(key = league.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedLeagues[league.id] = !isExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = league.logo,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = league.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = if (isExpanded) "COLLAPSE" else "EXPAND (${leagueMatches.size})",
                        color = GlassGlowGreen.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isExpanded) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.04f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    ) {
                        Column {
                            leagueMatches.forEachIndexed { index, match ->
                                MatchRow(
                                    match = match,
                                    onClick = onMatchClick
                                )
                                if (index < leagueMatches.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.06f)
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

@Composable
fun EmptyFixtures(liveOnly: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (liveOnly) "No live matches in play right now" else "No matches scheduled for this date",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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

