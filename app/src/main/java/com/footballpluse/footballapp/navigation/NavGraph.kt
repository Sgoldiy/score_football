package com.footballpluse.footballapp.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.hilt.navigation.compose.hiltViewModel
import com.footballpluse.footballapp.ui.screens.home.HomeScreen
import com.footballpluse.footballapp.ui.screens.fixtures.FixturesScreen
import com.footballpluse.footballapp.ui.screens.leagues.LeaguesScreen
import com.footballpluse.footballapp.ui.screens.favorites.FavoritesScreen
import com.footballpluse.footballapp.ui.screens.settings.SettingsScreen
import com.footballpluse.footballapp.ui.screens.settings.NotificationsScreen
import com.footballpluse.footballapp.ui.screens.search.SearchScreen
import com.footballpluse.footballapp.ui.screens.details.MatchCenterScreen
import com.footballpluse.footballapp.ui.screens.players.PlayerProfileScreen
import com.footballpluse.footballapp.ui.screens.players.TopPlayersScreen
import com.footballpluse.footballapp.ui.screens.stats.StatsScreen
import com.footballpluse.footballapp.viewmodel.ThemeViewModel
import com.footballpluse.footballapp.ui.screens.leagues.LeagueDetailScreen
import com.footballpluse.footballapp.ui.screens.competitions.ClubInfoScreen
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingEvent
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingViewModel
import com.footballpluse.footballapp.ui.screens.onboarding.flow.WelcomeScreen
import com.footballpluse.footballapp.ui.screens.onboarding.flow.UsernameScreen
import com.footballpluse.footballapp.ui.screens.onboarding.flow.LeagueScreen
import com.footballpluse.footballapp.ui.screens.onboarding.flow.ClubsScreen

