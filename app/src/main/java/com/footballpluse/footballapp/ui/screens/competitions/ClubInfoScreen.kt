package com.footballpluse.footballapp.ui.screens.competitions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.data.model.Coach
import com.footballpluse.footballapp.data.model.FixtureResponse
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.model.SquadPlayer
import com.footballpluse.footballapp.data.model.SquadResponse
import com.footballpluse.footballapp.data.model.TeamInfoResponse
import com.footballpluse.footballapp.data.model.TeamStatistics
import com.footballpluse.footballapp.data.util.UiState
import com.footballpluse.footballapp.viewmodel.ClubInfoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ScreenBackground = Color(0xFF0A0F1E)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryText = Color.White
private val SecondaryText = Color(0xFF94A3B8)

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
    val coaches by viewModel.coach.collectAsStateWithLifecycle()
    val recentFixtures by viewModel.recentFixtures.collectAsStateWithLifecycle()
    val topScorers by viewModel.topScorers.collectAsStateWithLifecycle()

    LaunchedEffect(teamId, leagueId) {
        viewModel.loadClubData(teamId, leagueId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            when (val state = teamInfo) {
                is UiState.Loading -> SectionLoading(height = 220.dp)
                is UiState.Error -> SectionError(text = "Could not load data")
                is UiState.Success -> HeroHeader(
                    info = state.data,
                    onBackClick = onBackClick
                )
            }
        }

        item {
            when (val state = teamStats) {
                is UiState.Loading -> SectionLoading(height = 92.dp)
                is UiState.Error -> SectionError(text = "Could not load data")
                is UiState.Success -> SeasonStatsRow(stats = state.data)
            }
        }

        item {
            when (val state = coaches) {
                is UiState.Loading -> SectionLoading(height = 88.dp)
                is UiState.Error -> SectionError(text = "Could not load data")
                is UiState.Success -> CoachCard(coaches = state.data)
            }
        }

        item {
            SectionTitle(text = "Squad")
            when (val state = squad) {
                is UiState.Loading -> SectionLoading(height = 160.dp)
                is UiState.Error -> SectionError(text = "Could not load data")
                is UiState.Success -> SquadRow(squad = state.data)
            }
        }

        item {
            SectionTitle(text = "Recent Matches")
            when (val state = recentFixtures) {
                is UiState.Loading -> SectionLoading(height = 220.dp)
                is UiState.Error -> SectionError(text = "Could not load data")
                is UiState.Success -> RecentFixtures(
                    fixtures = state.data,
                    teamId = teamId
                )
            }
        }

        item {
            SectionTitle(text = "Top Scorers — ${seasonLabel(2025)}")
            when (val state = topScorers) {
                is UiState.Loading -> SectionLoading(height = 240.dp)
                is UiState.Error -> SectionError(text = "Could not load data")
                is UiState.Success -> TopScorersList(scorers = state.data.take(5))
            }
        }
    }
}

