package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult
import java.util.Locale

private val CardBg = Color(0xFF161616)
private val CardBorder = Color(0xFF242424)
private val TrackColor = Color(0xFF242424)
private val AccentGreen = Color(0xFF4ADE80)
private val AccentGold = Color(0xFFFBBF24)
private val AccentRed = Color(0xFFF87171)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFA0A0A0)
private val TextTertiary = Color(0xFF777777)

/** Percentages below this are hidden as noise (e.g. a 0.15% title chance). */
private const val VISIBLE_PCT_THRESHOLD = 0.5f

/**
 * Monte Carlo season projection tab: for every team the model's title / top-4 /
 * relegation probabilities and the projected points range (p10-p90) from 10k
 * daily simulations served by the FootballCharts projection endpoint.
 */
@Composable
fun ProjectionTab(
    projection: ApiResult<ProjectionUiModel>,
    onTeamClick: (Int) -> Unit
) {
    when (projection) {
        is ApiResult.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGreen)
            }
        }
        is ApiResult.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = projection.message ?: "Projection unavailable",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        is ApiResult.Success -> ProjectionContent(projection.data, onTeamClick)
    }
}

@Composable
private fun ProjectionContent(data: ProjectionUiModel, onTeamClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "proj_header") { ProjectionHeader(data) }
        items(data.rows, key = { "proj_${it.team.id}" }) { row ->
            ProjectionRowCard(row, onTeamClick)
        }
        item(key = "proj_footer") { ProjectionFootnote(data) }
    }
}

@Composable
private fun ProjectionHeader(data: ProjectionUiModel) {
    Column {
        Text(
            text = "SEASON PROJECTION",
            fontSize = 11.sp,
            letterSpacing = 0.08.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
        ) {
            Text(
                text = buildString {
                    append(data.nSims?.let { "%,d".format(it) } ?: "10,000")
                    append(" simulations of the remaining season")
                    data.seasonLabel?.let { append(" \u00b7 $it") }
                    data.expectedRemaining?.let { append(" \u00b7 $it matches left") }
                    if (data.partialSchedule) append(" \u00b7 partial schedule")
                },
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ProjectionRowCard(row: ProjectionRowUiModel, onTeamClick: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTeamClick(row.team.id) }
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.rank.toString(),
                    color = if (row.rank <= 4) AccentGreen else TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(22.dp)
                )
                TeamBadge(row.team)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.team.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Played ${row.played} \u00b7 ${row.pointsNow} pts now",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (row.meanPoints != null) {
                        Text(
                            text = String.format(Locale.US, "%.1f", row.meanPoints),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("proj. pts", color = TextTertiary, fontSize = 9.sp)
                    }
                }
            }

            if (row.p10Points != null && row.p90Points != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Projected range ${row.p10Points}\u2013${row.p90Points} pts",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 30.dp)
                )
            }

            val bars = buildList {
                row.titlePct?.let { if (it >= VISIBLE_PCT_THRESHOLD) add(Triple("Title", it, AccentGold)) }
                row.top4Pct?.let { if (it >= VISIBLE_PCT_THRESHOLD) add(Triple("Top 4", it, AccentGreen)) }
                row.relegationPct?.let { if (it >= VISIBLE_PCT_THRESHOLD) add(Triple("Relegation", it, AccentRed)) }
            }
            if (bars.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    bars.forEach { (label, pct, color) ->
                        ProbabilityBar(label, pct, color, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamBadge(team: TeamUiModel) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(TrackColor),
        contentAlignment = Alignment.Center
    ) {
        if (team.logo != null) {
            AsyncImage(
                model = team.logo,
                contentDescription = team.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = team.name.take(1).uppercase(),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProbabilityBar(label: String, pct: Float, color: Color, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = (pct / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "prob_$label"
    )
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(
                text = if (pct >= 10f) "${pct.toInt()}%" else String.format(Locale.US, "%.1f%%", pct),
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TrackColor)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ProjectionFootnote(data: ProjectionUiModel) {
    Text(
        text = "Updated daily by FootballCharts \u00b7 range = middle 80% of simulated seasons",
        color = TextTertiary,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}
