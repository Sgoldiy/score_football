package com.footballpluse.footballapp.ui.screens.players

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.ui.theme.GlassGlowGreen
import com.footballpluse.footballapp.ui.theme.PitchBlack
import com.footballpluse.footballapp.ui.theme.PitchSurfaceHigh
import com.footballpluse.footballapp.ui.theme.TextSecondary
import com.footballpluse.footballapp.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    playerId: Int,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val season = SeasonUtils.currentSeasonStartYear()

    LaunchedEffect(playerId) {
        viewModel.loadPlayerData(playerId, season)
    }

    val state by viewModel.playerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name =
                        if (state is ApiResult.Success) (state as ApiResult.Success).data.info.name else "Player Profile"
                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(PitchBlack, PitchSurfaceHigh)))
        ) {
            when (val result = state) {
                is ApiResult.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GlassGlowGreen)
                    }
                }

                is ApiResult.Error -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                result.message,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is ApiResult.Success -> {
                    val detail = result.data
                    PlayerContent(detail = detail)
                }
            }
        }
    }
}

@Composable
private fun PlayerContent(detail: com.footballpluse.footballapp.domain.model.PlayerDetail) {
    val info = detail.info

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Player Bio Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(3.dp, GlassGlowGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = info.photo,
                            contentDescription = info.name,
                            modifier = Modifier
                                .size(108.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = info.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    // Info Chips Grid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        info.nationality?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumBioChip(
                                    label = "Nationality",
                                    value = it,
                                    icon = Icons.Rounded.Public
                                )
                            }
                        }
                        info.age?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumBioChip(
                                    label = "Age",
                                    value = "$it yrs",
                                    icon = Icons.Rounded.Cake
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        info.height?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumBioChip(
                                    label = "Height",
                                    value = it,
                                    icon = Icons.Rounded.Height
                                )
                            }
                        }
                        info.weight?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumBioChip(
                                    label = "Weight",
                                    value = it,
                                    icon = Icons.Rounded.FitnessCenter
                                )
                            }
                        }
                    }
                }
            }
        }
        // Season Statistics Section
        if (detail.stats.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = GlassGlowGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Season Statistics",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        detail.stats.forEach { ps ->
                            PlayerStatsCard(ps = ps)
                        }
                    }
                }
            }

            item {
                PlayerVisualAnalyticsSection(detail = detail)
            }
        }

        // Trophy Gallery Section
        if (detail.trophies.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Trophies Showcase",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(detail.trophies) { trophy ->
                            TrophyCard(trophy = trophy)
                        }
                    }
                }
            }
        }

        // Injuries Timeline Section
        if (detail.sidelined.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Healing,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Injuries & Sidelined",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            detail.sidelined.forEachIndexed { index, injury ->
                                InjuryTimelineItem(
                                    injury = injury,
                                    isLast = index == detail.sidelined.lastIndex
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
private fun PremiumBioChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GlassGlowGreen.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PlayerStatsCard(ps: com.footballpluse.footballapp.domain.model.PlayerStatDetail) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = ps.team.logo,
                        contentDescription = ps.team.name,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            ps.team.name ?: "—",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        // v3 player stats have no league info — show appearances instead of an empty line
                        Text(
                            ps.league.name.ifBlank { "${ps.appearances} apps" },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
                // Beautiful rating pill
                val ratingVal = ps.rating?.toFloatOrNull() ?: 0f
                val ratingColor = when {
                    ratingVal >= 7.5f -> GlassGlowGreen
                    ratingVal >= 6.8f -> Color(0xFFFFC107)
                    ratingVal > 0f -> Color(0xFFF44336)
                    else -> TextSecondary
                }
                if (ps.rating != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ratingColor.copy(alpha = 0.15f))
                            .border(1.dp, ratingColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ps.rating,
                            color = ratingColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(
                    label = "Matches Played",
                    value = "${ps.appearances}",
                    icon = Icons.Rounded.Star,
                    color = GlassGlowGreen
                )
                StatPill(
                    label = "Goals Scored",
                    value = "${ps.goals}",
                    icon = Icons.Rounded.EmojiEvents,
                    color = Color(0xFFFFC107)
                )
                StatPill(
                    label = "Assists Offered",
                    value = "${ps.assists}",
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    color = Color(0xFF03A9F4)
                )
            }
        }
    }
}

