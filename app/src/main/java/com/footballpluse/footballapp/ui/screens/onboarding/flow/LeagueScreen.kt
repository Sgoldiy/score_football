package com.footballpluse.footballapp.ui.screens.onboarding.flow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.ui.screens.onboarding.League
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingEvent
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingUiState
import com.footballpluse.footballapp.ui.theme.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LeagueScreen(
    state: OnboardingUiState,
    events: SharedFlow<OnboardingEvent>,
    greetingUsername: String,
    leagues: List<League>,
    onLeagueSelected: (League) -> Unit,
    onContinueClick: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is OnboardingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGrayBackground)
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
                            append("League")
                        }
                    },
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.onboarding_league_subtitle),
                    fontSize = 14.sp,
                    color = TextMutedGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    leagues.forEach { league ->
                        val selected = state.selectedLeague?.id == league.id

                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.02f else 1.0f,
                            animationSpec = tween(durationMillis = 200),
                            label = "card-scale"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable { onLeagueSelected(league) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) CardBackgroundSelected else CardBackgroundUnselected
                            ),
                            border = if (selected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, GreenAccent)
                            } else {
                                androidx.compose.foundation.BorderStroke(0.5.dp, CardBorderUnselected)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = league.name.take(2).uppercase()
                                    Text(initials, color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                    if (league.logoUrl != null) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(league.logoUrl)
                                                .crossfade(300)
                                                .size(80)
                                                .build(),
                                            contentDescription = league.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = league.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = league.country,
                                        fontSize = 12.sp,
                                        color = TextMutedGray
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (selected) GreenAccent else Color.Transparent)
                                        .then(
                                            if (!selected) Modifier.border(
                                                1.dp,
                                                CardBorderUnselected,
                                                RoundedCornerShape(11.dp)
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val buttonEnabled = state.selectedLeague != null
            OnboardingPrimaryButton(
                text = stringResource(id = R.string.onboarding_btn_continue),
                enabled = buttonEnabled,
                containerColor = if (buttonEnabled) GreenAccent else DisabledButtonBg,
                contentColor = if (buttonEnabled) Color.Black else DisabledButtonText,
                loading = state.isLoading,
                onClick = onContinueClick,
                modifier = Modifier.padding(top = 16.dp)
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
fun LeagueScreenPreview() {
    val dummyLeagues = listOf(
        League("39", "Premier League", "England", "\uD83C\uDFF4\uD83C\uDFE6\uD83C\uDFFD\u200D\uD83C\uDFF3\uFE0F\u200D\uD83C\uDFF4", "https://media.api-sports.io/football/leagues/39.png"),
        League("140", "La Liga", "Spain", "\uD83C\uDDEA\uD83C\uDDF8", "https://media.api-sports.io/football/leagues/140.png"),
        League("135", "Serie A", "Italy", "\uD83C\uDDEE\uD83C\uDDF9", "https://media.api-sports.io/football/leagues/135.png")
    )
    FootballPlusTheme(darkTheme = true) {
        LeagueScreen(
            state = OnboardingUiState(selectedLeague = dummyLeagues[0]),
            events = kotlinx.coroutines.flow.MutableSharedFlow(),
            greetingUsername = "striker",
            leagues = dummyLeagues,
            onLeagueSelected = {},
            onContinueClick = {},
            onNavigateNext = {}
        )
    }
}
