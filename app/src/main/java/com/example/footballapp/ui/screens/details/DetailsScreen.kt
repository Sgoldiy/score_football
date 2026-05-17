package com.example.footballapp.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.model.Prediction
import com.example.footballapp.data.repository.FixtureDetailData
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.viewmodel.FixtureDetailViewModel

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
        topBar = {
            TopAppBar(
                title = { Text("Match Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        if (id == null) {
            CenterMessage("Invalid fixture id", paddingValues)
            return@Scaffold
        }

        when (val current = state) {
            ApiResult.Loading -> CenterLoading(paddingValues)
            is ApiResult.Error -> CenterMessage(current.message, paddingValues)
            is ApiResult.Success -> DetailContent(current.data, paddingValues)
        }
    }
}

@Composable
private fun CenterLoading(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun CenterMessage(message: String, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = Color.White)
    }
}

@Composable
private fun DetailContent(data: FixtureDetailData, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MatchHeader(data.fixture)
        }

        item {
            SectionCard("Quick Summary") {
                SummaryRow("Events", data.events.size.toString())
                SummaryRow("Lineups", data.lineups.size.toString())
                SummaryRow("Team Stats", data.statistics.size.toString())
                SummaryRow("Player Stats", data.playerStats.size.toString())
                SummaryRow("Predictions", data.predictions.size.toString())
                SummaryRow("Odds", data.odds.size.toString())
                SummaryRow("Injuries", data.injuries.size.toString())
                SummaryRow("Head to Head", data.headToHead.size.toString())
            }
        }

        item {
            MatchInfoSection(data.fixture)
        }

        item {
            PredictionSection(data.predictions.firstOrNull())
        }

        if (data.headToHead.isNotEmpty()) {
            item {
                SectionCard("Recent Head to Head") {
                    data.headToHead.take(5).forEach { match ->
                        val home = match.teams?.home?.name.orEmpty()
                        val away = match.teams?.away?.name.orEmpty()
                        val score = "${match.goals?.home ?: "-"} : ${match.goals?.away ?: "-"}"
                        SummaryRow("$home vs $away", score)
                    }
                }
            }
        }

        item {
            GenericListSection("Events", data.events)
        }

        item {
            GenericListSection("Lineups", data.lineups)
        }

        item {
            GenericListSection("Team Statistics", data.statistics)
        }

        item {
            GenericListSection("Player Statistics", data.playerStats)
        }

        item {
            GenericListSection("Odds", data.odds)
        }

        item {
            GenericListSection("Injuries", data.injuries)
        }

        if (data.errors.isNotEmpty()) {
            item {
                SectionCard("Unavailable Sections") {
                    data.errors.forEach { (section, error) ->
                        Text(
                            text = "• $section: $error",
                            color = Color(0xFFFFB3B3),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchHeader(fixture: FixtureResponse?) {
    SectionCard("Overview") {
        val homeName = fixture?.teams?.home?.name ?: "Home"
        val awayName = fixture?.teams?.away?.name ?: "Away"
        val score = "${fixture?.goals?.home ?: "-"} : ${fixture?.goals?.away ?: "-"}"
        val status = fixture?.fixture?.status?.long ?: "Status unavailable"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamMini(homeName, fixture?.teams?.home?.logo)
            Text(
                text = score,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            TeamMini(awayName, fixture?.teams?.away?.logo)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = status, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun TeamMini(name: String, logo: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = logo, contentDescription = name, modifier = Modifier.size(40.dp))
        Text(
            text = name,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MatchInfoSection(fixture: FixtureResponse?) {
    SectionCard("Match Information") {
        SummaryRow("League", fixture?.league?.name ?: "-")
        SummaryRow("Country", fixture?.league?.country ?: "-")
        SummaryRow("Round", fixture?.league?.round ?: "-")
        SummaryRow("Venue", fixture?.fixture?.venue?.name ?: "-")
        SummaryRow("City", fixture?.fixture?.venue?.city ?: "-")
        SummaryRow("Referee", fixture?.fixture?.referee ?: "-")
        SummaryRow("Date", fixture?.fixture?.date ?: "-")
    }
}

@Composable
private fun PredictionSection(prediction: Prediction?) {
    SectionCard("Predictions") {
        if (prediction == null) {
            Text(text = "No prediction data", color = Color.White.copy(alpha = 0.7f))
            return@SectionCard
        }

        SummaryRow("Winner", prediction.predictions?.winner?.name ?: "-")
        SummaryRow("Comment", prediction.predictions?.winner?.comment ?: "-")
        SummaryRow("Home Win %", prediction.predictions?.percent?.home ?: "-")
        SummaryRow("Draw %", prediction.predictions?.percent?.draw ?: "-")
        SummaryRow("Away Win %", prediction.predictions?.percent?.away ?: "-")
        SummaryRow("Under/Over", prediction.predictions?.under_over ?: "-")
    }
}

@Composable
private fun GenericListSection(title: String, values: List<*>) {
    SectionCard(title) {
        if (values.isEmpty()) {
            Text(text = "No data", color = Color.White.copy(alpha = 0.7f))
            return@SectionCard
        }

        values.take(5).forEachIndexed { index, entry ->
            Text(
                text = "${index + 1}. ${entry.compactDisplay()}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (values.size > 5) {
            Text(
                text = "...and ${values.size - 5} more",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun Any?.compactDisplay(): String {
    val raw = when (this) {
        null -> "-"
        is Map<*, *> -> this.entries.take(6).joinToString { "${it.key}: ${it.value}" }
        is List<*> -> this.take(6).joinToString()
        else -> toString()
    }
    return raw.replace("\n", " ").take(220)
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(modifier = Modifier.height(6.dp))
}