@Composable
private fun HeroHeader(
    info: TeamInfoResponse,
    onBackClick: () -> Unit
) {
    val team = info.team
    val venue = info.venue
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PrimaryText)
            }
            Text(
                text = "Club Info",
                modifier = Modifier.weight(1f),
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = team?.logo,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = team?.name ?: "Club",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val city = venue?.city
            val founded = team?.founded
            Text(
                text = buildString {
                    append(city ?: "")
                    if (!city.isNullOrBlank() && founded != null) append(" • ")
                    if (founded != null) append("Est. $founded")
                }.ifBlank { " " },
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp
            )
            Text(
                text = venue?.name ?: "",
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeasonStatsRow(stats: TeamStatistics) {
    val played = stats.fixtures?.played?.total ?: 0
    val wins = stats.fixtures?.wins?.total ?: 0
    val goalsFor = stats.goals?.goalsFor?.total?.total ?: 0
    val goalsAgainst = stats.goals?.against?.total?.total ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatMiniCard(value = played, label = "Played", accentColor = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
        StatMiniCard(value = wins, label = "Wins", accentColor = Color(0xFF10B981), modifier = Modifier.weight(1f))
        StatMiniCard(value = goalsFor, label = "Goals For", accentColor = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
        StatMiniCard(value = goalsAgainst, label = "Goals Against", accentColor = Color(0xFFEF4444), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatMiniCard(
    value: Int,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun CoachCard(coaches: List<Coach>) {
    val coach = remember(coaches) {
        coaches.firstOrNull { it.career?.lastOrNull()?.end == null } ?: coaches.firstOrNull()
    }

    if (coach == null) {
        SectionError(text = "Could not load data")
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coach.photo,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(CircleShape),
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Manager", color = SecondaryText, fontSize = 11.sp)
                Text(
                    text = coach.name ?: "Coach",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = coach.nationality ?: "",
                    color = Color(0xFFD1D5DB),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SquadRow(squad: List<SquadResponse>) {
    val players: List<SquadPlayer> = remember(squad) {
        squad.flatMap { it.players ?: emptyList() }
    }

    if (players.isEmpty()) {
        SectionError(text = "Could not load data")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(players, key = { it.id ?: it.name ?: "" }) { player ->
            SquadPlayerCard(player = player)
        }
    }
}

@Composable
private fun SquadPlayerCard(player: SquadPlayer) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(140.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = player.photo,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = player.name ?: "Player",
                fontSize = 11.sp,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))

            val (badgeColor, posLabel) = when (player.position) {
                "Goalkeeper" -> Color(0xFFF59E0B) to "GK"
                "Defender" -> Color(0xFF3B82F6) to "DEF"
                "Midfielder" -> Color(0xFF10B981) to "MID"
                else -> Color(0xFFEF4444) to "ATT"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = posLabel,
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RecentFixtures(
    fixtures: List<FixtureResponse>,
    teamId: Int
) {
    val recent = remember(fixtures, teamId) {
        fixtures
            .filter { it.fixture?.status?.short == "FT" }
            .sortedByDescending { it.fixture?.timestamp ?: 0L }
            .take(5)
    }

    if (recent.isEmpty()) {
        SectionError(text = "Could not load data")
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        recent.forEach { fixture ->
            RecentFixtureRow(fixture = fixture, teamId = teamId)
            Divider(color = PrimaryText.copy(alpha = 0.07f))
        }
    }
}

@Composable
private fun RecentFixtureRow(
    fixture: FixtureResponse,
    teamId: Int
) {
    val isHome = fixture.teams?.home?.id == teamId
    val teamGoals = if (isHome) fixture.goals?.home ?: 0 else fixture.goals?.away ?: 0
    val oppGoals = if (isHome) fixture.goals?.away ?: 0 else fixture.goals?.home ?: 0
    val opponent = if (isHome) fixture.teams?.away?.name else fixture.teams?.home?.name
    val oppLogo = if (isHome) fixture.teams?.away?.logo else fixture.teams?.home?.logo

    val result = when {
        teamGoals > oppGoals -> "W"
        teamGoals == oppGoals -> "D"
        else -> "L"
    }
    val resultColor = when (result) {
        "W" -> Color(0xFF10B981)
        "D" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val formattedDate = remember(fixture.fixture?.date) { formatShortDate(fixture.fixture?.date) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(resultColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = result, color = resultColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        AsyncImage(
            model = oppLogo,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            placeholder = painterResource(R.drawable.ic_launcher_foreground),
            error = painterResource(R.drawable.ic_launcher_foreground)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isHome) "vs ${opponent ?: ""}" else "@ ${opponent ?: ""}",
                color = PrimaryText,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = formattedDate, color = SecondaryText, fontSize = 12.sp)
        }
        Text(
            text = "$teamGoals - $oppGoals",
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun TopScorersList(scorers: List<PlayerProfileStatisticsResponse>) {
    if (scorers.isEmpty()) {
        SectionError(text = "Could not load data")
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        scorers.forEachIndexed { index, scorer ->
            TopScorerRow(index = index, scorer = scorer)
            Divider(color = PrimaryText.copy(alpha = 0.07f))
        }
    }
}

@Composable
private fun TopScorerRow(
    index: Int,
    scorer: PlayerProfileStatisticsResponse
) {
    val goals = scorer.statistics?.firstOrNull()?.goals?.total ?: 0
    val teamName = scorer.statistics?.firstOrNull()?.team?.name ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}",
            color = SecondaryText,
            fontSize = 14.sp,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scorer.player?.name ?: "Player",
                color = PrimaryText,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = teamName,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$goals goals",
                color = Color(0xFF10B981),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryText,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SectionLoading(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF3B82F6))
    }
}

@Composable
private fun SectionError(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun seasonLabel(season: Int): String {
    return if (season == 2026) {
        "2026"
    } else {
        "${season}/${(season + 1).toString().takeLast(2)}"
    }
}

private fun formatShortDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""

    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())

    val date: Date = runCatching { parser.parse(iso) }.getOrNull() ?: return ""
    return formatter.format(date)
}
