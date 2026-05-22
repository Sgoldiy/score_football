package com.example.footballapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.footballapp.ui.screens.home.HomeScreen
import com.example.footballapp.ui.screens.fixtures.FixturesScreen
import com.example.footballapp.ui.screens.leagues.LeaguesScreen
import com.example.footballapp.ui.screens.favorites.FavoritesScreen
import com.example.footballapp.ui.screens.settings.SettingsScreen
import com.example.footballapp.ui.screens.search.SearchScreen
import com.example.footballapp.ui.screens.onboarding.OnboardingScreen
import com.example.footballapp.ui.screens.details.MatchCenterScreen
import com.example.footballapp.ui.screens.players.PlayerProfileScreen
import com.example.footballapp.viewmodel.ThemeViewModel
import com.example.footballapp.ui.screens.leagues.LeagueDetailScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Fixtures : Screen("fixtures")
    object Leagues : Screen("leagues")
    object Favourites : Screen("favourites")
    object MatchCenter : Screen("match_center/{matchId}") {
        fun createRoute(matchId: String) = "match_center/$matchId"
    }
    object LeagueDetail : Screen("league_detail/{leagueId}") {
        fun createRoute(leagueId: Int) = "league_detail/$leagueId"
    }
    object PlayerProfile : Screen("player_profile/{playerId}") {
        fun createRoute(playerId: Int) = "player_profile/$playerId"
    }
    object Search : Screen("search")
    object Settings : Screen("settings")
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
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToFavourites = { navController.navigate(Screen.Favourites.route) },
                onNavigateToNotifications = { /* TODO */ },
                onNavigateToMatchCenter = { matchId ->
                    navController.navigate(Screen.MatchCenter.createRoute(matchId))
                },
                onNavigateToLeagues = { navController.navigate(Screen.Leagues.route) },
                onNavigateToPlayerProfile = { playerId ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerId))
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
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId))
                }
            )
        }

        composable(Screen.Favourites.route) {
            FavoritesScreen(
                onSearchClick = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MatchCenter.route,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            MatchCenterScreen(
                matchId = matchId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LeagueDetail.route,
            arguments = listOf(navArgument("leagueId") { type = NavType.IntType })
        ) { backStackEntry ->
            val leagueId = backStackEntry.arguments?.getInt("leagueId") ?: 0
            LeagueDetailScreen(
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
    }
}

