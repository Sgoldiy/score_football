package com.example.footballapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footballapp.data.model.FixtureEvent
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.model.FixtureTeamStatistics
import com.example.footballapp.ui.theme.DangerRed
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.LiveGreen
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.SignalAmber
import com.example.footballapp.ui.theme.TextSecondary

data class StatData(
    val label: String,
    val home: String,
    val away: String,
    val accent: Color
)

@Composable
fun BroadcastMatchCard(
    match: FixtureResponse,
    modifier: Modifier = Modifier,
    expandedByDefault: Boolean = false,
    onClick: () -> Unit
) {
    var expanded by remember(match.fixture?.id) { mutableStateOf(expandedByDefault) }
    val isLive = match.isLiveMatch()
    val scoreScale by animateFloatAsState(
        targetValue = if (isLive) 1.04f else 1f,
        animationSpec = tween(600),
        label = "score-scale"
    )

    PremiumCard(
        modifier = modifier
            .animateContentSize()
            .clickable(onClick = onClick),
        brush = Brush.linearGradient(
            listOf(
                if (isLive) Color(0xFF0E3B29) else Color(0xFF0A1E38),
                PitchSurfaceHigh,
                PitchBlack
            )
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MatchHeader(match, isLive)
            ScoreRow(match, isLive, scoreScale)
            CompactStatsRow(match)
            BottomActionRow(match, expanded, onToggle = { expanded = !expanded })
            AnimatedVisibility(expanded) {
                ExpandedStatsSection(match)
            }
        }
    }
}

