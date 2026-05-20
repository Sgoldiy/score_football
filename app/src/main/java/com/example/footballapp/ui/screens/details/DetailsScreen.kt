package com.example.footballapp.ui.screens.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.data.model.FixtureEvent
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.model.FixtureTeamStatistics
import com.example.footballapp.data.model.Prediction
import com.example.footballapp.data.repository.FixtureDetailData
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.ui.components.InfoPill
import com.example.footballapp.ui.components.LivePulse
import com.example.footballapp.ui.components.PremiumCard
import com.example.footballapp.ui.components.PlayerAvatar
import com.example.footballapp.ui.components.SectionTitle
import com.example.footballapp.ui.components.ShimmerBlock
import com.example.footballapp.ui.components.StatComparisonBar
import com.example.footballapp.ui.components.TeamCrestName
import com.example.footballapp.ui.theme.DangerRed
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.LiveGreen
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.SignalAmber
import com.example.footballapp.ui.theme.TextSecondary
import com.example.footballapp.viewmodel.FixtureDetailViewModel

private enum class DetailTab(val label: String) {
    Summary("Summary"),
    Stats("Stats"),
    Lineups("Lineups"),
    Players("Players"),
    H2H("H2H")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    fixtureId: String,
    onBackClick: () -> Unit,
    viewModel: FixtureDetailViewModel = hiltViewModel()
) {
    val id = fixtureId.toIntOrNull()
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(id) {
        if (id != null) viewModel.loadFixtureDetails(id)
    }

    Scaffold(
        containerColor = PitchBlack,
        topBar = {
            TopAppBar(
                title = { Text("Match center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Share, contentDescription = "Share match")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PitchBlack,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (id == null) {
            CenterMessage("Invalid fixture id", paddingValues)
            return@Scaffold
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF07150F), PitchBlack)))
                .padding(paddingValues)
        ) {
            when (val current = state) {
                ApiResult.Loading -> DetailsShimmer()
                is ApiResult.Error -> CenterMessage(current.message, PaddingValues())
                is ApiResult.Success -> DetailContent(current.data)
            }
        }
    }
}

@Composable
private fun DetailContent(data: FixtureDetailData) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = DetailTab.entries

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MatchHeader(data.fixture)
        }
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = LiveGreen,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label) }
                    )
                }
            }
        }
        item {
            AnimatedContent(targetState = tabs[selectedTab], label = "detail-tab") { tab ->
                when (tab) {
                    DetailTab.Summary -> SummaryTab(data)
                    DetailTab.Stats -> StatsTab(data.statistics)
                    DetailTab.Lineups -> LineupsTab(data)
                    DetailTab.Players -> PlayersTab(data)
                    DetailTab.H2H -> H2HTab(data)
                }
            }
        }
    }
}

@Composable
private fun MatchHeader(fixture: FixtureResponse?) {
    val isLive = fixture?.fixture?.status?.short in setOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        brush = Brush.linearGradient(listOf(Color(0xFF123C2C), PitchSurfaceHigh, PitchSurface))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = fixture?.league?.logo, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(fixture?.league?.name ?: "League", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(fixture?.league?.round ?: fixture?.league?.country ?: "", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLive) {
                        LivePulse()
                        Spacer(Modifier.size(8.dp))
                    }
                    InfoPill(fixture.statusLabel(), accent = if (isLive) LiveGreen else SignalAmber)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TeamCrestName(fixture?.teams?.home?.name.orEmpty(), fixture?.teams?.home?.logo, Modifier.weight(1f), crestSize = 64)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${fixture?.goals?.home ?: "-"} : ${fixture?.goals?.away ?: "-"}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.displaySmall)
                    Text(fixture?.fixture?.status?.long ?: "Status unavailable", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
                TeamCrestName(fixture?.teams?.away?.name.orEmpty(), fixture?.teams?.away?.logo, Modifier.weight(1f), crestSize = 64)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Stadium", listOfNotNull(fixture?.fixture?.venue?.name, fixture?.fixture?.venue?.city).joinToString(", ").ifBlank { "-" })
                InfoRow("Referee", fixture?.fixture?.referee ?: "-")
                InfoRow("Kickoff", fixture?.fixture?.date?.replace("T", " ") ?: "-")
            }
        }
    }
}

@Composable
private fun SummaryTab(data: FixtureDetailData) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EventTimeline(data.events)
        PredictionCard(data.predictions.firstOrNull())
        MiniH2H(data.headToHead)
        InjuriesCard(data)
        UnavailableCard(data.errors)
    }
}

