package com.footballpluse.footballapp.ui.screens.onboarding.flow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.footballpluse.footballapp.ui.screens.onboarding.Club
import com.footballpluse.footballapp.ui.screens.onboarding.League
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingEvent
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingUiState
import com.footballpluse.footballapp.ui.theme.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ClubsScreen(
    state: OnboardingUiState,
    events: SharedFlow<OnboardingEvent>,
    leagues: List<League>,
    onClubToggled: (Club) -> Unit,
    onContinue: () -> Unit,
    getClubsForLeague: (String) -> List<Club>,
    mode: String = "first",
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedTabLeagueId by remember(state.selectedLeague?.id, leagues) {
        mutableStateOf(state.selectedLeague?.id ?: leagues.firstOrNull()?.id ?: "152")
    }
    var searchQuery by remember { mutableStateOf("") }

    val bgColor = if (isDark) Color(0xFF0D0D0D) else LightBackground
    val textPrimary = if (isDark) Color.White else LightTextPrimary
    val textSecondary = if (isDark) TextMutedGray else LightTextSecondary
    val textMuted = if (isDark) Color(0xFF555555) else LightTextMuted
    val cardBg = if (isDark) Color(0xFF161616) else LightSurface
    val searchBg = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F2F5)
    val searchBorder = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE1E4E8)
    val tabInactiveBg = if (isDark) Color(0xFF1A1A1A) else Color(0xFFE8EAED)
    val tabInactiveText = if (isDark) Color(0xFF888888) else Color(0xFF57606A)
    val inputPlaceholder = if (isDark) Color(0xFF555555) else LightTextMuted
    val selectedCardBg = if (isDark) CardBackgroundSelected else Color(0xFFE8F5E9)
    val badgeBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8EAED)

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is OnboardingEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    val allClubsForTab = getClubsForLeague(selectedTabLeagueId)
    val filteredClubs = if (searchQuery.isBlank()) {
        allClubsForTab
    } else {
        allClubsForTab.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedCount = state.selectedClubs.size

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append("Pick Your ")
                        }
                        withStyle(style = SpanStyle(color = GreenAccent, fontWeight = FontWeight.Bold)) {
                            append("Clubs")
                        }
                    },
                    fontSize = 28.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select the teams you want to follow",
                    fontSize = 13.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search clubs...", color = inputPlaceholder, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = textMuted, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Close, contentDescription = "Clear",
                                tint = textMuted,
                                modifier = Modifier.size(18.dp).clickable { searchQuery = "" }
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenAccent,
                        unfocusedBorderColor = searchBorder,
                        cursorColor = GreenAccent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = searchBg,
                        unfocusedContainerColor = searchBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(leagues, key = { it.id }) { league ->
                        val isTabActive = league.id == selectedTabLeagueId
                        val tabBg by animateColorAsState(
                            targetValue = if (isTabActive) GreenAccent else tabInactiveBg,
                            animationSpec = tween(250), label = "tab-bg"
                        )
                        val tabTextColor by animateColorAsState(
                            targetValue = if (isTabActive) Color.Black else tabInactiveText,
                            animationSpec = tween(250), label = "tab-text"
                        )
                        val tabElevation = if (isTabActive) 6.dp else 0.dp

                        Card(
                            modifier = Modifier.height(48.dp).clickable {
                                selectedTabLeagueId = league.id
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = tabBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = tabElevation)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (league.logoUrl != null) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(league.logoUrl)
                                                .crossfade(200)
                                                .size(48, 48)
                                                .build(),
                                            contentDescription = league.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.SportsSoccer,
                                            contentDescription = null,
                                            tint = tabTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Text(
                                        text = league.name,
                                        color = tabTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = if (isTabActive) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (filteredClubs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SportsSoccer,
                                contentDescription = null,
                                tint = textMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) 
                                    "No clubs match \"$searchQuery\"" 
                                else 
                                    "No clubs found for this league",
                                color = textSecondary,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Clear search", color = GreenAccent)
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredClubs, key = { it.id }) { club ->
                            val selected = state.selectedClubs.any { it.id == club.id }

                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.02f else 1f,
                                animationSpec = tween(200), label = "card-scale"
                            )
                            val currentCardBg by animateColorAsState(
                                targetValue = if (selected) selectedCardBg else cardBg,
                                animationSpec = tween(250), label = "card-bg"
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth().height(164.dp).graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }.clickable { onClubToggled(club) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = currentCardBg),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.55f)
                                                .background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFF5F5F7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(club.logoUrl)
                                                    .crossfade(300).size(128)
                                                    .build(),
                                                contentDescription = club.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(56.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = club.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val leagueName = leagues.find { it.id == club.leagueId }?.name ?: ""
                                        if (leagueName.isNotEmpty()) {
                                            Text(
                                                text = leagueName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = textSecondary,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (selected) {
                                        Box(
                                            modifier = Modifier.padding(8.dp).size(24.dp)
                                                .clip(CircleShape).background(GreenAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    if (club.rank > 0 && club.rank <= 3) {
                                        val badgeColor = when (club.rank) {
                                            1 -> Color(0xFFFFD700)
                                            2 -> Color(0xFFC0C0C0)
                                            3 -> Color(0xFFCD7F32)
                                            else -> badgeBg
                                        }
                                        Box(
                                            modifier = Modifier.padding(8.dp).size(22.dp)
                                                .clip(CircleShape).background(badgeColor.copy(alpha = 0.9f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${club.rank}",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedCount club${if (selectedCount != 1) "s" else ""} selected",
                            color = textSecondary,
                            fontSize = 12.sp
                        )

                        if (selectedCount < 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(min 3)",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.selectedClubs, key = { it.id }) { club ->
                            Card(
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF0F2F5)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                if (club.logoUrl != null) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(club.logoUrl).crossfade(200).size(48)
                                            .build(),
                                        contentDescription = club.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(34.dp).padding(5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            val buttonEnabled = selectedCount >= 3 && !state.isLoading
            val buttonText = when {
                state.isLoading -> "Saving..."
                selectedCount >= 3 -> "Continue with $selectedCount club${if (selectedCount != 1) "s" else ""}"
                selectedCount > 0 -> "Select ${3 - selectedCount} more"
                else -> "Select at least 3 clubs"
            }

            OnboardingPrimaryButton(
                text = buttonText,
                enabled = buttonEnabled,
                loading = state.isLoading,
                containerColor = if (buttonEnabled) GreenAccent else DisabledButtonBg,
                contentColor = if (buttonEnabled) Color.Black else DisabledButtonText,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun ClubsScreenDarkPreview() {
    val dummyLeagues = listOf(
        League("39", "Premier League", "England", "", "https://media.api-sports.io/football/leagues/39.png"),
        League("140", "La Liga", "Spain", "", "https://media.api-sports.io/football/leagues/140.png"),
        League("135", "Serie A", "Italy", "", "https://media.api-sports.io/football/leagues/135.png"),
        League("78", "Bundesliga", "Germany", "", "https://media.api-sports.io/football/leagues/78.png"),
        League("61", "Ligue 1", "France", "", "https://media.api-sports.io/football/leagues/61.png"),
    )
    val dummyClubs = listOf(
        Club("50", "Manchester City", "39", "https://media.api-sports.io/football/teams/50.png", 1),
        Club("42", "Arsenal", "39", "https://media.api-sports.io/football/teams/42.png", 2),
    )
    FootballPlusTheme(darkTheme = true) {
        ClubsScreen(
            state = OnboardingUiState(selectedClubs = dummyClubs),
            events = kotlinx.coroutines.flow.MutableSharedFlow(),
            leagues = dummyLeagues,
            onClubToggled = {},
            onContinue = {},
            getClubsForLeague = { dummyClubs }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F2F5)
@Composable
fun ClubsScreenLightPreview() {
    val dummyLeagues = listOf(
        League("39", "Premier League", "England", "", "https://media.api-sports.io/football/leagues/39.png"),
        League("140", "La Liga", "Spain", "", "https://media.api-sports.io/football/leagues/140.png"),
    )
    val dummyClubs = listOf(
        Club("50", "Manchester City", "39", "https://media.api-sports.io/football/teams/50.png", 1),
        Club("42", "Arsenal", "39", "https://media.api-sports.io/football/teams/42.png", 2),
    )
    FootballPlusTheme(darkTheme = false) {
        ClubsScreen(
            state = OnboardingUiState(selectedClubs = dummyClubs),
            events = kotlinx.coroutines.flow.MutableSharedFlow(),
            leagues = dummyLeagues,
            onClubToggled = {},
            onContinue = {},
            getClubsForLeague = { dummyClubs }
        )
    }
}
