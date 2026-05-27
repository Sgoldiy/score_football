package com.example.footballapp.ui.screens.competitions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
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
import com.example.footballapp.data.model.*
import com.example.footballapp.data.util.UiState
import com.example.footballapp.ui.components.SectionHeader
import com.example.footballapp.ui.theme.*
import com.example.footballapp.viewmodel.ClubInfoViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubInfoScreen(
    teamId: Int,
    leagueId: Int,
    onBackClick: () -> Unit,
    viewModel: ClubInfoViewModel = hiltViewModel()
) {
    val teamInfo by viewModel.teamInfo.collectAsStateWithLifecycle()
    val teamStats by viewModel.teamStats.collectAsStateWithLifecycle()
    val squad by viewModel.squad.collectAsStateWithLifecycle()
    val coach by viewModel.coach.collectAsStateWithLifecycle()
    val recentFixtures by viewModel.recentFixtures.collectAsStateWithLifecycle()
    val topScorers by viewModel.topScorers.collectAsStateWithLifecycle()

    LaunchedEffect(teamId) {
        viewModel.loadClubData(teamId, leagueId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = (teamInfo as? UiState.Success)?.data?.team?.name ?: "Club Info"
                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        containerColor = PitchBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Team Header
            item {
                when (val info = teamInfo) {
                    is UiState.Loading -> LoadingBox(height = 200)
                    is UiState.Error -> ErrorBox(info.message)
                    is UiState.Success -> TeamHeaderSection(info.data)
                }
            }

            // B. Stats Row
            item {
                when (val stats = teamStats) {
                    is UiState.Loading -> LoadingBox(height = 100)
                    is UiState.Error -> ErrorBox(stats.message)
                    is UiState.Success -> StatsRowSection(stats.data)
                }
            }

            // C. Coach
            item {
                SectionHeader("Coach")
                when (val coachState = coach) {
                    is UiState.Loading -> LoadingBox(height = 80)
                    is UiState.Error -> ErrorBox(coachState.message)
                    is UiState.Success -> CoachSection(coachState.data)
                }
            }

            // D. Squad
            item {
                SectionHeader("Squad")
                when (val squadState = squad) {
                    is UiState.Loading -> LoadingBox(height = 120)
                    is UiState.Error -> ErrorBox(squadState.message)
                    is UiState.Success -> SquadSection(squadState.data)
                }
            }

            // E. Recent Fixtures
            item {
                SectionHeader("Recent Fixtures")
                when (val fixtures = recentFixtures) {
                    is UiState.Loading -> LoadingBox(height = 200)
                    is UiState.Error -> ErrorBox(fixtures.message)
                    is UiState.Success -> RecentFixturesSection(fixtures.data, teamId)
                }
            }

            // F. Top Scorers
            item {
                SectionHeader("Top Scorers")
                when (val scorers = topScorers) {
                    is UiState.Loading -> LoadingBox(height = 200)
                    is UiState.Error -> ErrorBox(scorers.message)
                    is UiState.Success -> TopScorersSection(scorers.data)
                }
            }
        }
    }
}

