package com.example.footballapp.ui.screens.fixtures

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.ui.components.BroadcastMatchCard
import com.example.footballapp.ui.components.InfoPill
import com.example.footballapp.ui.components.PremiumCard
import com.example.footballapp.ui.components.SectionTitle
import com.example.footballapp.ui.components.ShimmerBlock
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.LiveGreen
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.TextSecondary
import com.example.footballapp.viewmodel.FixturesViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixturesScreen(
    onBackClick: () -> Unit,
    onNavigateToMatchDetails: (String) -> Unit,
    viewModel: FixturesViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val state by viewModel.fixturesState.collectAsState()

    LaunchedEffect(selectedDate) {
        viewModel.getFixturesByDate(selectedDate.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fixtures", fontWeight = FontWeight.Black)
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM")), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PitchBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF07150F), PitchBlack)))
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DateRail(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
            }

            when (val current = state) {
                ApiResult.Loading -> {
                    items(6) { ShimmerBlock(Modifier.fillMaxWidth().height(230.dp), RoundedCornerShape(24.dp)) }
                }
                is ApiResult.Error -> {
                    item {
                        PremiumCard(Modifier.fillMaxWidth()) {
                            Column {
                                Text("Fixtures unavailable", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text(current.message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is ApiResult.Success -> {
                    val matches = current.data
                    val featured = matches.sortedWith(
                        compareByDescending<FixtureResponse> { it.league?.id in setOf(39, 140, 135, 78, 61, 2, 3) }
                            .thenBy { it.fixture?.timestamp ?: Long.MAX_VALUE }
                    ).take(5)

                    item {
                        FixtureDaySummary(matches)
                    }

                    if (featured.isNotEmpty()) {
                        item {
                            SectionTitle("Featured fixtures", trailing = "${featured.size}")
                            Spacer(Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(featured, key = { it.fixture?.id ?: it.hashCode() }) { match ->
                                    BroadcastMatchCard(
                                        match = match,
                                        modifier = Modifier.fillParentMaxWidth(0.9f),
                                        expandedByDefault = true,
                                        onClick = { match.fixture?.id?.let { onNavigateToMatchDetails(it.toString()) } }
                                    )
                                }
                            }
                        }
                    }

                    matches.groupBy { it.league?.name ?: "Other fixtures" }.forEach { (leagueName, leagueMatches) ->
                        item {
                            SectionTitle(leagueName, trailing = "${leagueMatches.size} matches")
                        }
                        items(leagueMatches, key = { it.fixture?.id ?: it.hashCode() }) { match ->
                            BroadcastMatchCard(
                                match = match,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { match.fixture?.id?.let { onNavigateToMatchDetails(it.toString()) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateRail(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items((-3..3).map { LocalDate.now().plusDays(it.toLong()) }) { date ->
            val selected = date == selectedDate
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) LiveGreen else PitchSurfaceHigh)
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(date.format(DateTimeFormatter.ofPattern("EEE")), color = if (selected) PitchBlack else Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(date.dayOfMonth.toString(), color = if (selected) PitchBlack else Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun FixtureDaySummary(matches: List<FixtureResponse>) {
    val live = matches.count { it.fixture?.status?.short in setOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE") }
    val upcoming = matches.count { it.fixture?.status?.short in setOf("NS", "TBD") }
    val finished = matches.count { it.fixture?.status?.short in setOf("FT", "AET", "PEN") }
    PremiumCard(Modifier.fillMaxWidth(), brush = Brush.linearGradient(listOf(PitchSurfaceHigh, PitchSurface))) {
        Column {
            SectionTitle("Matchday pulse", trailing = "${matches.size} fixtures")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("Live $live", modifier = Modifier.weight(1f), accent = LiveGreen)
                InfoPill("Upcoming $upcoming", modifier = Modifier.weight(1f), accent = IceBlue)
                InfoPill("Final $finished", modifier = Modifier.weight(1f), accent = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Grouped by competition with featured fixtures, live status and match center access.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
