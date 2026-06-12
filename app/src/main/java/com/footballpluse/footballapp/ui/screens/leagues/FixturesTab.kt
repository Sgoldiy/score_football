package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult

@Composable
fun FixturesTab(
    fixtures: ApiResult<List<FixtureUiModel>>,
    onMatchClick: (Int) -> Unit
) {
    when (fixtures) {
        is ApiResult.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { FixtureCardShimmer() }
            }
        }
        is ApiResult.Success -> {
            val allFixtures = fixtures.data
            if (allFixtures.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No fixtures available", color = Color(0xFFA0A0A0))
                }
            } else {
                FixturesContent(allFixtures = allFixtures, onMatchClick = onMatchClick)
            }
        }
        is ApiResult.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(fixtures.message, color = Color(0xFFEF4444), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun FixturesContent(
    allFixtures: List<FixtureUiModel>,
    onMatchClick: (Int) -> Unit
) {
    val liveFixtures = allFixtures.filter { it.status == MatchStatusUi.LIVE }
    val completedFixtures = allFixtures.filter { it.status == MatchStatusUi.COMPLETED }
    val upcomingFixtures = allFixtures.filter { it.status == MatchStatusUi.UPCOMING }

    val hasLive = liveFixtures.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MatchdaySummaryBar(allFixtures)
        }

        item {
            MatchdayHeaderChip(hasLive = hasLive, liveCount = liveFixtures.size)
        }

        if (hasLive) {
            item {
                FixtureSectionHeader(title = "LIVE", color = Color(0xFFEF4444))
            }
            items(liveFixtures) { fixture ->
                FixtureCard(fixture = fixture, isLive = true, onMatchClick = onMatchClick)
            }
        }

        if (upcomingFixtures.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                FixtureSectionHeader(title = "UPCOMING", color = Color(0xFFA0A0A0))
            }
            items(upcomingFixtures) { fixture ->
                FixtureCard(fixture = fixture, isLive = false, isUpcoming = true, onMatchClick = onMatchClick)
            }
        }

        if (completedFixtures.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                FixtureSectionHeader(title = "COMPLETED", color = Color(0xFF555555))
            }
            items(completedFixtures) { fixture ->
                FixtureCard(fixture = fixture, isLive = false, isUpcoming = false, onMatchClick = onMatchClick)
            }
        }
    }
}

@Composable
fun MatchdaySummaryBar(fixtures: List<FixtureUiModel>) {
    val totalGoals = fixtures.sumOf { (it.homeScore ?: 0) + (it.awayScore ?: 0) }
    val completed = fixtures.filter { it.status == MatchStatusUi.COMPLETED }
    val cleanSheets = completed.count { it.awayScore == 0 } + completed.count { it.homeScore == 0 }
    val avgGoals = if (completed.isNotEmpty()) totalGoals.toFloat() / completed.size else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(text = "$totalGoals Goals")
        SummaryChip(text = "$cleanSheets Clean Sheets")
        SummaryChip(text = "Avg ${String.format("%.1f", avgGoals)}/Game")
        if (fixtures.isNotEmpty()) {
            SummaryChip(text = fixtures.first().homeTeam.name)
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFFA0A0A0),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun MatchdayHeaderChip(hasLive: Boolean = false, liveCount: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Matchday",
            fontSize = 11.sp,
            letterSpacing = 0.08.sp,
            color = Color(0xFFA0A0A0)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hasLive) {
                LivePulsingDot()
                Text(
                    text = "$liveCount live",
                    fontSize = 11.sp,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun LivePulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "live-dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live-alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFFEF4444).copy(alpha = alpha))
    )
}

@Composable
fun FixtureSectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (title == "LIVE") "LIVE" else if (title == "UPCOMING") "UPCOMING" else "FT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF1E1E1E),
            thickness = 1.dp
        )
    }
}

@Composable
fun FixtureCard(
    fixture: FixtureUiModel,
    isLive: Boolean,
    isUpcoming: Boolean = false,
    onMatchClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val containerColor = if (isLive) Color(0xFF2D1A1A) else Color(0xFF161616)
    val borderColor = if (isLive) Color(0xFFEF4444).copy(alpha = 0.3f) else Color(0xFF242424)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isLive || !isUpcoming) expanded = !expanded
                else onMatchClick(fixture.id.toIntOrNull() ?: 0)
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamColumn(
                    team = fixture.homeTeam,
                    score = fixture.homeScore,
                    status = fixture.status,
                    isWinner = fixture.homeScore != null && fixture.awayScore != null &&
                            fixture.homeScore > fixture.awayScore &&
                            (fixture.status == MatchStatusUi.COMPLETED || fixture.status == MatchStatusUi.LIVE),
                    modifier = Modifier.weight(1f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isUpcoming) {
                        Text(
                            text = fixture.kickoffTime ?: "--:--",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA0A0A0)
                        )
                    } else {
                        Text(
                            text = "${fixture.homeScore ?: 0} - ${fixture.awayScore ?: 0}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isLive && fixture.minute != null) {
                            Text(
                                text = "${fixture.minute}'",
                                fontSize = 10.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                TeamColumn(
                    team = fixture.awayTeam,
                    score = fixture.awayScore,
                    status = fixture.status,
                    isWinner = fixture.homeScore != null && fixture.awayScore != null &&
                            fixture.awayScore > fixture.homeScore &&
                            (fixture.status == MatchStatusUi.COMPLETED || fixture.status == MatchStatusUi.LIVE),
                    modifier = Modifier.weight(1f)
                )
            }

            if (!isUpcoming && fixture.goalEvents.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FixtureFooterStrip(fixture = fixture, isLive = isLive)
            }

            if (expanded && fixture.goalEvents.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF1E1E1E))
                Spacer(Modifier.height(8.dp))
                fixture.goalEvents.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = if (event.isHome) Arrangement.Start else Arrangement.End
                    ) {
                        Text(
                            text = "\u26BD ${event.minute}' ${event.playerName}",
                            fontSize = 11.sp,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamColumn(
    team: TeamUiModel,
    score: Int?,
    status: MatchStatusUi,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = team.logo,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = team.name,
            fontSize = 12.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color = if (isWinner) Color.White else Color(0xFF555555),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FixtureFooterStrip(fixture: FixtureUiModel, isLive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val goalScorers = fixture.goalEvents.take(4).joinToString(", ") {
            "${it.minute}' ${it.playerName}"
        }
        if (goalScorers.isNotEmpty()) {
            Text(
                text = "\u26BD $goalScorers",
                fontSize = 10.sp,
                color = Color(0xFFA0A0A0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (fixture.yellowCards > 0 || fixture.redCards > 0) {
                val parts = mutableListOf<String>()
                if (fixture.yellowCards > 0) parts.add("\uD83D\uDFE8 ${fixture.yellowCards}")
                if (fixture.redCards > 0) parts.add("\uD83D\uDFE5 ${fixture.redCards}")
                Text(
                    text = parts.joinToString("  "),
                    fontSize = 10.sp,
                    color = Color(0xFFA0A0A0)
                )
            }
            if (fixture.attendance != null) {
                Text(
                    text = "\uD83D\uDC65 ${fixture.attendance}",
                    fontSize = 10.sp,
                    color = Color(0xFF555555)
                )
            }
        }

        if (isLive) {
            val lastEvent = fixture.goalEvents.lastOrNull()
            if (lastEvent != null) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "\u26BD ${lastEvent.minute}'",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun FixtureCardShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f)))
            }
            Box(Modifier.width(40.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f)))
            }
        }
    }
}
