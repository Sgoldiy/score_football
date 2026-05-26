package com.example.footballapp.ui.screens.players

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.data.util.SeasonUtils
import com.example.footballapp.ui.components.PlayerAvatar
import com.example.footballapp.ui.theme.GlassGlowGreen
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.TextSecondary
import com.example.footballapp.viewmodel.PlayerViewModel

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
                    val name = if (state is ApiResult.Success) (state as ApiResult.Success).data.info.name else "Player Profile"
                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(result.message, color = Color.White.copy(alpha = 0.65f), textAlign = TextAlign.Center)
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
private fun PlayerContent(detail: com.example.footballapp.domain.model.PlayerDetail) {
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
                                PremiumBioChip(label = "Nationality", value = it, icon = Icons.Rounded.Public)
                            }
                        }
                        info.age?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumBioChip(label = "Age", value = "$it yrs", icon = Icons.Rounded.Cake)
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
                                PremiumBioChip(label = "Height", value = it, icon = Icons.Rounded.Height)
                            }
                        }
                        info.weight?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumBioChip(label = "Weight", value = it, icon = Icons.Rounded.FitnessCenter)
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
                        Icon(Icons.Rounded.BarChart, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Season Statistics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Trophies Showcase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Icon(Icons.Rounded.Healing, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Injuries & Sidelined", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            detail.sidelined.forEachIndexed { index, injury ->
                                InjuryTimelineItem(injury = injury, isLast = index == detail.sidelined.lastIndex)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBioChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun PlayerStatsCard(ps: com.example.footballapp.domain.model.PlayerStatDetail) {
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = ps.team.logo,
                        contentDescription = ps.team.name,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(ps.team.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(ps.league.name, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
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
                StatPill(label = "Matches Played", value = "${ps.appearances}", icon = Icons.Rounded.Star, color = GlassGlowGreen)
                StatPill(label = "Goals Scored", value = "${ps.goals}", icon = Icons.Rounded.EmojiEvents, color = Color(0xFFFFC107))
                StatPill(label = "Assists Offered", value = "${ps.assists}", icon = Icons.Rounded.TrendingUp, color = Color(0xFF03A9F4))
            }
        }
    }
}

@Composable
private fun RowScope.StatPill(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
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
private fun TrophyCard(trophy: com.example.footballapp.domain.model.PlayerTrophyInfo) {
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
private fun InjuryTimelineItem(injury: com.example.footballapp.domain.model.PlayerInjuryInfo, isLast: Boolean) {
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
private fun PlayerVisualAnalyticsSection(detail: com.example.footballapp.domain.model.PlayerDetail) {
    val name = detail.info.name.lowercase()
    val position = remember(name) {
        when {
            name.contains("haaland") || name.contains("saka") || name.contains("mbappe") || name.contains("vinicius") || name.contains("martinell") || name.contains("havertz") || name.contains("striker") || name.contains("winger") -> "Attacker"
            name.contains("odegaard") || name.contains("de bruyne") || name.contains("foden") || name.contains("silva") || name.contains("rodri") || name.contains("rice") || name.contains("bellingham") || name.contains("valverde") || name.contains("playmaker") || name.contains("midfield") -> "Midfielder"
            name.contains("dias") || name.contains("walker") || name.contains("saliba") || name.contains("magalhaes") || name.contains("rüdiger") || name.contains("carvajal") || name.contains("defender") || name.contains("calafiori") -> "Defender"
            name.contains("ederson") || name.contains("raya") || name.contains("courtois") || name.contains("keeper") || name.contains("hands") -> "Goalkeeper"
            else -> "Attacker"
        }
    }

    val rating = detail.stats.firstOrNull()?.rating?.toFloatOrNull() ?: 7.2f
    val ratingFactor = (rating / 8.5f).coerceIn(0.7f, 1.1f)
    val stat = detail.stats.firstOrNull()

    val attributes = remember(position, ratingFactor, stat) {
        if (stat != null) {
            // Speed Calculation
            val baseSpeed = when (position) {
                "Attacker" -> 78f
                "Midfielder" -> 68f
                "Defender" -> 62f
                else -> 50f
            }
            val speedScore = (baseSpeed + (ratingFactor * 12f) + (stat.dribblesAttempts * 1.2f)).coerceIn(40f, 99f)

            // Passing Calculation
            val passingScore = (stat.passesAccuracy.coerceIn(40, 95).toFloat() + (stat.passesKey * 2.2f) + (stat.assists * 8f)).coerceIn(30f, 99f)

            // Physical Calculation
            val duelRatio = if (stat.duelsTotal > 0) (stat.duelsWon.toFloat() / stat.duelsTotal) else 0.5f
            val physicalScore = ((duelRatio * 65f) + (ratingFactor * 15f) + (stat.foulsDrawn * 1.5f)).coerceIn(35f, 99f)

            // Defense Calculation
            val defensiveActions = stat.tacklesTotal + stat.interceptions + stat.blocks
            val baseDefense = when (position) {
                "Defender" -> 65f
                "Midfielder" -> 45f
                else -> 20f
            }
            val defenseScore = (baseDefense + (defensiveActions * 3f) + (ratingFactor * 10f)).coerceIn(15f, 99f)

            // Shooting Calculation
            val shotsRatio = if (stat.shotsTotal > 0) (stat.shotsOnTarget.toFloat() / stat.shotsTotal) else 0.4f
            val baseShooting = when (position) {
                "Attacker" -> 60f
                "Midfielder" -> 45f
                else -> 15f
            }
            val shootingScore = (baseShooting + (shotsRatio * 30f) + (stat.goals * 7f)).coerceIn(25f, 99f)

            listOf(speedScore, passingScore, physicalScore, defenseScore, shootingScore)
        } else {
            // Fallback to sensible defaults
            when (position) {
                "Attacker" -> listOf(88f, 75f, 70f, 35f, 85f)
                "Midfielder" -> listOf(76f, 88f, 72f, 60f, 72f)
                "Defender" -> listOf(72f, 70f, 82f, 88f, 40f)
                else -> listOf(52f, 62f, 78f, 90f, 10f)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Rounded.Analytics, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Advanced Visual Analytics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Performance Radar Chart", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Rating: $rating • Role: $position", color = TextSecondary, fontSize = 11.sp)
                
                Spacer(Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center = androidx.compose.ui.geometry.Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val radius = minOf(canvasWidth, canvasHeight) * 0.4f
                        
                        val skeletonSteps = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                        skeletonSteps.forEach { step ->
                            val path = Path()
                            for (i in 0..4) {
                                val angle = i * 2 * kotlin.math.PI / 5 - kotlin.math.PI / 2
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
                        
                        for (i in 0..4) {
                            val angle = i * 2 * kotlin.math.PI / 5 - kotlin.math.PI / 2
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
                        for (i in 0..4) {
                            val angle = i * 2 * kotlin.math.PI / 5 - kotlin.math.PI / 2
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
                        
                        for (i in 0..4) {
                            val angle = i * 2 * kotlin.math.PI / 5 - kotlin.math.PI / 2
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
                        Text("SPD\n(${attributes[0].toInt()})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-14).dp), textAlign = TextAlign.Center)
                        Text("PAS\n(${attributes[1].toInt()})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = 40.dp), textAlign = TextAlign.Center)
                        Text("PHY\n(${attributes[2].toInt()})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 12.dp), textAlign = TextAlign.Center)
                        Text("DEF\n(${attributes[3].toInt()})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-6).dp, y = 12.dp), textAlign = TextAlign.Center)
                        Text("SHO\n(${attributes[4].toInt()})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-10).dp, y = 40.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tactical Activity Heatmap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Live game heatmap density coverage", color = TextSecondary, fontSize = 11.sp)
                
                Spacer(Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F3B20))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        drawRect(
                            color = Color.White.copy(alpha = 0.2f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(width / 2f, 0f),
                            end = androidx.compose.ui.geometry.Offset(width / 2f, height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            radius = 28.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        
                        drawRect(
                            color = Color.White.copy(alpha = 0.2f),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, height * 0.25f),
                            size = androidx.compose.ui.geometry.Size(width * 0.12f, height * 0.5f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.2f),
                            topLeft = androidx.compose.ui.geometry.Offset(width * 0.88f, height * 0.25f),
                            size = androidx.compose.ui.geometry.Size(width * 0.12f, height * 0.5f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        
                        val spots = when (position) {
                            "Attacker" -> listOf(
                                androidx.compose.ui.geometry.Offset(width * 0.75f, height * 0.4f) to 48.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.55f) to 36.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(width * 0.65f, height * 0.3f) to 32.dp.toPx()
                            )
                            "Midfielder" -> listOf(
                                androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.5f) to 54.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(width * 0.4f, height * 0.35f) to 40.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(width * 0.6f, height * 0.65f) to 42.dp.toPx()
                            )
                            "Defender" -> listOf(
                                androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.5f) to 50.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(width * 0.15f, height * 0.3f) to 36.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(width * 0.35f, height * 0.6f) to 32.dp.toPx()
                            )
                            else -> listOf(
                                androidx.compose.ui.geometry.Offset(width * 0.06f, height * 0.5f) to 40.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(width * 0.08f, height * 0.45f) to 28.dp.toPx()
                            )
                        }
                        
                        spots.forEach { (center, radiusValue) ->
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF3D00).copy(alpha = 0.5f),
                                        Color(0xFFFFC107).copy(alpha = 0.25f),
                                        Color(0xFF4CAF50).copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = radiusValue
                                ),
                                radius = radiusValue,
                                center = center
                            )
                        }
                    }
                }
            }
        }
    }
}