const val ROUTE_WELCOME = "welcome"
const val ROUTE_USERNAME = "username"
const val ROUTE_LEAGUE = "league"
const val ROUTE_CLUBS = "clubs"
const val ROUTE_HOME = "home"

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object OnboardingWelcome : Screen(ROUTE_WELCOME)
    data object OnboardingUsername : Screen(ROUTE_USERNAME)
    data object OnboardingLeague : Screen(ROUTE_LEAGUE)
    object OnboardingClubs : Screen("$ROUTE_CLUBS/{mode}") {
        fun createRoute(mode: String) = "$ROUTE_CLUBS/$mode"
    }
    data object Home : Screen(ROUTE_HOME)
    data object Fixtures : Screen("fixtures")
    data object Leagues : Screen("leagues")
    data object Favourites : Screen("favourites")
    object MatchCenter : Screen("match_center/{matchId}") {
        fun createRoute(matchId: String) = "match_center/$matchId"
    }
    object LeagueDetail : Screen("league_detail/{leagueId}/{season}") {
        fun createRoute(leagueId: Int, season: Int) = "league_detail/$leagueId/$season"
    }
    object ClubInfo : Screen("club_info/{teamId}/{leagueId}") {
        fun createRoute(teamId: Int, leagueId: Int) = "club_info/$teamId/$leagueId"
    }
    object PlayerProfile : Screen("player_profile/{playerId}") {
        fun createRoute(playerId: Int) = "player_profile/$playerId"
    }
    data object TopPlayers : Screen("top_players")
    data object Stats : Screen("stats")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Notifications : Screen("notifications")
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    themeViewModel: ThemeViewModel,
    startDestination: String = Screen.Home.route 
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        navigation(
            startDestination = Screen.OnboardingWelcome.route,
            route = Screen.Onboarding.route
        ) {
            composable(
                route = Screen.OnboardingWelcome.route,
                enterTransition = {
                    slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(280))
                }
            ) {
                WelcomeScreen(
                    onGetStarted = { navController.navigate(Screen.OnboardingUsername.route) }
                )
            }

            composable(
                route = Screen.OnboardingUsername.route,
                enterTransition = {
                    slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(280))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(280))
                }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Onboarding.route)
                }
                val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by viewModel.state.collectAsState()
                val events = viewModel.events

                LaunchedEffect(events) {
                    events.collect { event ->
                        when (event) {
                            is OnboardingEvent.NavigateToLeague -> {
                                navController.navigate(Screen.OnboardingLeague.route)
                            }
                            else -> {}
                        }
                    }
                }

                BackHandler(enabled = true) { }

                UsernameScreen(
                    state = state,
                    events = events,
                    onUsernameChanged = viewModel::onUsernameChanged,
                    onSubmit = viewModel::submitUsername,
                    onNavigateNext = { navController.navigate(Screen.OnboardingLeague.route) },
                    onRandomizeUsername = viewModel::randomizeUsername
                )
            }

            composable(
                route = Screen.OnboardingLeague.route,
                enterTransition = {
                    slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(280))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(280))
                }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Onboarding.route)
                }
                val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by viewModel.state.collectAsState()
                val events = viewModel.events

                LaunchedEffect(events) {
                    events.collect { event ->
                        when (event) {
                            is OnboardingEvent.NavigateToClubs -> {
                                navController.navigate(Screen.OnboardingClubs.createRoute("first"))
                            }
                            else -> {}
                        }
                    }
                }

                BackHandler(enabled = true) { }

                LeagueScreen(
                    state = state,
                    events = events,
                    greetingUsername = state.username,
                    leagues = viewModel.defaultLeagues,
                    onLeagueSelected = viewModel::selectLeague,
                    onContinueClick = viewModel::confirmLeague,
                    onNavigateNext = { navController.navigate(Screen.OnboardingClubs.createRoute("first")) }
                )
            }

            composable(
                route = Screen.OnboardingClubs.route,
                arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                enterTransition = {
                    slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(280))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(280))
                }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Onboarding.route)
                }
                val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by viewModel.state.collectAsState()
                val events = viewModel.events

                LaunchedEffect(events) {
                    events.collect { event ->
                        when (event) {
                            is OnboardingEvent.NavigateToHome -> {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                val mode = backStackEntry.arguments?.getString("mode") ?: "first"

                BackHandler(enabled = true) { }

                ClubsScreen(
                    state = state,
                    events = events,
                    leagues = viewModel.defaultLeagues,
                    onClubToggled = viewModel::toggleClub,
                    onContinue = {
                        if (mode == "edit") {
                            viewModel.saveClubsEdit()
                            navController.popBackStack()
                        } else {
                            viewModel.clubsContinue()
                        }
                    },
                    getClubsForLeague = viewModel::getClubsForLeague,
                    mode = mode
                )
            }
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToFavourites = { navController.navigate(Screen.Favourites.route) },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToMatchCenter = { matchId ->
                    navController.navigate(Screen.MatchCenter.createRoute(matchId))
                },
                onNavigateToLeagues = { navController.navigate(Screen.Leagues.route) },
                onNavigateToLeagueDetail = { leagueId, season ->
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId, season))
                },
                onNavigateToClubInfo = { teamId, leagueId ->
                    navController.navigate(Screen.ClubInfo.createRoute(teamId, leagueId))
                }
            )
        }

        composable(Screen.Fixtures.route) {
            FixturesScreen(
                onNavigateToMatchCenter = { matchId ->
                    navController.navigate(Screen.MatchCenter.createRoute(matchId))
                }
            )
        }

        composable(Screen.Leagues.route) {
            LeaguesScreen(
                onNavigateToLeagueDetail = { leagueId ->
                    val season = when (leagueId) {
                        28 -> 2026
                        15 -> 2025
                        1 -> 2024
                        4 -> 2025
                        else -> 2025
                    }
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId, season))
                },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                unreadNotificationCount = 0,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Favourites.route) {
            FavoritesScreen(
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onAddClubs = { navController.navigate(Screen.OnboardingClubs.createRoute("edit")) },
                onPlayerClick = { playerId ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerId))
                },
                onMatchClick = { matchId ->
                    navController.navigate(Screen.MatchCenter.createRoute(matchId.toString()))
                },
                onTeamClick = { teamId, leagueId ->
                    navController.navigate(Screen.ClubInfo.createRoute(teamId, leagueId))
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onLeagueClick = { leagueId ->
                    val season = when (leagueId) {
                        28 -> 2026
                        15 -> 2025
                        1 -> 2024
                        4 -> 2025
                        else -> 2025
                    }
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId, season))
                },
                onTeamClick = { teamId ->
                    navController.navigate(Screen.ClubInfo.createRoute(teamId, 0))
                },
                viewModel = hiltViewModel()
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onBack = { navController.popBackStack() },
                onEditPreferences = {
                    navController.navigate(Screen.OnboardingClubs.createRoute("edit"))
                }
            )
        }

        composable(
            route = Screen.MatchCenter.route,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            MatchCenterScreen(
                matchId = matchId,
                onBackClick = { navController.popBackStack() },
                onPlayerClick = { playerId ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerId))
                }
            )
        }

        composable(
            route = Screen.LeagueDetail.route,
            arguments = listOf(
                navArgument("leagueId") { type = NavType.IntType },
                navArgument("season") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val leagueId = backStackEntry.arguments?.getInt("leagueId") ?: 0
            val season = backStackEntry.arguments?.getInt("season") ?: 2025
            LeagueDetailScreen(
                leagueId = leagueId,
                season = season,
                onBackClick = { navController.popBackStack() },
                onMatchClick = { matchId ->
                    navController.navigate(Screen.MatchCenter.createRoute(matchId.toString()))
                },
                onTeamClick = { teamId ->
                    navController.navigate(Screen.ClubInfo.createRoute(teamId, leagueId))
                }
            )
        }

        composable(
            route = Screen.ClubInfo.route,
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("leagueId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getInt("teamId") ?: 0
            val leagueId = backStackEntry.arguments?.getInt("leagueId") ?: 0
            ClubInfoScreen(
                teamId = teamId,
                leagueId = leagueId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlayerProfile.route,
            arguments = listOf(navArgument("playerId") { type = NavType.IntType })
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getInt("playerId") ?: 0
            PlayerProfileScreen(
                playerId = playerId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.TopPlayers.route) {
            TopPlayersScreen(
                onBackClick = { navController.popBackStack() },
                onPlayerClick = { playerId ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerId))
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateToPlayerProfile = { playerId ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerId))
                },
                onNavigateToClubInfo = { teamId, leagueId ->
                    navController.navigate(Screen.ClubInfo.createRoute(teamId, leagueId))
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