@Composable
private fun TeamHeaderSection(info: TeamInfoResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = info.team?.logo,
                contentDescription = info.team?.name,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = info.team?.name ?: "",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(4.dp))
        val details = mutableListOf<String>()
        info.team?.country?.let { details.add(it) }
        info.team?.founded?.let { details.add("Founded $it") }
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString(" · "),
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        if (info.venue?.name != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(info.venue.name)
                    if (info.venue.city != null) append(", ${info.venue.city}")
                },
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StatsRowSection(stats: TeamStatistics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Played", stats.fixtures?.played?.total?.toString() ?: "-")
        StatCard("Wins", stats.fixtures?.wins?.total?.toString() ?: "-")
        StatCard("Goals For", stats.goals?.goalsFor?.total?.total?.toString() ?: "-")
        StatCard("Goals Against", stats.goals?.against?.total?.total?.toString() ?: "-")
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CoachSection(coaches: List<Coach>) {
    val activeCoach = coaches.firstOrNull()
    if (activeCoach == null) {
        Text(
            "No coach info available",
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = activeCoach.photo,
                contentDescription = activeCoach.name,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = activeCoach.name ?: "Unknown",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            activeCoach.nationality?.let {
                Text(text = it, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SquadSection(squadResponse: List<SquadResponse>) {
    val players = squadResponse.flatMap { it.players ?: emptyList() }
    if (players.isEmpty()) {
        Text(
            "No squad data available",
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(players, key = { it.id ?: it.hashCode() }) { player ->
            PlayerCard(player)
        }
    }
}

@Composable
private fun PlayerCard(player: SquadPlayer) {
    val positionBadgeColor = when (player.position?.take(3)?.uppercase()) {
        "GK" -> Color(0xFFD97706)
        "DEF" -> Color(0xFF2563EB)
        "MID" -> Color(0xFF059669)
        "ATT" -> Color(0xFFDC2626)
        else -> Color(0xFF6B7280)
    }
    Card(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = player.photo,
                    contentDescription = player.name,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = player.name ?: "",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(positionBadgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = player.position?.take(3)?.uppercase() ?: "N/A",
                    color = positionBadgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            player.age?.let { age ->
                Text(
                    text = "$age yrs",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RecentFixturesSection(fixtures: List<FixtureResponse>, teamId: Int) {
    if (fixtures.isEmpty()) {
        Text(
            "No recent fixtures",
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fixtures.forEach { fixture ->
            FixtureCard(fixture, teamId)
        }
    }
}

@Composable
private fun FixtureCard(fixture: FixtureResponse, teamId: Int) {
    val homeId = fixture.teams?.home?.id
    val awayId = fixture.teams?.away?.id
    val homeScore = fixture.goals?.home
    val awayScore = fixture.goals?.away
    val isTeamHome = homeId == teamId
    val isTeamAway = awayId == teamId

    val resultBadge: String?
    val resultColor: Color
    if (homeScore != null && awayScore != null && fixture.fixture?.status?.short != "NS") {
        val teamScore = if (isTeamHome) homeScore else awayScore
        val opponentScore = if (isTeamHome) awayScore else homeScore
        when {
            teamScore > opponentScore -> { resultBadge = "W"; resultColor = Color(0xFF16A34A) }
            teamScore < opponentScore -> { resultBadge = "L"; resultColor = Color(0xFFDC2626) }
            else -> { resultBadge = "D"; resultColor = Color(0xFFD97706) }
        }
    } else {
        resultBadge = null
        resultColor = Color.Transparent
    }

    val statusText = when (fixture.fixture?.status?.short) {
        "FT", "AET", "PEN" -> "FT"
        "NS" -> "NS"
        in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE") -> "LIVE"
        else -> fixture.fixture?.status?.short ?: ""
    }

    val dateText = fixture.fixture?.date?.let { rawDate ->
        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val cleaned = rawDate.substringBefore("+").substringBefore("Z")
            val parsed = fmt.parse(cleaned)
            if (parsed != null) {
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(parsed)
            } else {
                rawDate.take(10)
            }
        } catch (e: Exception) {
            rawDate.take(10)
        }
    } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(50.dp)) {
                Text(
                    text = dateText,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = statusText,
                    color = if (statusText == "LIVE") LiveGreen else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = fixture.teams?.home?.logo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = fixture.teams?.home?.name ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isTeamHome) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = fixture.teams?.away?.logo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = fixture.teams?.away?.name ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isTeamAway) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (homeScore != null && awayScore != null && statusText != "NS") {
                Text(
                    text = "$homeScore - $awayScore",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            } else {
                Text(
                    text = "vs",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
            if (resultBadge != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(resultColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resultBadge,
                        color = resultColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopScorersSection(scorers: List<PlayerProfileStatisticsResponse>) {
    if (scorers.isEmpty()) {
        Text(
            "No top scorer data available",
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        scorers.forEachIndexed { index, scorer ->
            val stats = scorer.statistics?.firstOrNull()
            TopScorerRow(
                rank = index + 1,
                name = scorer.player?.name ?: "Player",
                goals = stats?.goals?.total ?: 0,
                assists = stats?.goals?.assists ?: 0,
                clubName = stats?.team?.name ?: ""
            )
        }
    }
}

@Composable
private fun TopScorerRow(
    rank: Int,
    name: String,
    goals: Int,
    assists: Int,
    clubName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank",
                color = LiveGreen,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = clubName,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$goals",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "goals",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$assists",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "assists",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingBox(height: Int = 100) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun ErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
