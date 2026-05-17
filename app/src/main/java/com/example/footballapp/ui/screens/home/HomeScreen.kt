package com.example.footballapp.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.viewmodel.HomeUiState
import com.example.footballapp.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFixtures: () -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMatchDetails: (String) -> Unit
) {
    val uiState by viewModel.homeState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Football Plus",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(end = 48.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open Menu */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF00FF57)
                    )
                }
                is HomeUiState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        featuredMatches = state.featuredMatches,
                        isLive = state.isLive,
                        onMatchClick = { fixtureId -> onNavigateToMatchDetails(fixtureId.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    featuredMatches: List<FixtureResponse>,
    isLive: Boolean,
    onMatchClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // For You Section
        item {
            Text(
                text = "For You",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Main Carousel (Simulated with LazyRow for now)
        item {
            StackedMatchCards(
                matches = featuredMatches,
                isLive = isLive,
                modifier = Modifier.padding(horizontal = 16.dp),
                onMatchClick = onMatchClick
            )
        }

        // Section: Continue Watching
        item {
            Text(
                text = "Continue Watching",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { // Dummy items
                    ContinueWatchingCard()
                }
            }
        }
        
        // Section: Leagues
        item {
            Text(
                text = "Popular Leagues",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { // Dummy items
                    LeagueSmallCard()
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun StackedMatchCards(
    matches: List<FixtureResponse>,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    onMatchClick: (Int) -> Unit
) {
    if (matches.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        var currentIndex by remember(matches) { mutableIntStateOf(0) }
        val dragOffsetX = remember { Animatable(0f) }
        val incomingPrevOffsetX = remember { Animatable(0f) }
        var incomingPrevIndex by remember { mutableIntStateOf(-1) }
        val scope = rememberCoroutineScope()
        val swipeOutDistance = constraints.maxWidth.toFloat() * 0.9f
        val swipeThreshold = constraints.maxWidth.toFloat() * 0.2f
        var totalDragX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            val currentCard = matches[currentIndex]
            val previousCard = if (matches.size > 1) matches[(currentIndex - 1 + matches.size) % matches.size] else null
            val nextCard = if (matches.size > 1) matches[(currentIndex + 1) % matches.size] else null
            val nextNextCard = if (matches.size > 2) matches[(currentIndex + 2) % matches.size] else null

            if (previousCard != null) {
                MatchCarouselCard(
                    fixture = previousCard,
                    isLive = isLive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = (-18).dp)
                        .graphicsLayer {
                            scaleX = 0.975f
                            scaleY = 0.975f
                            alpha = 0.58f
                        },
                    onClick = { previousCard.fixture?.id?.let(onMatchClick) }
                )
            }

            if (nextNextCard != null) {
                MatchCarouselCard(
                    fixture = nextNextCard,
                    isLive = isLive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = 30.dp)
                        .graphicsLayer {
                            scaleX = 0.95f
                            scaleY = 0.95f
                            alpha = 0.55f
                        },
                    onClick = { nextNextCard.fixture?.id?.let(onMatchClick) }
                )
            }

            if (nextCard != null) {
                MatchCarouselCard(
                    fixture = nextCard,
                    isLive = isLive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = 16.dp)
                        .graphicsLayer {
                            scaleX = 0.98f
                            scaleY = 0.98f
                            alpha = 0.74f
                        },
                    onClick = { nextCard.fixture?.id?.let(onMatchClick) }
                )
            }

            MatchCarouselCard(
                fixture = currentCard,
                isLive = isLive,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                    .pointerInput(currentIndex, matches.size) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                if (dragAmount.x < 0f) {
                                    // Next card: current card moves left as before.
                                    scope.launch { dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x) }
                                } else {
                                    // Previous card: keep current card fixed.
                                    scope.launch { dragOffsetX.snapTo(0f) }
                                }
                            },
                            onDragEnd = {
                                val currentDrag = totalDragX
                                scope.launch {
                                    when {
                                        currentDrag <= -swipeThreshold -> {
                                            dragOffsetX.animateTo(-swipeOutDistance, tween(220))
                                            currentIndex = (currentIndex + 1) % matches.size
                                            dragOffsetX.snapTo(0f)
                                        }
                                        currentDrag >= swipeThreshold -> {
                                            // Previous card comes in; current card stays fixed.
                                            val previousIndex = (currentIndex - 1 + matches.size) % matches.size
                                            incomingPrevIndex = previousIndex
                                            incomingPrevOffsetX.snapTo(-swipeOutDistance)
                                            incomingPrevOffsetX.animateTo(0f, tween(240))
                                            currentIndex = previousIndex
                                            incomingPrevIndex = -1
                                        }
                                        else -> {
                                            dragOffsetX.animateTo(0f, tween(220))
                                        }
                                    }
                                    totalDragX = 0f
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dragOffsetX.animateTo(0f, tween(220))
                                    totalDragX = 0f
                                }
                            }
                        )
                    }
                    .graphicsLayer {
                        val progress = (abs(dragOffsetX.value) / swipeThreshold).coerceIn(0f, 1f)
                        scaleX = 1f - (progress * 0.03f)
                        scaleY = 1f - (progress * 0.03f)
                    },
                onClick = { currentCard.fixture?.id?.let(onMatchClick) }
            )

            if (incomingPrevIndex >= 0 && incomingPrevIndex < matches.size) {
                MatchCarouselCard(
                    fixture = matches[incomingPrevIndex],
                    isLive = isLive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(incomingPrevOffsetX.value.roundToInt(), 0) },
                    onClick = { matches[incomingPrevIndex].fixture?.id?.let(onMatchClick) }
                )
            }
        }
    }
}

@Composable
fun MatchCarouselCard(
    fixture: FixtureResponse,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .let {
                if (onClick != null) {
                    it.clickable(onClick = onClick)
                } else {
                    it
                }
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF221326)),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image (Stadium or League logo)
            AsyncImage(
                model = fixture.league?.logo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFDF2E63).copy(alpha = 0.42f),
                                Color(0xFF341B58).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.92f)
                            ),
                            startY = 140f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Live Indicator
                if (isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE ${fixture.fixture?.status?.elapsed}'",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Middle: Teams and Score
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamColumn(name = fixture.teams?.home?.name ?: "", logo = fixture.teams?.home?.logo ?: "")
                        
                        Text(
                            text = "${fixture.goals?.home ?: 0} : ${fixture.goals?.away ?: 0}",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        
                        TeamColumn(name = fixture.teams?.away?.name ?: "", logo = fixture.teams?.away?.logo ?: "")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = fixture.league?.name ?: "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${fixture.league?.country} • Football",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                // Bottom: Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamColumn(name: String, logo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = logo,
            contentDescription = name,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ContinueWatchingCard() {
    Column(modifier = Modifier.width(160.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2B2B2B))
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(20.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Match Highlights",
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "15m left",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun LeagueSmallCard() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder for league logo
        Text(text = "L", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
