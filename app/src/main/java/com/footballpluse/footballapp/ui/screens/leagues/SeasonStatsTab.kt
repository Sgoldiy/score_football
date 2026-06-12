package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SeasonStatsTab(seasonStats: SeasonStatsUiModel?) {
    if (seasonStats == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4ADE80))
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeasonAtAGlanceRow(seasonStats)

            SectionLabel("WHEN ARE GOALS SCORED?")
            GoalsByMinuteChart(seasonStats.goalsByMinuteBand)

            SectionLabel("TOP TEAMS")
            TopTeamsRow(seasonStats.bestAttack, seasonStats.bestDefense)

            SectionLabel("HOME vs AWAY")
            HomeAwayBreakdown(
                homePct = seasonStats.homeWinPct,
                awayPct = seasonStats.awayWinPct,
                drawPct = seasonStats.drawPct
            )

            SectionLabel("FORM TABLE (LAST 5 MATCHDAYS)")
            FormTable(seasonStats.formTable)
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        letterSpacing = 0.08.sp,
        color = Color(0xFFA0A0A0),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun SeasonAtAGlanceRow(stats: SeasonStatsUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlanceCard("Total Goals", "${stats.totalGoals}")
        GlanceCard("Avg Goals/Game", String.format("%.1f", stats.avgGoalsPerGame))
        GlanceCard("Common Score", stats.mostCommonScoreline)
        GlanceCard("Red Cards", "${stats.totalRedCards}")
        GlanceCard("Yellow Cards", "${stats.totalYellowCards}")
        if (stats.biggestWin.isNotBlank()) {
            GlanceCard("Biggest Win", stats.biggestWin)
        }
    }
}

@Composable
private fun GlanceCard(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4ADE80)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFFA0A0A0)
            )
        }
    }
}

@Composable
fun GoalsByMinuteChart(bands: List<GoalBand>) {
    val maxCount = bands.maxOfOrNull { it.count } ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "When are goals scored?",
                fontSize = 11.sp,
                letterSpacing = 0.08.sp,
                color = Color(0xFFA0A0A0)
            )
            Spacer(Modifier.height(12.dp))

            val animatedProgresses = bands.map { band ->
                animateFloatAsState(
                    targetValue = if (maxCount > 0) band.count.toFloat() / maxCount else 0f,
                    animationSpec = tween(600),
                    label = "bar-${band.label}"
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val barCount = bands.size
                val totalWidth = size.width
                val barGap = 8.dp.toPx()
                val barWidth = (totalWidth - barGap * (barCount - 1)) / barCount
                val chartHeight = size.height - 30f

                bands.forEachIndexed { index, band ->
                    val progress = animatedProgresses[index].value
                    val barHeight = chartHeight * progress
                    val x = index * (barWidth + barGap)
                    val y = size.height - 20f - barHeight

                    val isMax = band.count == maxCount && maxCount > 0
                    val barColor = if (isMax) Color(0xFF4ADE80) else Color(0xFF4ADE80).copy(alpha = 0.5f)

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "${band.count}",
                        x + barWidth / 2f,
                        y - 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#A0A0A0")
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        band.label,
                        x + barWidth / 2f,
                        size.height - 2f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#555555")
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TopTeamsRow(bestAttack: Pair<String, Int>, bestDefense: Pair<String, Int>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242424))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Best Attack",
                    fontSize = 11.sp,
                    letterSpacing = 0.08.sp,
                    color = Color(0xFFA0A0A0)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bestAttack.first,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${bestAttack.second} goals scored",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4ADE80)
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242424))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Best Defense",
                    fontSize = 11.sp,
                    letterSpacing = 0.08.sp,
                    color = Color(0xFFA0A0A0)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bestDefense.first,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${bestDefense.second} goals conceded",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4ADE80)
                )
            }
        }
    }
}

@Composable
fun HomeAwayBreakdown(homePct: Float, awayPct: Float, drawPct: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BreakdownRow(label = "Home Wins", pct = homePct, color = Color(0xFF4ADE80))
            BreakdownRow(label = "Away Wins", pct = awayPct, color = Color(0xFF4ADE80).copy(alpha = 0.6f))
            BreakdownRow(label = "Draws", pct = drawPct, color = Color(0xFF555555))
        }
    }
}

@Composable
private fun BreakdownRow(label: String, pct: Float, color: Color) {
    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(600),
        label = "breakdown-$label"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = Color(0xFFA0A0A0))
            Text("${(pct * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedPct.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun FormTable(formTable: FormTableData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "HOT STREAKS",
                fontSize = 11.sp,
                letterSpacing = 0.08.sp,
                color = Color(0xFFA0A0A0)
            )
            formTable.inForm.take(5).forEach { row ->
                FormTableRow(row, isHot = true)
            }

            HorizontalDivider(color = Color(0xFF1E1E1E), thickness = 1.dp)
            Spacer(Modifier.height(4.dp))

            Text(
                text = "COLD STREAKS",
                fontSize = 11.sp,
                letterSpacing = 0.08.sp,
                color = Color(0xFFA0A0A0)
            )
            formTable.outOfForm.take(5).forEach { row ->
                FormTableRow(row, isHot = false)
            }
        }
    }
}

@Composable
private fun FormTableRow(row: FormTeamRow, isHot: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.teamName,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.width(60.dp)
        ) {
            val results = row.form.takeLast(5).padStart(5, 'U')
            results.forEach { result ->
                val color = when (result) {
                    'W' -> Color(0xFF4ADE80)
                    'D' -> Color(0xFF555555)
                    'L' -> Color(0xFFEF4444)
                    else -> Color(0xFF1E1E1E)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }

        Text(
            text = "${row.pointsGained} pts",
            modifier = Modifier.width(44.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isHot) Color(0xFF4ADE80) else Color(0xFFEF4444),
            textAlign = TextAlign.End
        )
    }
}
