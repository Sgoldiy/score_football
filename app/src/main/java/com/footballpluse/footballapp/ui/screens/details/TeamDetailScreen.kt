package com.footballpluse.footballapp.ui.screens.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.domain.model.*
import com.footballpluse.footballapp.ui.components.PlayerAvatar
import com.footballpluse.footballapp.ui.theme.*
import com.footballpluse.footballapp.viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamId: Int,
    leagueId: Int,
    season: Int,
    onBackClick: () -> Unit,
    onPlayerClick: (Int) -> Unit,
    viewModel: TeamViewModel = hiltViewModel()
) {
    LaunchedEffect(teamId, leagueId, season) {
        viewModel.loadTeamData(teamId, leagueId, season)
    }

    val state by viewModel.teamState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val name = if (state is ApiResult.Success) (state as ApiResult.Success).data.info.name else "Club Profile"
                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PitchBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PitchBlack)
        ) {
            when (val result = state) {
                is ApiResult.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GlassGlowGreen)
                    }
                }
                is ApiResult.Error -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(result.message, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        }
                    }
                }
                is ApiResult.Success -> {
                    val detail = result.data
                    TeamDetailContent(detail = detail, onPlayerClick = onPlayerClick, season = season)
                }
            }
        }
    }
}

@Composable
private fun TeamDetailContent(
    detail: TeamDetail,
    onPlayerClick: (Int) -> Unit,
    season: Int
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Squad", "Transfers", "Stats & Info")

    Column(modifier = Modifier.fillMaxSize()) {
        // Club Header Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(GlassGlowGreen.copy(alpha = 0.15f), Color.Transparent)))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = detail.info.logo,
                    contentDescription = detail.info.name,
                    modifier = Modifier.size(48.dp),
                    placeholder = androidx.compose.ui.res.painterResource(com.footballpluse.footballapp.R.drawable.ic_team_placeholder),
                    error = androidx.compose.ui.res.painterResource(com.footballpluse.footballapp.R.drawable.ic_team_placeholder)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = detail.info.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "Season: ${SeasonUtils.displaySeasonLabel(season)}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
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
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> TeamSquadTab(squad = detail.squad, onPlayerClick = onPlayerClick)
            1 -> TeamTransfersTab(transfers = detail.transfers, clubName = detail.info.name)
            2 -> TeamInfoTab(detail = detail)
        }
    }
}

