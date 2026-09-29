package com.footballpluse.footballapp.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.viewmodel.ModelStatsUiState
import com.footballpluse.footballapp.viewmodel.TrackPickUi
import com.footballpluse.footballapp.viewmodel.TrackSummaryUi

private val CardBg = Color(0xFF131620)
private val CardBorder = Color(0xFF1A1E2A)
private val AccentGreen = Color(0xFF00E676)
private val AccentRed = Color(0xFFEF4444)
private val AccentGold = Color(0xFFFFC107)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)
private val TextTertiary = Color(0xFF555555)

/**
 * Model tab: the FootballCharts prediction model's own published track record —
 * lifetime hit rate and P/L, per-market breakdown, calibration accuracy and the
 * most recent graded picks. Everything here comes straight from /track-record/.
 */
@Composable
fun ModelTabContent(state: ModelStatsUiState) {
    when (state) {
        is ModelStatsUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGreen)
            }
        }
        is ModelStatsUiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = state.message,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        is ModelStatsUiState.Success -> ModelContent(state)
        else -> {}
    }
}

@Composable
private fun ModelContent(data: ModelStatsUiState.Success) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SummaryCard(data.summary, data.signalsOnly, data.pending) }
        item { MarketsCard("SIGNAL PICKS", data.markets, "the model's confident selections") }
        if (data.marketsAll.isNotEmpty()) {
            item { MarketsCard("ALL RANKED PICKS", data.marketsAll, "every pick the model graded") }
        }
        if (data.calibration.isNotEmpty()) {
            item { CalibrationCard(data) }
        }
        if (data.recentPicks.isNotEmpty()) {
            item {
                Text("RECENT PICKS", color = TextTertiary, fontSize = 11.sp, letterSpacing = 1.sp)
            }
            items(data.recentPicks) { pick -> PickRow(pick) }
        }
    }
}

@Composable
private fun SummaryCard(s: TrackSummaryUi, signalsOnly: Boolean, pending: Int?) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MODEL TRACK RECORD", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (signalsOnly) {
                    Text("signals only", color = TextTertiary, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell("${s.n}", "picks graded")
                StatCell("%.1f%%".format(s.hitRate * 100), "hit rate")
                StatCell(
                    "%+.1f".format(s.pl).let { v -> if (s.pl >= 0) v else v },
                    "units P/L",
                    valueColor = if (s.pl >= 0) AccentGreen else AccentRed
                )
                pending?.let { StatCell("$it", "pending") }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${s.won} won \u00b7 ${s.lost} lost \u00b7 ${s.void} void \u2014 every graded pick since launch, no cherry-picking",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, valueColor: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextTertiary, fontSize = 9.sp)
    }
}

@Composable
private fun MarketsCard(title: String, markets: List<com.footballpluse.footballapp.viewmodel.TrackMarketUi>, subtitle: String) {
    if (markets.isEmpty()) return
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = TextTertiary, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(subtitle, color = TextTertiary, fontSize = 9.sp)
            Spacer(Modifier.height(10.dp))
            markets.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(m.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("${m.n} picks", color = TextTertiary, fontSize = 9.sp)
                    }
                    Text("%.1f%%".format(m.hitRate * 100), color = if (m.hitRate >= 0.5) AccentGreen else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "%+.1f".format(m.pl),
                        color = if (m.pl >= 0) AccentGreen else AccentRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun CalibrationCard(data: ModelStatsUiState.Success) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("CALIBRATION", color = TextTertiary, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(
                text = data.brierScore?.let { "Brier score %.3f over %s picks \u2014 lower is better".format(it, data.calibrationN ?: 0) }
                    ?: "how often each probability band hits",
                color = TextSecondary, fontSize = 10.sp
            )
            Spacer(Modifier.height(10.dp))
            data.calibration.forEach { b ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%.0f\u2013%.0f%%".format(b.lo * 100, b.hi * 100),
                        color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(58.dp)
                    )
                    // Predicted vs actual: two thin bars.
                    Column(Modifier.weight(1f)) {
                        MiniBar(fraction = b.avgProb.toFloat(), color = AccentGold)
                        Spacer(Modifier.height(2.dp))
                        MiniBar(fraction = b.hitRate.toFloat(), color = AccentGreen)
                    }
                    Text(
                        "%.0f%%".format(b.hitRate * 100),
                        color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.End
                    )
                    Text("${b.n}", color = TextTertiary, fontSize = 9.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("gold = predicted \u00b7 green = actual \u00b7 n on the right", color = TextTertiary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MiniBar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(CardBorder, RoundedCornerShape(3.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(5.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun PickRow(p: TrackPickUi) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val outcomeColor = when (p.outcome) {
                "won" -> AccentGreen
                "lost" -> AccentRed
                "void" -> TextTertiary
                else -> AccentGold
            }
            Text(
                text = (p.outcome ?: "?").uppercase(),
                color = outcomeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "${p.homeTeam} vs ${p.awayTeam}",
                    color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1
                )
                Text(
                    listOfNotNull(
                        p.market,
                        p.side.takeIf { it.isNotBlank() }?.let { s -> p.line?.let { l -> "$s $l" } ?: s },
                        p.modelProb?.let { "%.0f%%".format(it * 100) }
                    ).joinToString(" \u00b7 "),
                    color = TextSecondary, fontSize = 10.sp, maxLines = 1
                )
                Text(
                    listOfNotNull(p.league.takeIf { it.isNotBlank() }, p.date.takeIf { it.isNotBlank() }).joinToString(" \u00b7 "),
                    color = TextTertiary, fontSize = 9.sp, maxLines = 1
                )
            }
            p.pl?.let {
                Text(
                    "%+.1f".format(it),
                    color = if (it >= 0) AccentGreen else AccentRed,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
