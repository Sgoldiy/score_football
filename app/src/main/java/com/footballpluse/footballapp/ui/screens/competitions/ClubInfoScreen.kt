package com.footballpluse.footballapp.ui.screens.competitions

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import com.footballpluse.footballapp.data.model.FixtureResponse
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.data.util.UiState
import com.footballpluse.footballapp.domain.model.*
import com.footballpluse.footballapp.ui.components.PlayerAvatar
import com.footballpluse.footballapp.viewmodel.ClubInfoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ScreenBackground = Color(0xFF0D0F14)
private val CardBackground = Color(0xFF131620)
private val PrimaryText = Color.White
private val SecondaryText = Color(0xFF8B949E)
private val BorderColor = Color(0xFF1F2430)
private val AccentGreen = Color(0xFF00E676)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubInfoScreen(
    teamId: Int,
    leagueId: Int,
    onBackClick: () -> Unit,
    onPlayerClick: (Int) -> Unit = {},
    onMatchClick: (Int) -> Unit = {},
    viewModel: ClubInfoViewModel = hiltViewModel()
) {
    val teamInfo by viewModel.teamInfo.collectAsStateWithLifecycle()
    val teamDetail by viewModel.teamDetail.collectAsStateWithLifecycle()
    val recentFixtures by viewModel.recentFixtures.collectAsStateWithLifecycle()

    LaunchedEffect(teamId, leagueId) {
        viewModel.loadClubData(teamId, leagueId)
    }

    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Squad", "Matches", "Transfers", "Stats & Info")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = when (val state = teamInfo) {
                        is UiState.Success -> state.data.team?.name ?: "Club Profile"
                        else -> "Club Profile"
                    }
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavoriteClub() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unfollow Club" else "Follow Club",
                            tint = if (isFavorite) AccentGreen else SecondaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
            )
        },
        containerColor = ScreenBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ScreenBackground)
        ) {
            // Hero Header
            when (val state = teamInfo) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                }
                is UiState.Success -> {
                    ClubHeroHeader(info = state.data)
                }
                is UiState.Error -> {
                    // Fallback to teamDetail info if available
                    if (teamDetail is ApiResult.Success) {
                        val d = (teamDetail as ApiResult.Success).data
                        ClubDetailHeader(detail = d)
                    }
                }
            }

            // Tab Navigation Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ScreenBackground,
                contentColor = AccentGreen,
                divider = { HorizontalDivider(color = BorderColor, thickness = 0.5.dp) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentGreen,
                        height = 2.5.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PrimaryText else SecondaryText
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Tab Content
            val detailData = (teamDetail as? ApiResult.Success)?.data

            when (selectedTab) {
                0 -> ClubSquadTab(
                    squad = detailData?.squad ?: emptyList(),
                    onPlayerClick = onPlayerClick
                )
                1 -> ClubMatchesTab(
                    fixtures = recentFixtures,
                    teamId = teamId,
                    onMatchClick = onMatchClick
                )
                2 -> ClubTransfersTab(
                    transfers = detailData?.transfers ?: emptyList(),
                    clubName = detailData?.info?.name ?: ""
                )
                3 -> ClubStatsTab(
                    detail = detailData
                )
            }
        }
    }
}

@Composable
private fun ClubHeroHeader(info: com.footballpluse.footballapp.data.model.TeamInfoResponse) {
    val team = info.team
    val venue = info.venue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(AccentGreen.copy(alpha = 0.12f), Color.Transparent)))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = team?.logo,
                contentDescription = team?.name,
                modifier = Modifier.size(44.dp),
                placeholder = painterResource(R.drawable.ic_team_placeholder),
                error = painterResource(R.drawable.ic_team_placeholder)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = team?.name ?: "Club",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = buildString {
                team?.country?.takeIf { it.isNotBlank() }?.let { append(it) }
                if (venue?.city != null && venue.city.isNotBlank()) {
                    if (isNotEmpty()) append(" \u00B7 ")
                    append(venue.city)
                }
                team?.founded?.let {
                    if (isNotEmpty()) append(" \u00B7 ")
                    append("Est. $it")
                }
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            venue?.name?.takeIf { it.isNotBlank() }?.let { stadium ->
                Text(
                    text = "\uD83C\uDFDF\uFE0F $stadium",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ClubDetailHeader(detail: TeamDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(AccentGreen.copy(alpha = 0.12f), Color.Transparent)))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = detail.info.logo,
                contentDescription = detail.info.name,
                modifier = Modifier.size(44.dp),
                placeholder = painterResource(R.drawable.ic_team_placeholder),
                error = painterResource(R.drawable.ic_team_placeholder)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.info.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            detail.info.country?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = SecondaryText
                )
            }
        }
    }
}

