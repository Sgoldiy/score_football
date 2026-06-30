package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.viewmodel.League
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.ui.components.HeaderIcon
import com.footballpluse.footballapp.ui.components.MatchRowShimmer
import com.footballpluse.footballapp.viewmodel.LeagueFilter
import com.footballpluse.footballapp.viewmodel.LeagueSortOrder
import com.footballpluse.footballapp.viewmodel.LeagueTab
import com.footballpluse.footballapp.viewmodel.LeaguesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguesScreen(
    onNavigateToLeagueDetail: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    unreadNotificationCount: Int = 0,
    onBackClick: () -> Unit,
    viewModel: LeaguesViewModel = hiltViewModel()
) {
    val popularLeagues by viewModel.popularLeagues.collectAsState()
    val allLeagues by viewModel.allLeagues.collectAsState()
    val liveLeagues by viewModel.liveLeagues.collectAsState()
    val domesticLeagues by viewModel.domesticLeagues.collectAsState()
    val cupLeagues by viewModel.cupLeagues.collectAsState()
    val internationalLeagues by viewModel.internationalLeagues.collectAsState()
    val youthLeagues by viewModel.youthLeagues.collectAsState()
    val womenLeagues by viewModel.womenLeagues.collectAsState()

    val leaguesState by viewModel.leaguesState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val activeFilters by viewModel.activeFilters.collectAsState()
    val totalLiveCount by viewModel.totalLiveCount.collectAsState()

    var showFilter by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tempSort by viewModel.tempSortOrder.collectAsState()
    val tempFilters by viewModel.tempFilters.collectAsState()

    fun applyFiltersAndSort(
        list: List<League>,
        query: String,
        filters: LeagueFilter,
        sort: LeagueSortOrder
    ): List<League> {
        return list.filter { league ->
            val matchesSearch = if (query.isBlank()) true else {
                league.name.contains(query, ignoreCase = true) ||
                league.country.contains(query, ignoreCase = true)
            }
            val matchesTodayFilter = !filters.hasMatchesToday || league.todayCount > 0
            val matchesLiveFilter = !filters.hasLiveMatches || league.liveCount > 0
            val matchesFavFilter = !filters.favoritedOnly || league.isFavorited
            val matchesContinentFilter = filters.continents.isEmpty() || 
                getContinentForCountry(league.country) in filters.continents
            
            matchesSearch && matchesTodayFilter && matchesLiveFilter && matchesFavFilter && matchesContinentFilter
        }.sortedWith { l1, l2 ->
            when (sort) {
                LeagueSortOrder.ALPHABETICAL -> l1.name.compareTo(l2.name, ignoreCase = true)
                LeagueSortOrder.MOST_MATCHES_TODAY -> l2.todayCount.compareTo(l1.todayCount)
                LeagueSortOrder.LIVE_FIRST -> l2.liveCount.compareTo(l1.liveCount)
                LeagueSortOrder.FAVORITES_FIRST -> {
                    val f1 = if (l1.isFavorited) 1 else 0
                    val f2 = if (l2.isFavorited) 1 else 0
                    f2.compareTo(f1)
                }
            }
        }
    }

    val filteredPopularLeagues by remember {
        derivedStateOf { applyFiltersAndSort(popularLeagues, searchQuery, activeFilters, sortOrder) }
    }
    val filteredAllLeagues by remember {
        derivedStateOf { applyFiltersAndSort(allLeagues, searchQuery, activeFilters, sortOrder) }
    }
    val filteredLiveLeagues by remember {
        derivedStateOf { applyFiltersAndSort(liveLeagues, searchQuery, activeFilters, sortOrder) }
    }
    val filteredDomesticLeagues by remember {
        derivedStateOf { applyFiltersAndSort(domesticLeagues, searchQuery, activeFilters, sortOrder) }
    }
    val filteredCupLeagues by remember {
        derivedStateOf { applyFiltersAndSort(cupLeagues, searchQuery, activeFilters, sortOrder) }
    }
    val filteredInternationalLeagues by remember {
        derivedStateOf { applyFiltersAndSort(internationalLeagues, searchQuery, activeFilters, sortOrder) }
    }
    val filteredYouthLeagues by remember {
        derivedStateOf { applyFiltersAndSort(youthLeagues, searchQuery, activeFilters, sortOrder) }
    }
    val filteredWomenLeagues by remember {
        derivedStateOf { applyFiltersAndSort(womenLeagues, searchQuery, activeFilters, sortOrder) }
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row {
                    Text(
                        text = "All ",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Leagues",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
                Text(
                    text = "Explore competitions",
                    fontSize = 11.sp,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderIcon(
                    icon = Icons.Default.Search,
                    onClick = onNavigateToSearch
                )
                HeaderIcon(
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToNotifications,
                    badgeCount = unreadNotificationCount
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .background(Color(0xFF131620), RoundedCornerShape(12.dp))
                .border(0.5.dp, Color(0xFF1a1e2a), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF555555),
                    modifier = Modifier.size(18.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearch(it) },
                    textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search leagues, cups, countries…",
                                fontSize = 13.sp,
                                color = Color(0xFF444444)
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        viewModel.onOpenFilterSheet()
                        showFilter = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter and Sort",
                        tint = Color(0xFF00e676),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            items(LeagueTab.entries.toTypedArray()) { tab ->
                val isSelected = selectedTab == tab
                val label = when (tab) {
                    LeagueTab.POPULAR -> "⭐ Popular"
                    LeagueTab.ALL -> "All"
                    LeagueTab.LIVE -> "🔴 Live · $totalLiveCount"
                    LeagueTab.DOMESTIC -> "🏴 Domestic"
                    LeagueTab.CUPS -> "🏆 Cups"
                    LeagueTab.INTERNATIONAL -> "🌍 International"
                    LeagueTab.YOUTH -> "🎽 Youth"
                    LeagueTab.WOMEN -> "👩 Women's"
                }

                val activeBg = if (tab == LeagueTab.LIVE) Color(0xFF1a0a0a) else Color(0xFF00e676)
                val activeBorder = if (tab == LeagueTab.LIVE) Color(0xFF3a1212) else Color.Transparent
                val activeText = if (tab == LeagueTab.LIVE) Color(0xFFff4444) else Color(0xFF0d0f14)

                val inactiveBg = if (tab == LeagueTab.LIVE) Color(0xFF1a0a0a) else Color(0xFF131620)
                val inactiveBorder = if (tab == LeagueTab.LIVE) Color(0xFF3a1212) else Color(0xFF1a1e2a)
                val inactiveText = if (tab == LeagueTab.LIVE) Color(0xFFff4444) else Color(0xFF888888)

                val bg = if (isSelected) activeBg else inactiveBg
                val border = if (isSelected) activeBorder else inactiveBorder
                val text = if (isSelected) activeText else inactiveText

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(0.5.dp, border, RoundedCornerShape(12.dp))
                        .clickable { viewModel.onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tab == LeagueTab.LIVE) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFff4444))
                        )
                    }
                    Text(
                        text = label,
                        color = text,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        when (val state = leaguesState) {
            is ApiResult.Loading -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp)) {
                    items(10) { MatchRowShimmer() }
                }
            }
            is ApiResult.Success -> {
                when (selectedTab) {
                    LeagueTab.POPULAR -> PopularTabContent(
                        leagues = filteredPopularLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                    LeagueTab.ALL -> AllTabContent(
                        leagues = filteredAllLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                    LeagueTab.LIVE -> LiveTabContent(
                        leagues = filteredLiveLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                    LeagueTab.DOMESTIC -> DomesticTabContent(
                        leagues = filteredDomesticLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                    LeagueTab.CUPS -> CupsTabContent(
                        leagues = filteredCupLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                    LeagueTab.INTERNATIONAL -> InternationalTabContent(
                        leagues = filteredInternationalLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                    LeagueTab.YOUTH -> YouthTabContent(
                        leagues = filteredYouthLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                    LeagueTab.WOMEN -> WomensTabContent(
                        leagues = filteredWomenLeagues,
                        onLeagueClick = onNavigateToLeagueDetail,
                        onFavoriteClick = { viewModel.onToggleFavorite(it) },
                        viewModel = viewModel
                    )
                }
            }
            is ApiResult.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showFilter) {
        ModalBottomSheet(
            onDismissRequest = { showFilter = false },
            sheetState = sheetState,
            containerColor = Color(0xFF131620),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFF2a2d35), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sort & Filter",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Sort By", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    LeagueSortOrder.entries.forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateTempSortOrder(order) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = tempSort == order,
                                onClick = { viewModel.updateTempSortOrder(order) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00e676), unselectedColor = Color(0xFF555555))
                            )
                            val label = when (order) {
                                LeagueSortOrder.ALPHABETICAL -> "Alphabetical A-Z"
                                LeagueSortOrder.MOST_MATCHES_TODAY -> "Most matches today"
                                LeagueSortOrder.LIVE_FIRST -> "Live first"
                                LeagueSortOrder.FAVORITES_FIRST -> "Your favorites first"
                            }
                            Text(label, color = Color.White, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1a1e2a), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    Text("Filters", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTempFilters(tempFilters.copy(hasMatchesToday = !tempFilters.hasMatchesToday)) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = tempFilters.hasMatchesToday,
                            onCheckedChange = { viewModel.updateTempFilters(tempFilters.copy(hasMatchesToday = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00e676), uncheckedColor = Color(0xFF555555), checkmarkColor = Color(0xFF0d0f14))
                        )
                        Text("Has matches today", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTempFilters(tempFilters.copy(hasLiveMatches = !tempFilters.hasLiveMatches)) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = tempFilters.hasLiveMatches,
                            onCheckedChange = { viewModel.updateTempFilters(tempFilters.copy(hasLiveMatches = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00e676), uncheckedColor = Color(0xFF555555), checkmarkColor = Color(0xFF0d0f14))
                        )
                        Text("Has live matches", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTempFilters(tempFilters.copy(favoritedOnly = !tempFilters.favoritedOnly)) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = tempFilters.favoritedOnly,
                            onCheckedChange = { viewModel.updateTempFilters(tempFilters.copy(favoritedOnly = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00e676), uncheckedColor = Color(0xFF555555), checkmarkColor = Color(0xFF0d0f14))
                        )
                        Text("Favorited only", color = Color.White, fontSize = 13.sp)
                    }

                    Text("By Continent", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    val continents = listOf("Europe", "South America", "North America", "Asia", "Africa", "Oceania")
                    continents.forEach { continent ->
                        val isChecked = continent in tempFilters.continents
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = tempFilters.continents.toMutableSet()
                                    if (isChecked) current.remove(continent) else current.add(continent)
                                    viewModel.updateTempFilters(tempFilters.copy(continents = current))
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val current = tempFilters.continents.toMutableSet()
                                    if (checked) current.add(continent) else current.remove(continent)
                                    viewModel.updateTempFilters(tempFilters.copy(continents = current))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00e676), uncheckedColor = Color(0xFF555555), checkmarkColor = Color(0xFF0d0f14))
                            )
                            Text(continent, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        showFilter = false
                        scope.launch {
                            kotlinx.coroutines.delay(300)
                            viewModel.applyFilters(tempSort, tempFilters)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00e676), contentColor = Color(0xFF0d0f14))
                ) {
                    Text("Apply", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PopularTabContent(
    leagues: List<League>,
    onLeagueClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    viewModel: LeaguesViewModel
) {
    val favorites = remember(leagues) { leagues.filter { it.isFavorited } }
    val worldCups = remember(leagues) { leagues.filter { it.leagueType == "Cup" || it.isInternational } }
    val topDomestic = remember(leagues) { leagues.filter { it.id in setOf(152, 302, 207, 175, 168) } }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (favorites.isNotEmpty()) {
            item {
                SectionHeader("⭐ Your Favorites")
            }
            items(favorites, key = { "fav_${it.id}" }) { league ->
                LaunchedEffect(league.id) {
                    viewModel.loadExtraDetails(league.id)
                }
                LeagueItem(
                    league = league,
                    selectedTab = LeagueTab.POPULAR,
                    onLeagueClick = onLeagueClick,
                    onFavoriteClick = onFavoriteClick,
                    viewModel = viewModel
                )
            }
        }

        if (worldCups.isNotEmpty()) {
            item {
                SectionHeader("🌍 World & Cups")
            }
            items(worldCups, key = { "world_${it.id}" }) { league ->
                LaunchedEffect(league.id) {
                    viewModel.loadExtraDetails(league.id)
                }
                LeagueItem(
                    league = league,
                    selectedTab = LeagueTab.POPULAR,
                    onLeagueClick = onLeagueClick,
                    onFavoriteClick = onFavoriteClick,
                    viewModel = viewModel
                )
            }
        }

        if (topDomestic.isNotEmpty()) {
            item {
                SectionHeader("🏴 Top Domestic")
            }
            items(topDomestic, key = { "dom_${it.id}" }) { league ->
                LaunchedEffect(league.id) {
                    viewModel.loadExtraDetails(league.id)
                }
                LeagueItem(
                    league = league,
                    selectedTab = LeagueTab.POPULAR,
                    onLeagueClick = onLeagueClick,
                    onFavoriteClick = onFavoriteClick,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun AllTabContent(leagues: List<League>, onLeagueClick: (Int) -> Unit, onFavoriteClick: (Int) -> Unit, viewModel: LeaguesViewModel) {
    FlatLeagueList(leagues, LeagueTab.ALL, onLeagueClick, onFavoriteClick, viewModel)
}

@Composable
fun LiveTabContent(leagues: List<League>, onLeagueClick: (Int) -> Unit, onFavoriteClick: (Int) -> Unit, viewModel: LeaguesViewModel) {
    FlatLeagueList(leagues, LeagueTab.LIVE, onLeagueClick, onFavoriteClick, viewModel)
}

@Composable
fun DomesticTabContent(leagues: List<League>, onLeagueClick: (Int) -> Unit, onFavoriteClick: (Int) -> Unit, viewModel: LeaguesViewModel) {
    FlatLeagueList(leagues, LeagueTab.DOMESTIC, onLeagueClick, onFavoriteClick, viewModel)
}

@Composable
fun CupsTabContent(leagues: List<League>, onLeagueClick: (Int) -> Unit, onFavoriteClick: (Int) -> Unit, viewModel: LeaguesViewModel) {
    FlatLeagueList(leagues, LeagueTab.CUPS, onLeagueClick, onFavoriteClick, viewModel)
}

@Composable
fun InternationalTabContent(leagues: List<League>, onLeagueClick: (Int) -> Unit, onFavoriteClick: (Int) -> Unit, viewModel: LeaguesViewModel) {
    FlatLeagueList(leagues, LeagueTab.INTERNATIONAL, onLeagueClick, onFavoriteClick, viewModel)
}

@Composable
fun YouthTabContent(leagues: List<League>, onLeagueClick: (Int) -> Unit, onFavoriteClick: (Int) -> Unit, viewModel: LeaguesViewModel) {
    FlatLeagueList(leagues, LeagueTab.YOUTH, onLeagueClick, onFavoriteClick, viewModel)
}

@Composable
fun WomensTabContent(leagues: List<League>, onLeagueClick: (Int) -> Unit, onFavoriteClick: (Int) -> Unit, viewModel: LeaguesViewModel) {
    FlatLeagueList(leagues, LeagueTab.WOMEN, onLeagueClick, onFavoriteClick, viewModel)
}

@Composable
fun FlatLeagueList(
    leagues: List<League>,
    tab: LeagueTab,
    onLeagueClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    viewModel: LeaguesViewModel
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(leagues, key = { it.id }) { league ->
            LaunchedEffect(league.id) {
                viewModel.loadExtraDetails(league.id)
            }
            LeagueItem(
                league = league,
                selectedTab = tab,
                onLeagueClick = onLeagueClick,
                onFavoriteClick = onFavoriteClick,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun SectionHeader(sectionTitle: String) {
    Text(
        text = sectionTitle,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF555555),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(
            start = 16.dp, end = 16.dp,
            top = 12.dp, bottom = 8.dp
        )
    )
}

@Composable
fun LeagueItem(
    league: League,
    selectedTab: LeagueTab,
    onLeagueClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    viewModel: LeaguesViewModel
) {
    val isFavorited = league.isFavorited
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp)
            .background(
                Color(0xFF131620),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 0.5.dp,
                color = if (isFavorited) Color(0xFF1e3a22) else Color(0xFF1a1e2a),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onLeagueClick(league.id) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(league.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = league.name,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.ic_league_placeholder),
                error = painterResource(R.drawable.ic_league_placeholder)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = league.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${league.country} · ${league.season}",
                fontSize = 10.sp,
                color = Color(0xFF555555),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            if (selectedTab == LeagueTab.LIVE) {
                Text(
                    text = "${league.liveCount} matches live right now",
                    fontSize = 9.sp,
                    color = Color(0xFFff4444),
                    fontWeight = FontWeight.Medium
                )
            } else {
                val details = if (league.currentRound != null && league.teamCount != null) {
                    "${league.currentRound} · ${league.teamCount} teams"
                } else {
                    "Loading details..."
                }
                Text(
                    text = details,
                    fontSize = 9.sp,
                    color = Color(0xFF444444),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            when {
                league.liveCount > 0 -> LiveBadge(league.liveCount)
                league.todayCount > 0 -> TodayBadge(league.todayCount)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (league.isFavorited)
                        Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (league.isFavorited)
                        Color(0xFF00e676) else Color(0xFF444444),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            onClick = { onFavoriteClick(league.id) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open league",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LiveBadge(liveCount: Int) {
    Row(
        modifier = Modifier
            .background(Color(0xFF1a0a0a), RoundedCornerShape(6.dp))
            .border(0.5.dp, Color(0xFF3a1212), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(Color(0xFFff4444))
        )
        Text(
            text = "$liveCount Live",
            fontSize = 9.sp,
            color = Color(0xFFff4444),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TodayBadge(todayCount: Int) {
    Text(
        text = "$todayCount today",
        fontSize = 9.sp,
        color = Color(0xFF00e676),
        modifier = Modifier
            .background(Color(0xFF0d1a10), RoundedCornerShape(6.dp))
            .border(0.5.dp, Color(0xFF1a3a22), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

private fun getContinentForCountry(countryName: String): String {
    return when (countryName) {
        "England", "Spain", "Italy", "Germany", "France", "Netherlands", "Portugal", "Belgium", "Scotland", "Wales", "Turkey", "Greece", "Austria", "Switzerland", "Croatia", "Denmark", "Ukraine", "Russia", "Poland", "Sweden", "Norway", "Europe", "UEFA" -> "Europe"
        "Brazil", "Argentina", "Colombia", "Chile", "Uruguay", "Ecuador", "Paraguay", "Peru", "Bolivia", "Venezuela", "CONMEBOL" -> "South America"
        "USA", "Mexico", "Canada", "Costa Rica", "Jamaica", "Honduras", "Panama", "CONCACAF" -> "North America"
        "Saudi Arabia", "Japan", "South Korea", "China", "Australia", "Iran", "Qatar", "UAE", "India", "Asia", "AFC" -> "Asia"
        "Egypt", "Morocco", "Algeria", "Tunisia", "Senegal", "Nigeria", "Cameroon", "Ghana", "South Africa", "Ivory Coast", "Africa", "CAF" -> "Africa"
        "New Zealand", "Fiji", "Oceania", "OFC" -> "Oceania"
        else -> "Europe"
    }
}
