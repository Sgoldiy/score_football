package com.footballpluse.footballapp.ui.screens.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

import com.footballpluse.footballapp.ui.components.HeaderIcon
import com.footballpluse.footballapp.ui.components.PlayerAvatar
import com.footballpluse.footballapp.domain.model.FavouriteClub
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.domain.model.OnboardingDefaults
import com.footballpluse.footballapp.ui.theme.GlassGlowGreen
import com.footballpluse.footballapp.ui.theme.PitchBlack
import com.footballpluse.footballapp.ui.theme.PitchSurface
import com.footballpluse.footballapp.ui.theme.PitchSurfaceHigh
import com.footballpluse.footballapp.ui.theme.TextSecondary
import androidx.compose.animation.core.*

@Composable
fun FavoritesScreen(
    onSearchClick: () -> Unit,
    onAddClubs: () -> Unit,
    onPlayerClick: (Int) -> Unit,
    onMatchClick: (Int) -> Unit,
    onTeamClick: (Int, Int) -> Unit,
    viewModel: FavouriteClubsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PitchBlack, PitchSurfaceHigh)))
    ) {
        TopBar(onSearchClick = onSearchClick, onRefresh = { viewModel.retry() })

        if (state.clubs.isEmpty()) {
            EmptyFavouriteClubs(onAddClubs = onAddClubs)
            return@Column
        }

        ClubSelectorBar(
            clubs = state.clubs,
            activeClubId = state.activeClubId,
            onSelect = { viewModel.setActiveClub(it) }
        )

        val active = state.activeClub ?: return@Column
        ClubHeader(
            club = active,
            loadedSeason = state.detail.loadedSeason,
            onTeamClick = onTeamClick
        )

        ClubTabs(
            club = active,
            detail = state.detail,
            viewModel = viewModel,
            onPlayerClick = onPlayerClick,
            onMatchClick = onMatchClick
        )
    }
}

@Composable
private fun TopBar(onSearchClick: () -> Unit, onRefresh: () -> Unit) {
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
                    text = "Your ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Favorites",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }
            Text(
                text = "Teams and leagues you follow",
                fontSize = 11.sp,
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderIcon(
                icon = Icons.Default.Search,
                onClick = onSearchClick
            )
            HeaderIcon(icon = Icons.Default.Notifications)
        }
    }
}

