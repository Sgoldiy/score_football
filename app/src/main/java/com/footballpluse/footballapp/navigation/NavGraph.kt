package com.footballpluse.footballapp.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
import com.footballpluse.footballapp.ui.screens.onboarding.flow.OnboardingClubsScreen
import com.footballpluse.footballapp.ui.screens.onboarding.flow.OnboardingLeagueScreen
import com.footballpluse.footballapp.ui.screens.onboarding.flow.OnboardingWelcomeScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object OnboardingWelcome : Screen("onboarding/welcome")
    data object OnboardingLeague : Screen("onboarding/league")
    object OnboardingClubs : Screen("onboarding/clubs/{mode}") {
        fun createRoute(mode: String) = "onboarding/clubs/$mode"
    }
    data object Home : Screen("home")
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
                OnboardingWelcomeScreen(
                    onGetStarted = { navController.navigate(Screen.OnboardingLeague.route) }
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
            ) {
                OnboardingLeagueScreen(
                    onContinue = { navController.navigate(Screen.OnboardingClubs.createRoute("first")) }
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
                val mode = backStackEntry.arguments?.getString("mode") ?: "first"
                OnboardingClubsScreen(
                    mode = mode,
                    onBack = {
                        if (mode == "edit") navController.popBackStack() else navController.popBackStack()
                    },
                    onDone = {
                        if (mode == "edit") {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
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
                onNavigateToPlayerProfile = { playerId ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerId))
                },
                onNavigateToTopPlayers = {
                    navController.navigate(Screen.Stats.route)
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
                        1 -> 2026
                        253 -> 2026
                        4 -> 2024
                        else -> 2025
                    }
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId, season))
                },
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
                        1 -> 2026
                        253 -> 2026
                        4 -> 2024
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