@Composable
private fun MatchHeader(match: FixtureResponse, isLive: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            FootballLogo(
                match.league?.logo,
                match.league?.name,
                Modifier.size(26.dp),
                glow = if (isLive) LiveGreen else IceBlue
            )
            Spacer(Modifier.width(8.dp))
            Text(
                match.league?.name ?: "Competition",
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLive) {
                LivePulse(color = LiveGreen)
                Spacer(Modifier.width(5.dp))
            }
            val statusText = match.broadcastStatus()
            Text(
                statusText,
                color = if (isLive) LiveGreen else Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
            if (isLive) {
                Box(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LiveGreen)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("LIVE", color = PitchBlack, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(match: FixtureResponse, isLive: Boolean, scale: Float) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeamScoreBlock(
            name = match.teams?.home?.name.orEmpty(),
            logo = match.teams?.home?.logo,
            alignStart = true,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${match.goals?.home ?: "-"}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " : ",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
                Text(
                    text = "${match.goals?.away ?: "-"}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        TeamScoreBlock(
            name = match.teams?.away?.name.orEmpty(),
            logo = match.teams?.away?.logo,
            alignStart = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TeamScoreBlock(
    name: String,
    logo: String?,
    alignStart: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!alignStart) {
            Text(
                name.ifBlank { "Team" },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
        }
        FootballLogo(logo, name, Modifier.size(40.dp), glow = LiveGreen.copy(alpha = 0.3f))
        if (alignStart) {
            Spacer(Modifier.width(8.dp))
            Text(
                name.ifBlank { "Team" },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactStatsRow(match: FixtureResponse) {
    val stats = match.statistics.orEmpty()

    val statList = listOf(
        StatData("Poss", stats.valueFor(0, "Ball Possession"), stats.valueFor(1, "Ball Possession"), LiveGreen),
        StatData("SOT", stats.valueFor(0, "Shots on Goal"), stats.valueFor(1, "Shots on Goal"), IceBlue),
        StatData("Corn", stats.valueFor(0, "Corner Kicks"), stats.valueFor(1, "Corner Kicks"), SignalAmber),
        StatData("Fl", stats.valueFor(0, "Fouls"), stats.valueFor(1, "Fouls"), DangerRed),
    )

    val availableStats = statList.filter { it.home.isNotBlank() || it.away.isNotBlank() }

    if (availableStats.isEmpty()) return

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        availableStats.forEach { stat ->
            StatPill(
                label = stat.label,
                value = stat.home,
                secondary = stat.away,
                accent = stat.accent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    secondary: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val displayValue = value.ifBlank { "0" }
    val displaySecondary = secondary.ifBlank { "0" }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = displayValue,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Text(
                text = " : ",
                color = Color.White.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = displaySecondary,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun BottomActionRow(
    match: FixtureResponse,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommentaryPreview(match, Modifier.weight(1f))
        if (hasAnyStats(match)) {
            Text(
                if (expanded) "Less" else "Stats",
                color = LiveGreen,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CommentaryPreview(match: FixtureResponse, modifier: Modifier = Modifier) {
    val latest = match.events.orEmpty().maxByOrNull { it.time?.elapsed ?: -1 }
    val text = latest?.let {
        "${it.minuteLabel()} ${it.detail ?: it.type ?: "Event"} ${it.player?.name.orEmpty()}".trim()
    } ?: when {
        match.isLiveMatch() -> "Live action in progress"
        match.fixture?.status?.short == "NS" -> "Kick-off soon"
        else -> "Match finished"
    }
    Text(
        text,
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun ExpandedStatsSection(match: FixtureResponse) {
    val stats = match.statistics.orEmpty()

    val statItems = listOf(
        "Ball Possession" to LiveGreen,
        "Shots on Goal" to IceBlue,
        "Corner Kicks" to SignalAmber,
        "Fouls" to DangerRed,
        "Yellow Cards" to SignalAmber,
        "Red Cards" to DangerRed,
        "Shots off Goal" to IceBlue.copy(alpha = 0.7f),
        "Total Shots" to Color.White,
        "Goalkeeper Saves" to IceBlue,
        "Offsides" to TextSecondary,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var hasAnyStat = false
        statItems.forEach { (statType, accent) ->
            val homeVal = stats.valueFor(0, statType)
            val awayVal = stats.valueFor(1, statType)
            if (homeVal.isNotBlank() || awayVal.isNotBlank()) {
                hasAnyStat = true
                StatComparisonBar(
                    label = statType,
                    home = homeVal.numericOrFallback(null),
                    away = awayVal.numericOrFallback(null),
                    homeText = homeVal.ifBlank { "0" },
                    awayText = awayVal.ifBlank { "0" }
                )
            }
        }

        if (!hasAnyStat) {
            Text(
                "No detailed stats available",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        val events = match.events.orEmpty().takeLast(4).reversed()
        if (events.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Events", color = TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            events.forEach { event ->
                EventMicroRow(event)
            }
        }
    }
}

@Composable
private fun EventMicroRow(event: FixtureEvent) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            event.minuteLabel(),
            color = LiveGreen,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(38.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                event.detail ?: event.type ?: "Event",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                listOfNotNull(event.player?.name, event.assist?.name?.let { "Assist: $it" }).joinToString(" • "),
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun hasAnyStats(match: FixtureResponse): Boolean {
    return match.statistics.orEmpty().any { teamStats ->
        teamStats.statistics.orEmpty().any { it.value?.display?.isNotBlank() == true }
    }
}

private fun FixtureResponse.isLiveMatch(): Boolean {
    return fixture?.status?.short in setOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
}

private fun FixtureResponse.broadcastStatus(): String {
    val elapsed = fixture?.status?.elapsed
    val extra = fixture?.status?.extra
    return when {
        elapsed != null && extra != null -> "$elapsed+$extra'"
        elapsed != null -> "$elapsed'"
        else -> fixture?.status?.short ?: fixture?.status?.long ?: "-"
    }
}

private fun List<FixtureTeamStatistics>.valueFor(index: Int, label: String): String {
    return getOrNull(index)?.statistics.orEmpty()
        .firstOrNull { it.type == label }
        ?.value?.display?.takeIf { it != "-" && !it.isNullOrBlank() }
        .orEmpty()
}

private fun String.numericOrFallback(goals: Int?): Float {
    return replace("%", "").toFloatOrNull() ?: 0f
}

private fun FixtureEvent.minuteLabel(): String {
    val elapsed = time?.elapsed ?: return "-"
    return if (time.extra != null) "$elapsed+${time.extra}'" else "$elapsed'"
}
