package com.example.footballapp.ui.screens.fixtures

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.footballapp.domain.model.Match
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.ui.components.MatchRow
import com.example.footballapp.ui.components.SectionHeader
import com.example.footballapp.ui.components.MatchRowShimmer
import com.example.footballapp.viewmodel.FixturesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FixturesScreen(
    onNavigateToMatchCenter: (String) -> Unit,
    viewModel: FixturesViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    val fixturesState by viewModel.fixturesState.collectAsState()

    LaunchedEffect(selectedDate) {
        viewModel.getFixturesByDate(selectedDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Date Rail
        DateRail(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        when (val state = fixturesState) {
            is ApiResult.Loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) { _ ->
                        MatchRowShimmer()
                    }
                }
            }
            is ApiResult.Success -> {
                val matches = state.data
                if (matches.isEmpty()) {
                    EmptyFixtures()
                } else {
                    FixturesList(matches, onNavigateToMatchCenter)
                }
            }
            is ApiResult.Error -> {
                ErrorView(message = state.message) {
                    viewModel.getFixturesByDate(selectedDate)
                }
            }
        }
    }
}

@Composable
fun DateRail(selectedDate: String, onDateSelected: (String) -> Unit) {
    val dates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        repeat(14) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val daySdf = SimpleDateFormat("EEE", Locale.getDefault())
    val dateSdf = SimpleDateFormat("dd", Locale.getDefault())

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = dates) { date ->
            val dateStr = sdf.format(date)
            val isSelected = dateStr == selectedDate
            
            DateItem(
                day = daySdf.format(date),
                date = dateSdf.format(date),
                isSelected = isSelected,
                onClick = { onDateSelected(dateStr) }
            )
        }
    }
}

@Composable
fun DateItem(day: String, date: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FixturesList(matches: List<Match>, onMatchClick: (String) -> Unit) {
    val grouped = matches.groupBy { it.league.name }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (league, leagueMatches) ->
            item {
                SectionHeader(title = league)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        leagueMatches.forEachIndexed { index, match ->
                            MatchRow(
                                match = match,
                                onClick = onMatchClick
                            )
                            if (index < leagueMatches.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
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
fun EmptyFixtures() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No matches scheduled for this date", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