@Composable
private fun EmptyFavouriteClubs(onAddClubs: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No favourite clubs yet",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAddClubs,
            colors = ButtonDefaults.buttonColors(containerColor = GlassGlowGreen, contentColor = PitchBlack),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Clubs", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ClubSelectorBar(
    clubs: List<FavouriteClub>,
    activeClubId: Int?,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(PitchSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(clubs) { club ->
                val active = club.clubId == activeClubId
                Column(
                    modifier = Modifier
                        .width(60.dp)
                        .clickable { onSelect(club.clubId) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(
                                width = 2.dp,
                                color = if (active) GlassGlowGreen else Color.White.copy(alpha = 0.20f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = club.logoUrl,
                            contentDescription = club.clubName,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = club.clubName,
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = Color.White.copy(alpha = if (active) 1f else 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth(0.7f)
                            .background(if (active) GlassGlowGreen else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClubHeader(
    club: FavouriteClub,
    loadedSeason: Int,
    onTeamClick: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(GlassGlowGreen.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            .clickable { onTeamClick(club.clubId, club.leagueId) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.5.dp, GlassGlowGreen.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(model = club.logoUrl, contentDescription = club.clubName, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = club.clubName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = OnboardingDefaults.leagueLogoUrl(club.leagueId),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${club.leagueName}  •  ${SeasonUtils.displaySeasonLabel(loadedSeason)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.60f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "View Club",
            tint = Color.White.copy(alpha = 0.30f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ClubTabs(
    club: FavouriteClub,
    detail: ClubDetailUiState,
    viewModel: FavouriteClubsViewModel,
    onPlayerClick: (Int) -> Unit,
    onMatchClick: (Int) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fixtures", "Squad", "Info", "Community")

    ScrollableTabRow(
        selectedTabIndex = tab,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[tab]),
                height = 3.dp,
                color = GlassGlowGreen
            )
        }
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = tab == index
            Tab(
                selected = selected,
                onClick = { tab = index },
                text = {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White.copy(alpha = if (selected) 1f else 0.5f)
                    )
                }
            )
        }
    }

    when (tab) {
        0 -> FixturesTab(detail = detail, clubId = club.clubId, onMatchClick = onMatchClick)
        1 -> SquadTab(detail = detail, onPlayerClick = onPlayerClick)
        2 -> InfoTab(detail = detail)
        3 -> CommunityTab(viewModel = viewModel, club = club)
    }
}

@Composable
private fun FixturesTab(
    detail: ClubDetailUiState,
    clubId: Int,
    onMatchClick: (Int) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    val now = System.currentTimeMillis() / 1000L
    val fixtures = detail.fixtures
    val upcoming = fixtures.filter { it.timestamp >= now }.sortedBy { it.timestamp }
    val results = fixtures.filter { it.timestamp < now }.sortedByDescending { it.timestamp }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SegmentedRow(options = listOf("Upcoming", "Results"), selected = mode, onSelect = { mode = it })
        Spacer(Modifier.height(12.dp))

        if (detail.isLoading) {
            SkeletonList(rows = 6, rowHeight = 60.dp)
            return@Column
        }
        if (detail.fixturesError != null) {
            ErrorInline(detail.fixturesError)
            return@Column
        }

        val list = if (mode == 0) upcoming else results
        if (list.isEmpty()) {
            Text(
                text = if (mode == 0) "No upcoming fixtures found" else "No recent results found",
                color = Color.White.copy(alpha = 0.65f)
            )
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list) { match ->
                MatchRow(match = match, clubId = clubId, isResult = mode == 1, onMatchClick = onMatchClick)
            }
        }
    }
}

private fun getSimulatedMarketValue(name: String, position: String?): String {
    val cleanName = name.lowercase()
    if (cleanName.contains("haaland")) return "€180.00M"
    if (cleanName.contains("saka")) return "€130.00M"
    if (cleanName.contains("ødegaard") || cleanName.contains("odegaard")) return "€110.00M"
    if (cleanName.contains("mbappé") || cleanName.contains("mbappe")) return "€180.00M"
    if (cleanName.contains("bellingham")) return "€180.00M"
    if (cleanName.contains("vinicius") || cleanName.contains("vini")) return "€150.00M"
    if (cleanName.contains("musiala")) return "€110.00M"
    if (cleanName.contains("wirtz")) return "€110.00M"
    if (cleanName.contains("foden")) return "€150.00M"
    if (cleanName.contains("rice")) return "€110.00M"
    if (cleanName.contains("rodri")) return "€110.00M"
    if (cleanName.contains("kane")) return "€110.00M"
    if (cleanName.contains("salah")) return "€65.00M"
    if (cleanName.contains("palmer")) return "€90.00M"
    if (cleanName.contains("saliba")) return "€80.00M"

    val hash = name.hashCode().let { if (it < 0) -it else it }
    val baseValue = when (position) {
        "Goalkeeper" -> 5 + (hash % 25)
        "Defender" -> 10 + (hash % 65)
        "Midfielder" -> 15 + (hash % 85)
        "Attacker" -> 20 + (hash % 100)
        else -> 10 + (hash % 50)
    }
    val decimalStr = when (hash % 4) {
        0 -> "00"
        1 -> "50"
        2 -> "80"
        else -> "25"
    }
    return "€$baseValue.${decimalStr}M"
}

@Composable
private fun SquadTab(
    detail: ClubDetailUiState,
    onPlayerClick: (Int) -> Unit
) {
    val squad = detail.teamDetail?.squad.orEmpty()
    if (detail.isLoading) {
        Column(Modifier.fillMaxSize().padding(16.dp)) { SkeletonList(rows = 8, rowHeight = 56.dp) }
        return
    }
    if (detail.teamDetailError != null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) { ErrorInline(detail.teamDetailError) }
        return
    }

    val grouped = squad.groupBy { it.position ?: "Other" }
    val order = listOf("Goalkeeper", "Defender", "Midfielder", "Attacker", "Other")
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (squad.isEmpty()) {
            item {
                Text("No squad data found", color = Color.White.copy(alpha = 0.65f))
            }
            return@LazyColumn
        }
        order.forEach { key ->
            val players = grouped[key].orEmpty()
            if (players.isNotEmpty()) {
                val positionColor = when (key) {
                    "Goalkeeper" -> Color(0xFFB0BEC5) // Silver
                    "Defender" -> Color(0xFF81C784) // Green
                    "Midfielder" -> Color(0xFF64B5F6) // Blue
                    "Attacker" -> Color(0xFFFFD54F) // Gold
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
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
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
                            // Player Avatar with position-colored border glow and jersey number overlay
                            Box(
                                modifier = Modifier.size(50.dp)
                            ) {
                                PlayerAvatar(
                                    url = p.photo,
                                    name = p.name,
                                    ringColor = positionColor.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .align(Alignment.TopStart)
                                )
                                
                                val number = p.number
                                if (number != null) {
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
                                Text(
                                    text = p.position ?: "",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            
                            // Market value badge on the right
                            val marketValue = getSimulatedMarketValue(p.name, p.position)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassGlowGreen.copy(alpha = 0.12f))
                                    .border(0.5.dp, GlassGlowGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = marketValue,
                                    color = GlassGlowGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransfersTab(detail: ClubDetailUiState, clubName: String) {
    var mode by remember { mutableIntStateOf(0) }
    val transfers = detail.teamDetail?.transfers.orEmpty()
    val incoming = transfers.filter { it.teamIn.equals(clubName, ignoreCase = true) }
    val outgoing = transfers.filter { it.teamOut.equals(clubName, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SegmentedRow(options = listOf("Incoming", "Outgoing"), selected = mode, onSelect = { mode = it })
        Spacer(Modifier.height(12.dp))

        if (detail.isLoading) {
            SkeletonList(rows = 7, rowHeight = 72.dp)
            return@Column
        }
        if (detail.teamDetailError != null) {
            ErrorInline(detail.teamDetailError)
            return@Column
        }

        val list = if (mode == 0) incoming else outgoing
        if (list.isEmpty()) {
            Text(
                text = if (mode == 0) "No incoming transfers found" else "No outgoing transfers found",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                modifier = Modifier.padding(8.dp)
            )
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { t ->
                val accentColor = if (mode == 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                val priceVal = when {
                    t.type.contains("€") || t.type.contains("£") || t.type.contains("M") -> t.type
                    t.type.lowercase().contains("free") -> "Free"
                    t.type.lowercase().contains("loan") -> "Loan"
                    else -> "€45M"
                }
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Player photo avatar with arrow badge overlay
                        Box(
                            modifier = Modifier.size(50.dp)
                        ) {
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
                                    AsyncImage(
                                        model = destinationLogo,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
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
                                Text(
                                    priceVal,
                                    color = accentColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t.date,
                                color = Color.White.copy(alpha = 0.40f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTab(detail: ClubDetailUiState) {
    val td = detail.teamDetail
    if (detail.isLoading) {
        Column(Modifier.fillMaxSize().padding(16.dp)) { SkeletonList(rows = 8, rowHeight = 60.dp) }
        return
    }
    if (detail.teamDetailError != null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) { ErrorInline(detail.teamDetailError) }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stadium Details Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Stadium, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Club Stadium", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    td?.venue?.image?.let { img ->
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
                    DetailRow(icon = Icons.Rounded.Stadium, label = "Venue", value = td?.venue?.name ?: "-")
                    DetailRow(icon = Icons.Rounded.Place, label = "City", value = td?.venue?.city ?: "-")
                    
                    val capacity = td?.venue?.capacity
                    if (capacity != null) {
                        DetailRow(icon = Icons.Rounded.Groups, label = "Capacity", value = String.format("%,d seats", capacity))
                        Spacer(Modifier.height(12.dp))
                        // Visual Capacity Gauge Meter
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Arena Size", color = TextSecondary, fontSize = 10.sp)
                                Text("${capacity / 1000}K capacity", color = GlassGlowGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(GlassGlowGreen.copy(alpha = 0.7f), GlassGlowGreen)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Season Stats Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
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
                    val stats = td?.stats
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = "Matches", value = "${stats?.played ?: "-"}")
                        StatItem(label = "Wins", value = "${stats?.wins ?: "-"}", valueColor = Color(0xFF4CAF50))
                        StatItem(label = "Draws", value = "${stats?.draws ?: "-"}", valueColor = Color(0xFFFFC107))
                        StatItem(label = "Losses", value = "${stats?.loses ?: "-"}", valueColor = Color(0xFFF44336))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = "Goals For", value = "${stats?.goalsFor ?: "-"}")
                        StatItem(label = "Goals Against", value = "${stats?.goalsAgainst ?: "-"}")
                        val gd = if (stats != null) stats.goalsFor - stats.goalsAgainst else 0
                        StatItem(
                            label = "Goal Diff",
                            value = if (stats != null) (if (gd >= 0) "+$gd" else "$gd") else "-",
                            valueColor = if (gd >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                    
                    stats?.form?.let { form ->
                        if (form.isNotBlank()) {
                            Spacer(Modifier.height(18.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent Form: ", color = TextSecondary, fontSize = 11.sp)
                                Spacer(Modifier.width(6.dp))
                                form.take(5).forEach { c ->
                                    val dotColor = when (c.uppercaseChar()) {
                                        'W' -> Color(0xFF4CAF50)
                                        'D' -> Color(0xFFFFC107)
                                        'L' -> Color(0xFFF44336)
                                        else -> Color.White.copy(alpha = 0.30f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Coach Bio Card
        td?.coaches?.firstOrNull()?.let { coach ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Head Coach", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.5.dp, GlassGlowGreen.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = coach.photo,
                                    contentDescription = coach.name,
                                    modifier = Modifier.size(54.dp).clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(coach.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Manager / Tactician", color = TextSecondary, fontSize = 12.sp)
                            }
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
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, modifier = Modifier.width(80.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun SegmentedRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) GlassGlowGreen else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (active) PitchBlack else Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MatchRow(
    match: Match,
    clubId: Int,
    isResult: Boolean,
    onMatchClick: (Int) -> Unit
) {
    val isHome = match.homeTeam.id == clubId
    val opponent = if (isHome) match.awayTeam else match.homeTeam

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable { onMatchClick(match.id) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isHome) "H" else "A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        AsyncImage(model = opponent.logo, contentDescription = opponent.name, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(opponent.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            val subtitle = match.league.name
            Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1)
        }
        if (isResult) {
            val home = match.homeScore ?: 0
            val away = match.awayScore ?: 0
            val win = (isHome && home > away) || (!isHome && away > home)
            val draw = home == away
            val color = when {
                draw -> Color(0xFFFFC107)
                win -> Color(0xFF4CAF50)
                else -> Color(0xFFF44336)
            }
            Text("$home-$away", color = color, fontWeight = FontWeight.Black, fontSize = 16.sp)
        } else {
            // Format timestamp as readable date+time
            val dateLabel = remember(match.timestamp) {
                try {
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = match.timestamp * 1000L
                    val now = java.util.Calendar.getInstance()
                    val tomorrow = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
                    val dayStr = when {
                        cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) &&
                        cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) -> "Today"
                        cal.get(java.util.Calendar.DAY_OF_YEAR) == tomorrow.get(java.util.Calendar.DAY_OF_YEAR) &&
                        cal.get(java.util.Calendar.YEAR) == tomorrow.get(java.util.Calendar.YEAR) -> "Tomorrow"
                        else -> java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault())
                            .format(cal.time)
                    }
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(cal.time)
                    "$dayStr • $timeStr"
                } catch (e: Exception) {
                    match.date
                }
            }
            Text(dateLabel, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SkeletonList(rows: Int, rowHeight: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(rows) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            )
        }
    }
}

@Composable
private fun ErrorInline(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = message, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CommunityTab(
    viewModel: FavouriteClubsViewModel,
    club: FavouriteClub
) {
    val state by viewModel.uiState.collectAsState()
    var showCreatePostDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                DeveloperControls(
                    isPremium = state.isPremium,
                    onTogglePremium = { viewModel.togglePremium() }
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(GlassGlowGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${club.clubName.uppercase()} FAN FEED",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            if (state.socialPosts.isEmpty()) {
                item {
                    EmptySocialFeedPlaceholder(clubName = club.clubName)
                }
            } else {
                items(state.socialPosts, key = { "post_${it.id}" }) { post ->
                    SocialPostCard(
                        post = post,
                        onLikeClick = { viewModel.likePost(post.id) },
                        onVoteClick = { optionIdx -> viewModel.voteInPoll(post.id, optionIdx) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreatePostDialog = true },
            containerColor = GlassGlowGreen,
            contentColor = PitchBlack,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Create Post")
        }

        if (showCreatePostDialog) {
            CreatePostDialog(
                onDismiss = { showCreatePostDialog = false },
                onSubmit = { content, tag, options ->
                    viewModel.createSocialPost(content, tag, options)
                    showCreatePostDialog = false
                }
            )
        }
    }
}

@Composable
private fun DeveloperControls(
    isPremium: Boolean,
    onTogglePremium: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "🛠️ Developer Sandbox Controls",
                color = GlassGlowGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPremium) GlassGlowGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                    .border(0.5.dp, if (isPremium) GlassGlowGreen else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onTogglePremium() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPremium) "PRO Enabled 👑  (Tap to downgrade)" else "Free Account — Tap to simulate PRO",
                    color = if (isPremium) GlassGlowGreen else Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// LiveMatchChatCard and ChatInactivePlaceholder removed – feature dropped per user request

@Composable
private fun SocialPostCard(
    post: SocialPost,
    onLikeClick: () -> Unit,
    onVoteClick: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.username.take(2).uppercase(),
                        color = GlassGlowGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = remember(post.timestamp) {
                            val elapsedMin = (System.currentTimeMillis() - post.timestamp) / (60 * 1000)
                            if (elapsedMin < 60) {
                                "$elapsedMin mins ago"
                            } else {
                                val elapsedHrs = elapsedMin / 60
                                if (elapsedHrs < 24) "$elapsedHrs hrs ago" else "${elapsedHrs / 24} days ago"
                            }
                        },
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }

                if (post.hotTakeTag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassGlowGreen.copy(alpha = 0.1f))
                            .border(0.5.dp, GlassGlowGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = post.hotTakeTag,
                            color = GlassGlowGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = post.content,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )

            if (post.pollQuestion != null && post.pollOptions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val totalVotes = post.pollOptions.sumOf { it.votes }.coerceAtLeast(1)
                val userVoted = post.userVotedIndex != null
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    post.pollOptions.forEachIndexed { idx, opt ->
                        val pct = (opt.votes * 100) / totalVotes
                        val isUserPick = post.userVotedIndex == idx
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable(enabled = !userVoted) { onVoteClick(idx) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(opt.votes.toFloat() / totalVotes)
                                    .background(if (isUserPick) GlassGlowGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt.text,
                                    color = if (isUserPick) GlassGlowGreen else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = if (isUserPick) FontWeight.Bold else FontWeight.Normal
                                )
                                if (userVoted) {
                                    Text(
                                        text = "$pct%",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (post.hasLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.hasLiked) Color(0xFFFF4444) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.likes}",
                        color = if (post.hasLiked) Color.White else Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.commentCount}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySocialFeedPlaceholder(clubName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Groups,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No Community Posts Yet",
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Be the first to share your thoughts about $clubName!",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CreatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String?, List<String>?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var hotTakeTag by remember { mutableStateOf<String?>(null) }
    var isPollSelected by remember { mutableStateOf(false) }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Fan Post",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = Color(0xFF1E1E1E),
        tonalElevation = 6.dp,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("What is on your mind?", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        disabledContainerColor = Color.White.copy(alpha = 0.04f),
                        cursorColor = GlassGlowGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tag: ", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    listOf("None", "Matchday", "Hot Take 🔥", "Lineup").forEach { label ->
                        val currentVal = if (label == "None") null else label
                        val selected = hotTakeTag == currentVal
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) GlassGlowGreen else Color.White.copy(alpha = 0.06f))
                                .clickable { hotTakeTag = currentVal }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) PitchBlack else Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPollSelected,
                        onCheckedChange = { isPollSelected = it },
                        colors = CheckboxDefaults.colors(checkedColor = GlassGlowGreen, uncheckedColor = Color.White.copy(alpha = 0.4f))
                    )
                    Text("Add a Poll to this post", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }

                if (isPollSelected) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = pollOption1,
                            onValueChange = { pollOption1 = it },
                            placeholder = { Text("Poll Option 1", fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = GlassGlowGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = pollOption2,
                            onValueChange = { pollOption2 = it },
                            placeholder = { Text("Poll Option 2", fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = GlassGlowGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = pollOption3,
                            onValueChange = { pollOption3 = it },
                            placeholder = { Text("Poll Option 3 (Optional)", fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = GlassGlowGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val options = if (isPollSelected) listOf(pollOption1, pollOption2, pollOption3).filter { it.isNotBlank() } else null
                    onSubmit(text, hotTakeTag, options)
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GlassGlowGreen, contentColor = PitchBlack)
            ) {
                Text("Post", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