// ─── SQUAD TAB ────────────────────────────────────────────────────────────────

@Composable
private fun ClubSquadTab(
    squad: List<SquadMember>,
    onPlayerClick: (Int) -> Unit
) {
    if (squad.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No squad roster available", color = SecondaryText, fontSize = 14.sp)
        }
        return
    }

    val grouped = squad.groupBy { com.footballpluse.footballapp.data.mapper.normalizeSquadPosition(it.position) }
    val baseOrder = listOf("Goalkeeper", "Defender", "Midfielder", "Attacker")
    val extraPositions = (grouped.keys - baseOrder.toSet() - "Other").sorted()
    val order = baseOrder + extraPositions + if ("Other" in grouped) listOf("Other") else emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        order.forEach { positionKey ->
            val players = grouped[positionKey].orEmpty()
            if (players.isNotEmpty()) {
                val positionColor = when (positionKey) {
                    "Goalkeeper" -> Color(0xFFF59E0B)
                    "Defender" -> Color(0xFF3B82F6)
                    "Midfielder" -> Color(0xFF10B981)
                    "Attacker" -> Color(0xFFEF4444)
                    else -> AccentGreen
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(positionColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (positionKey) {
                                "Goalkeeper" -> "GOALKEEPERS"
                                "Defender" -> "DEFENDERS"
                                "Midfielder" -> "MIDFIELDERS"
                                "Attacker" -> "FORWARDS"
                                else -> positionKey.uppercase()
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(positionColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${players.size}",
                                fontSize = 10.sp,
                                color = positionColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(players) { p ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(0.5.dp, BorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(p.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(46.dp)) {
                                PlayerAvatar(
                                    url = p.photo,
                                    name = p.name,
                                    ringColor = positionColor.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .align(Alignment.TopStart)
                                )
                                p.number?.let { number ->
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(positionColor)
                                            .border(1.dp, CardBackground, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = number.toString(),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = p.name,
                                    color = PrimaryText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = p.position ?: positionKey,
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                tint = SecondaryText.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── MATCHES TAB ──────────────────────────────────────────────────────────────

@Composable
private fun ClubMatchesTab(
    fixtures: UiState<List<FixtureResponse>>,
    teamId: Int,
    onMatchClick: (Int) -> Unit
) {
    when (fixtures) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGreen)
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Could not load fixtures", color = SecondaryText)
            }
        }
        is UiState.Success -> {
            val list = fixtures.data
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matches found", color = SecondaryText)
                }
                return
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list) { fixture ->
                    ClubMatchCard(
                        fixture = fixture,
                        teamId = teamId,
                        onClick = {
                            fixture.fixture?.id?.let { onMatchClick(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClubMatchCard(
    fixture: FixtureResponse,
    teamId: Int,
    onClick: () -> Unit
) {
    val homeTeam = fixture.teams?.home
    val awayTeam = fixture.teams?.away
    val isHome = homeTeam?.id == teamId
    val homeGoals = fixture.goals?.home
    val awayGoals = fixture.goals?.away

    val outcomeColor = when {
        homeGoals == null || awayGoals == null -> Color.Transparent
        isHome && homeGoals > awayGoals -> Color(0xFF10B981)
        !isHome && awayGoals > homeGoals -> Color(0xFF10B981)
        homeGoals == awayGoals -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Outcome indicator bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(outcomeColor)
            )
            Spacer(Modifier.width(10.dp))

            // Teams & Logos
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = homeTeam?.logo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = homeTeam?.name ?: "Home",
                        fontSize = 13.sp,
                        fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = awayTeam?.logo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = awayTeam?.name ?: "Away",
                        fontSize = 13.sp,
                        fontWeight = if (!isHome) FontWeight.Bold else FontWeight.Normal,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Score or Date
            Column(horizontalAlignment = Alignment.End) {
                if (homeGoals != null && awayGoals != null) {
                    Text(
                        text = "$homeGoals - $awayGoals",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    val statusText = fixture.fixture?.status?.short ?: "FT"
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        color = if (statusText in listOf("1H", "2H", "LIVE", "HT")) AccentGreen else SecondaryText
                    )
                } else {
                    val dateFormatted = remember(fixture.fixture?.date) {
                        formatShortDate(fixture.fixture?.date)
                    }
                    Text(
                        text = dateFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentGreen
                    )
                }
            }
        }
    }
}

// ─── TRANSFERS TAB ────────────────────────────────────────────────────────────

@Composable
private fun ClubTransfersTab(
    transfers: List<TransferRecord>,
    clubName: String
) {
    var mode by remember { mutableIntStateOf(0) }
    val incoming = transfers.filter { it.teamIn.equals(clubName, ignoreCase = true) }
    val outgoing = transfers.filter { it.teamOut.equals(clubName, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { mode = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == 0) AccentGreen.copy(alpha = 0.2f) else CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, if (mode == 0) AccentGreen else BorderColor)
            ) {
                Text("Arrivals", color = if (mode == 0) AccentGreen else SecondaryText, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { mode = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == 1) Color(0xFFEF4444).copy(alpha = 0.2f) else CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, if (mode == 1) Color(0xFFEF4444) else BorderColor)
            ) {
                Text("Departures", color = if (mode == 1) Color(0xFFEF4444) else SecondaryText, fontWeight = FontWeight.Bold)
            }
        }

        val list = if (mode == 0) incoming else outgoing
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (mode == 0) "No confirmed arrivals recorded" else "No confirmed departures recorded",
                    color = SecondaryText,
                    fontSize = 13.sp
                )
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(list) { t ->
                val accentColor = if (mode == 0) Color(0xFF10B981) else Color(0xFFEF4444)
                val priceVal = when {
                    t.type.contains("€") || t.type.contains("£") || t.type.contains("M") -> t.type
                    t.type.lowercase().contains("free") -> "Free"
                    t.type.lowercase().contains("loan") -> "Loan"
                    else -> "Free"
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(0.5.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerAvatar(
                            url = t.playerPhotoUrl,
                            name = t.player,
                            ringColor = accentColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.player, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (mode == 0) "From: " else "To: ",
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (mode == 0) t.teamOut else t.teamIn,
                                    color = PrimaryText.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(priceVal, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(t.date, color = SecondaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── STATS & INFO TAB ─────────────────────────────────────────────────────────

@Composable
private fun ClubStatsTab(detail: TeamDetail?) {
    if (detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentGreen)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Head Coach Card
        detail.coaches.firstOrNull()?.let { coach ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerAvatar(
                            url = coach.photo,
                            name = coach.name,
                            ringColor = AccentGreen.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Head Coach", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(coach.name, color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Trophy Cabinet
        if (detail.trophies.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Trophy Honours", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        detail.trophies.forEachIndexed { idx, trophy ->
                            if (idx > 0) {
                                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trophy.league, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    trophy.seasons?.takeIf { it.isNotBlank() }?.let {
                                        Text(it.trimEnd(','), color = SecondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val label = if (trophy.count > 1) "${trophy.status} x${trophy.count}" else trophy.status
                                    Text(label, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Performance Metrics Card
        item {
            val td = detail.stats
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Season Record", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ClubStatPill("Played", "${td?.played ?: "-"}")
                        ClubStatPill("Wins", "${td?.wins ?: "-"}", Color(0xFF10B981))
                        ClubStatPill("Draws", "${td?.draws ?: "-"}", Color(0xFFF59E0B))
                        ClubStatPill("Losses", "${td?.loses ?: "-"}", Color(0xFFEF4444))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ClubStatPill("GF", "${td?.goalsFor ?: "-"}")
                        ClubStatPill("GA", "${td?.goalsAgainst ?: "-"}")
                        val diff = if (td != null) td.goalsFor - td.goalsAgainst else 0
                        ClubStatPill("GD", if (diff >= 0) "+$diff" else "$diff", if (diff >= 0) AccentGreen else Color(0xFFEF4444))
                    }
                }
            }
        }

        // Stadium Spec Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Stadium & Facility", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    ClubDetailRow(label = "Stadium Name", value = detail.venue?.name ?: "N/A")
                    detail.venue?.city?.takeIf { it.isNotBlank() }?.let {
                        ClubDetailRow(label = "City", value = it)
                    }
                    detail.venue?.capacity?.let { cap ->
                        ClubDetailRow(label = "Capacity", value = String.format(Locale.US, "%,d seats", cap))
                    }
                    detail.venue?.surface?.takeIf { it.isNotBlank() }?.let {
                        ClubDetailRow(label = "Pitch Surface", value = it.replaceFirstChar { c -> c.uppercase() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubStatPill(label: String, value: String, valueColor: Color = PrimaryText) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(label, fontSize = 10.sp, color = SecondaryText)
    }
}

@Composable
private fun ClubDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SecondaryText, fontSize = 12.sp)
        Text(value, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatShortDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    if (iso.length == 10) {
        val d = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
        }.getOrNull() ?: return ""
        return formatter.format(d)
    }
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val date: Date = runCatching { parser.parse(iso) }.getOrNull()
        ?: runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.take(10)) }.getOrNull()
        ?: return ""
    return formatter.format(date)
}