@Composable
private fun TeamSquadTab(squad: List<SquadMember>, onPlayerClick: (Int) -> Unit) {
    if (squad.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No squad roster found", color = Color.White.copy(alpha = 0.4f))
        }
        return
    }

    val grouped = squad.groupBy { com.footballpluse.footballapp.data.mapper.normalizeSquadPosition(it.position) }
    val baseOrder = listOf("Goalkeeper", "Defender", "Midfielder", "Attacker")
    val extraPositions = (grouped.keys - baseOrder.toSet() - "Other").sorted()
    val order = baseOrder + extraPositions + if ("Other" in grouped) listOf("Other") else emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        order.forEach { key ->
            val players = grouped[key].orEmpty()
            if (players.isNotEmpty()) {
                val positionColor = when (key) {
                    "Goalkeeper" -> Color(0xFFB0BEC5)
                    "Defender" -> Color(0xFF81C784)
                    "Midfielder" -> Color(0xFF64B5F6)
                    "Attacker" -> Color(0xFFFFD54F)
                    else -> GlassGlowGreen
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(positionColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (key) {
                                "Goalkeeper" -> "GOALKEEPERS"
                                "Defender" -> "DEFENDERS"
                                "Midfielder" -> "MIDFIELDERS"
                                "Attacker" -> "FORWARDS"
                                else -> key.uppercase()
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
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
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                items(players) { p ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(p.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(50.dp)) {
                                PlayerAvatar(
                                    url = p.photo,
                                    name = p.name,
                                    ringColor = positionColor.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .align(Alignment.TopStart)
                                )
                                p.number?.let { number ->
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(positionColor)
                                            .border(1.dp, PitchSurface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = number.toString(),
                                            color = PitchBlack,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                Text(text = p.position ?: "", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamTransfersTab(transfers: List<TransferRecord>, clubName: String) {
    var mode by remember { mutableIntStateOf(0) }
    val incoming = transfers.filter { it.teamIn.equals(clubName, ignoreCase = true) }
    val outgoing = transfers.filter { it.teamOut.equals(clubName, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = { mode = 0 },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (mode == 0) LiveGreen.copy(alpha = 0.15f) else DarkCard,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Text("Arrivals", color = if (mode == 0) LiveGreen else Color.White, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { mode = 1 },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (mode == 1) DangerRed.copy(alpha = 0.15f) else DarkCard,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Text("Departures", color = if (mode == 1) DangerRed else Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))

        val list = if (mode == 0) incoming else outgoing
        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (mode == 0) "No recent arrivals" else "No recent departures",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(list) { t ->
                val accentColor = if (mode == 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                val priceVal = when {
                    t.type.contains("€") || t.type.contains("£") || t.type.contains("M") -> t.type
                    t.type.lowercase().contains("free") -> "Free"
                    t.type.lowercase().contains("loan") -> "Loan"
                    else -> "Free"
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(50.dp)) {
                            PlayerAvatar(
                                url = t.playerPhotoUrl,
                                name = t.player,
                                ringColor = accentColor.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.TopStart)
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .border(1.dp, PitchSurface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (mode == 0) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                    tint = PitchBlack,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.player, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (mode == 0) "From " else "To ",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                                val destinationLogo = if (mode == 0) t.teamOutLogoUrl else t.teamInLogoUrl
                                if (destinationLogo != null) {
                                    AsyncImage(model = destinationLogo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (mode == 0) t.teamOut else t.teamIn,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(priceVal, color = accentColor, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(t.date, color = Color.White.copy(alpha = 0.40f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamInfoTab(detail: TeamDetail) {
    val td = detail.stats

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Trophy Cabinet
        if (detail.trophies.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Trophy Cabinet & Honours", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Official titles and achievements", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        detail.trophies.forEachIndexed { idx, trophy ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = Color.White.copy(alpha = 0.06f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trophy.league,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    trophy.seasons?.takeIf { it.isNotBlank() }?.let { seasons ->
                                        Text(
                                            text = seasons.trimEnd(','),
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (trophy.status.equals("Winner", ignoreCase = true))
                                                Color(0xFFFFD700).copy(alpha = 0.15f)
                                            else
                                                Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            0.5.dp,
                                            if (trophy.status.equals("Winner", ignoreCase = true))
                                                Color(0xFFFFD700).copy(alpha = 0.40f)
                                            else
                                                Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    val badgeLabel = if (trophy.count > 1) "${trophy.status} x${trophy.count}" else trophy.status
                                    Text(
                                        text = badgeLabel,
                                        color = if (trophy.status.equals("Winner", ignoreCase = true)) Color(0xFFFFD700) else Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Stadium Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Stadium, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Club Stadium Spec", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    detail.venue?.image?.let { img ->
                        AsyncImage(
                            model = img,
                            contentDescription = "Stadium",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    detail.info.founded?.takeIf { it.isNotBlank() }?.let { founded ->
                        DetailRow(icon = Icons.Rounded.CalendarToday, label = "Founded", value = "Est. $founded")
                    }
                    val teamCountry = detail.info.country?.takeIf { it.isNotBlank() }
                        ?: com.footballpluse.footballapp.data.remote.uitslagen.UitslagenLeagues.countryFor(null, detail.info.name)
                    DetailRow(icon = Icons.Rounded.Public, label = "Country", value = teamCountry)
                    DetailRow(icon = Icons.Rounded.Stadium, label = "Venue", value = detail.venue?.name ?: "-")
                    detail.venue?.city?.takeIf { it.isNotBlank() }?.let { city ->
                        DetailRow(icon = Icons.Rounded.Place, label = "City", value = city)
                    }
                    detail.venue?.address?.takeIf { it.isNotBlank() }?.let { address ->
                        DetailRow(icon = Icons.Rounded.LocationOn, label = "Address", value = address)
                    }
                    detail.venue?.surface?.takeIf { it.isNotBlank() }?.let { surface ->
                        DetailRow(icon = Icons.Rounded.Grass, label = "Surface", value = surface.replaceFirstChar { it.uppercase() })
                    }
                    detail.venue?.capacity?.let { capacity ->
                        DetailRow(icon = Icons.Rounded.Groups, label = "Capacity", value = String.format(java.util.Locale.US, "%,d seats", capacity))
                        Spacer(Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Arena Size", color = TextSecondary, fontSize = 10.sp)
                                Text("${capacity / 1000}K seats", color = GlassGlowGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            val progressRatio = (capacity / 100000f).coerceIn(0.1f, 1.0f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressRatio)
                                        .background(Brush.horizontalGradient(listOf(GlassGlowGreen.copy(alpha = 0.7f), GlassGlowGreen)))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stats Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BarChart, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Performance & Stats", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = "Matches", value = "${td?.played ?: "-"}")
                        StatItem(label = "Wins", value = "${td?.wins ?: "-"}", valueColor = Color(0xFF4CAF50))
                        StatItem(label = "Draws", value = "${td?.draws ?: "-"}", valueColor = Color(0xFFFFC107))
                        StatItem(label = "Losses", value = "${td?.loses ?: "-"}", valueColor = Color(0xFFF44336))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = "Goals For", value = "${td?.goalsFor ?: "-"}")
                        StatItem(label = "Goals Against", value = "${td?.goalsAgainst ?: "-"}")
                        val gd = if (td != null) td.goalsFor - td.goalsAgainst else 0
                        StatItem(
                            label = "Goal Diff",
                            value = if (td != null) (if (gd >= 0) "+$gd" else "$gd") else "-",
                            valueColor = if (gd >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }

                    val dStats = detail.detailedStats
                    if (dStats != null && (dStats.cleanSheetTotal > 0 || dStats.winHome > 0 || dStats.winAway > 0 || dStats.avgGoalsScored != null)) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        Spacer(Modifier.height(14.dp))
                        Text("Record & Efficiency Splits", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))

                        DetailRow(
                            icon = Icons.Rounded.Home,
                            label = "Home Record",
                            value = "${dStats.winHome}W • ${dStats.drawHome}D • ${dStats.lostHome}L"
                        )
                        DetailRow(
                            icon = Icons.Rounded.FlightTakeoff,
                            label = "Away Record",
                            value = "${dStats.winAway}W • ${dStats.drawAway}D • ${dStats.lostAway}L"
                        )
                        if (dStats.cleanSheetTotal > 0) {
                            DetailRow(
                                icon = Icons.Rounded.Shield,
                                label = "Clean Sheets",
                                value = "${dStats.cleanSheetTotal} matches"
                            )
                        }
                        dStats.avgGoalsScored?.let { avgScored ->
                            DetailRow(
                                icon = Icons.Rounded.SportsSoccer,
                                label = "Avg Scored",
                                value = "$avgScored / match"
                            )
                        }
                    }
                }
            }
        }

        // Coach Card
        if (detail.coaches.isNotEmpty()) {
            item {
                val coach = detail.coaches.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(model = coach.photo, contentDescription = coach.name, modifier = Modifier.size(36.dp).clip(CircleShape))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("CHIEF TACTICIAN", color = GlassGlowGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(coach.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}