@Composable
private fun RowScope.StatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(0.5.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun TrophyCard(trophy: com.footballpluse.footballapp.domain.model.PlayerTrophyInfo) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(110.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC107).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = trophy.place.uppercase(),
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
            Column {
                Text(
                    text = trophy.league,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${trophy.season} • ${trophy.country}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InjuryTimelineItem(
    injury: com.footballpluse.footballapp.domain.model.PlayerInjuryInfo,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF44336).copy(alpha = 0.15f))
                    .border(2.dp, Color(0xFFF44336), CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(55.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFF44336), Color.White.copy(alpha = 0.05f))
                            )
                        )
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = injury.type,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            val dateText = buildString {
                append(injury.start)
                injury.end?.let { append(" → $it") }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = dateText,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerVisualAnalyticsSection(detail: com.footballpluse.footballapp.domain.model.PlayerDetail) {
    // Position comes from the API (get_players player_type). No name-based guessing.
    val position = detail.info.type?.removeSuffix("s")?.takeIf { it.isNotBlank() } ?: "Player"
    val rating = detail.stats.firstOrNull()?.rating?.toFloatOrNull()
    val stat = detail.stats.firstOrNull()

    if (stat == null) return

    // Radar axes show REAL season totals from the API (no invented attribute scores).
    val axisLabels = listOf("Goals", "Assists", "Shots", "Passes", "Dribbles", "Defending")
    val axisMax = listOf(40f, 25f, 120f, 3000f, 150f, 150f)
    val axisValues = listOf(
        stat.goals.toFloat(),
        stat.assists.toFloat(),
        stat.shotsTotal.toFloat(),
        stat.passesTotal.toFloat(),
        stat.dribblesAttempts.toFloat(),
        (stat.tacklesTotal + stat.interceptions + stat.blocks).toFloat()
    )
    val attributes = remember(stat) {
        axisValues.mapIndexed { i, v -> ((v / axisMax[i]) * 100f).coerceIn(2f, 100f) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Rounded.Analytics,
                contentDescription = null,
                tint = GlassGlowGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Season Output",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Performance Radar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    buildString {
                        append(position)
                        rating?.let { append("  -  Rating: "); append(String.format("%.1f", it)) }
                        append("  -  ")
                        append(stat.appearances)
                        append(" apps")
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center =
                            androidx.compose.ui.geometry.Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val radius = minOf(canvasWidth, canvasHeight) * 0.38f

                        val skeletonSteps = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                        skeletonSteps.forEach { step ->
                            val path = Path()
                            for (i in 0..5) {
                                val angle = i * 2 * kotlin.math.PI / 6 - kotlin.math.PI / 2
                                val r = radius * step
                                val px = (center.x + r * kotlin.math.cos(angle)).toFloat()
                                val py = (center.y + r * kotlin.math.sin(angle)).toFloat()
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            path.close()
                            drawPath(
                                path = path,
                                color = Color.White.copy(alpha = 0.08f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        for (i in 0..5) {
                            val angle = i * 2 * kotlin.math.PI / 6 - kotlin.math.PI / 2
                            val px = (center.x + radius * kotlin.math.cos(angle)).toFloat()
                            val py = (center.y + radius * kotlin.math.sin(angle)).toFloat()
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = center,
                                end = androidx.compose.ui.geometry.Offset(px, py),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val attrPath = Path()
                        for (i in 0..5) {
                            val angle = i * 2 * kotlin.math.PI / 6 - kotlin.math.PI / 2
                            val attrVal = attributes[i] / 100f
                            val r = radius * attrVal
                            val px = (center.x + r * kotlin.math.cos(angle)).toFloat()
                            val py = (center.y + r * kotlin.math.sin(angle)).toFloat()
                            if (i == 0) attrPath.moveTo(px, py) else attrPath.lineTo(px, py)
                        }
                        attrPath.close()

                        drawPath(
                            path = attrPath,
                            color = GlassGlowGreen.copy(alpha = 0.20f)
                        )
                        drawPath(
                            path = attrPath,
                            color = GlassGlowGreen,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        for (i in 0..5) {
                            val angle = i * 2 * kotlin.math.PI / 6 - kotlin.math.PI / 2
                            val attrVal = attributes[i] / 100f
                            val r = radius * attrVal
                            val px = (center.x + r * kotlin.math.cos(angle)).toFloat()
                            val py = (center.y + r * kotlin.math.sin(angle)).toFloat()
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(px, py)
                            )
                        }
                    }

                    Box(Modifier.fillMaxSize()) {
                        val labelAlignments = listOf(
                            Alignment.TopCenter,
                            Alignment.TopEnd,
                            Alignment.BottomEnd,
                            Alignment.BottomCenter,
                            Alignment.BottomStart,
                            Alignment.TopStart
                        )
                        labelAlignments.forEachIndexed { i, align ->
                            Text(
                                axisLabels[i] + "\n(" + ("%g".format(axisValues[i])) + ")",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .align(align)
                                    .padding(2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