@Composable
private fun EventTimeline(events: List<FixtureEvent>) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column {
            SectionTitle("Timeline", trailing = "${events.size} events")
            Spacer(Modifier.height(12.dp))
            if (events.isEmpty()) {
                Text("No timeline events available", color = TextSecondary)
            } else {
                events.sortedBy { it.time?.elapsed ?: 0 }.forEach { event ->
                    EventRow(event)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: FixtureEvent) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(event.minuteLabel(), color = LiveGreen, style = MaterialTheme.typography.labelMedium, modifier = Modifier.size(width = 48.dp, height = 20.dp))
        Column(Modifier.weight(1f)) {
            Text("${event.type ?: "Event"} • ${event.detail ?: "-"}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(listOfNotNull(event.player?.name, event.assist?.name?.let { "Assist: $it" }, event.team?.name).joinToString("  "), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatsTab(stats: List<FixtureTeamStatistics>) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column {
            SectionTitle("Match stats", trailing = "${stats.size} teams")
            Spacer(Modifier.height(14.dp))
            val home = stats.getOrNull(0)
            val away = stats.getOrNull(1)
            if (home == null || away == null) {
                Text("No team statistics available", color = TextSecondary)
                return@Column
            }
            val statNames = (home.statistics.orEmpty().mapNotNull { it.type } + away.statistics.orEmpty().mapNotNull { it.type }).distinct()
            statNames.take(16).forEach { label ->
                val homeText = home.valueFor(label)
                val awayText = away.valueFor(label)
                StatComparisonBar(
                    label = label,
                    home = homeText.numericValue(),
                    away = awayText.numericValue(),
                    homeText = homeText,
                    awayText = awayText
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun LineupsTab(data: FixtureDetailData) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (data.lineups.isEmpty()) {
            EmptyPanel("Lineups are not available yet")
        } else {
            data.lineups.forEach { lineup ->
                PremiumCard(Modifier.fillMaxWidth()) {
                    Column {
                        SectionTitle(lineup.team?.name ?: "Team", trailing = lineup.formation ?: "Formation")
                        Spacer(Modifier.height(12.dp))
                        FormationPitch(lineup.startXI.orEmpty())
                        Spacer(Modifier.height(14.dp))
                        Text("Coach: ${lineup.coach?.name ?: "-"}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        Text("Starting XI", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        lineup.startXI.orEmpty().forEach { row ->
                            InfoRow(row.player?.pos ?: "-", "${row.player?.number ?: ""} ${row.player?.name ?: "-"}")
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Substitutes", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        lineup.substitutes.orEmpty().take(12).forEach { row ->
                            InfoRow(row.player?.pos ?: "-", "${row.player?.number ?: ""} ${row.player?.name ?: "-"}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayersTab(data: FixtureDetailData) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (data.playerStats.isEmpty()) {
            EmptyPanel("Player ratings are not available yet")
        } else {
            data.playerStats.forEach { team ->
                PremiumCard(Modifier.fillMaxWidth()) {
                    Column {
                        SectionTitle(team.team?.name ?: "Team", trailing = "${team.players.orEmpty().size} players")
                        Spacer(Modifier.height(10.dp))
                        team.players.orEmpty().take(14).forEach { entry ->
                            val stats = entry.statistics?.firstOrNull()
                            PlayerStatRow(
                                name = entry.player?.name ?: "-",
                                photo = entry.player?.photo,
                                meta = listOfNotNull(
                                    stats?.games?.position,
                                    stats?.games?.rating?.let { "Rating $it" },
                                    stats?.goals?.total?.let { "$it goals" },
                                    stats?.goals?.assists?.let { "$it assists" },
                                    stats?.passes?.key?.let { "$it key passes" }
                                ).joinToString(" • ").ifBlank { "-" }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun H2HTab(data: FixtureDetailData) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MiniH2H(data.headToHead)
        PredictionCard(data.predictions.firstOrNull())
    }
}

@Composable
private fun PredictionCard(prediction: Prediction?) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column {
            SectionTitle("Prediction", trailing = prediction?.predictions?.winner?.name)
            Spacer(Modifier.height(12.dp))
            if (prediction == null) {
                Text("No prediction data available", color = TextSecondary)
                return@Column
            }
            InfoRow("Winner", prediction.predictions?.winner?.name ?: "-")
            InfoRow("Comment", prediction.predictions?.winner?.comment ?: "-")
            InfoRow("Advice", prediction.predictions?.advice ?: "-")
            StatComparisonBar("win probability", prediction.predictions?.percent?.home.numericValue(), prediction.predictions?.percent?.away.numericValue(), prediction.predictions?.percent?.home ?: "-", prediction.predictions?.percent?.away ?: "-")
            Spacer(Modifier.height(8.dp))
            InfoRow("Draw", prediction.predictions?.percent?.draw ?: "-")
            InfoRow("Under/Over", prediction.predictions?.under_over ?: "-")
        }
    }
}

@Composable
private fun FormationPitch(players: List<com.example.footballapp.data.model.LineupPlayerWrapper>) {
    val rows = players
        .mapNotNull { it.player }
        .groupBy { it.grid?.substringBefore(":")?.toIntOrNull() ?: 9 }
        .toSortedMap()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F5C36), Color(0xFF083D27), Color(0xFF052719))
                )
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            rows.values.forEach { rowPlayers ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowPlayers.forEach { player ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color.White.copy(alpha = 0.92f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${player.number ?: ""}", color = PitchBlack, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                            }
                            Text(player.name ?: "-", color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatRow(name: String, photo: String?, meta: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAvatar(url = photo, name = name, modifier = Modifier.size(42.dp), ringColor = LiveGreen.copy(alpha = 0.35f))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, color = TextSecondary, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MiniH2H(matches: List<FixtureResponse>) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column {
            SectionTitle("Head to head", trailing = "${matches.size} games")
            Spacer(Modifier.height(12.dp))
            if (matches.isEmpty()) {
                Text("No recent head to head data", color = TextSecondary)
            } else {
                matches.take(5).forEach { match ->
                    InfoRow(
                        "${match.teams?.home?.name ?: "Home"} vs ${match.teams?.away?.name ?: "Away"}",
                        "${match.goals?.home ?: "-"} : ${match.goals?.away ?: "-"}"
                    )
                }
            }
        }
    }
}

@Composable
private fun InjuriesCard(data: FixtureDetailData) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column {
            SectionTitle("Injuries", trailing = "${data.injuries.size}")
            Spacer(Modifier.height(10.dp))
            if (data.injuries.isEmpty()) {
                Text("No injury report available", color = TextSecondary)
            } else {
                data.injuries.take(8).forEach {
                    InfoRow(it.player?.name ?: "-", it.team?.name ?: "-")
                }
            }
        }
    }
}

@Composable
private fun UnavailableCard(errors: Map<String, String>) {
    if (errors.isEmpty()) return
    PremiumCard(Modifier.fillMaxWidth(), brush = Brush.linearGradient(listOf(Color(0xFF32161B), PitchSurface))) {
        Column {
            SectionTitle("Limited data", trailing = "${errors.size}")
            Spacer(Modifier.height(8.dp))
            errors.forEach { (section, error) ->
                Text("$section: $error", color = DangerRed, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyPanel(text: String) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailsShimmer() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ShimmerBlock(Modifier.fillMaxWidth().height(260.dp), RoundedCornerShape(24.dp)) }
        item { ShimmerBlock(Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(18.dp)) }
        items(4) { ShimmerBlock(Modifier.fillMaxWidth().height(140.dp), RoundedCornerShape(24.dp)) }
    }
}

@Composable
private fun CenterMessage(message: String, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onBackground)
    }
}

private fun FixtureResponse?.statusLabel(): String {
    val elapsed = this?.fixture?.status?.elapsed
    val extra = this?.fixture?.status?.extra
    return when {
        elapsed != null && extra != null -> "$elapsed+$extra'"
        elapsed != null -> "$elapsed'"
        else -> this?.fixture?.status?.short ?: this?.fixture?.status?.long ?: "-"
    }
}

private fun FixtureEvent.minuteLabel(): String {
    val elapsed = time?.elapsed ?: return "-"
    return if (time.extra != null) "$elapsed+${time.extra}'" else "$elapsed'"
}

private fun FixtureTeamStatistics.valueFor(label: String): String {
    val value = statistics.orEmpty().firstOrNull { it.type == label }?.value
    return value?.display ?: "-"
}

private fun String?.numericValue(): Float {
    if (this == null) return 0f
    return replace("%", "")
        .replace(",", ".")
        .filter { it.isDigit() || it == '.' || it == '-' }
        .toFloatOrNull()
        ?: 0f
}
