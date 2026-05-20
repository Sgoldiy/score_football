package com.example.footballapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.footballapp.ui.screens.splash.SplashScreen
import com.example.footballapp.ui.screens.onboarding.OnboardingScreen
import com.example.footballapp.ui.screens.home.HomeScreen
import com.example.footballapp.ui.screens.fixtures.FixturesScreen
import com.example.footballapp.ui.screens.leagues.LeaguesScreen
import com.example.footballapp.ui.screens.details.DetailsScreen
import com.example.footballapp.ui.screens.favorites.FavoritesScreen
import com.example.footballapp.ui.screens.settings.SettingsScreen
import com.example.footballapp.ui.screens.players.TopPlayersScreen
import com.example.footballapp.ui.screens.players.PlayerProfileScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Fixtures : Screen("fixtures")
    object Leagues : Screen("leagues")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object TopPlayers : Screen("top_players")
    object Details : Screen("details/{fixtureId}") {
        fun createRoute(fixtureId: String) = "details/$fixtureId"
    }
    object PlayerProfile : Screen("player_profile/{playerId}") {
        fun createRoute(playerId: Int) = "player_profile/$playerId"
    }
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = tween(500)
            ) + fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = tween(500)
            ) + fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = tween(500)
            ) + fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = tween(500)
            ) + fadeOut(animationSpec = tween(500))
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Onboarding.route) {
            val viewModel: com.example.footballapp.ui.screens.onboarding.OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            OnboardingScreen(
                onComplete = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToFixtures = { navController.navigate(Screen.Fixtures.route) },
                onNavigateToLeagues = { navController.navigate(Screen.Leagues.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTopPlayers = { navController.navigate(Screen.TopPlayers.route) },
                onNavigateToMatchDetails = { fixtureId ->
                    navController.navigate(Screen.Details.createRoute(fixtureId))
                }
            )
        }
        
        composable(Screen.Fixtures.route) {
            FixturesScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToMatchDetails = { fixtureId ->
                    navController.navigate(Screen.Details.createRoute(fixtureId))
                }
            )
        }
        
        composable(Screen.Leagues.route) {
            LeaguesScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.TopPlayers.route) {
            TopPlayersScreen(
                onBackClick = { navController.popBackStack() },
                onPlayerClick = { playerId -> navController.navigate(Screen.PlayerProfile.createRoute(playerId)) }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("fixtureId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fixtureId = backStackEntry.arguments?.getString("fixtureId") ?: ""
            DetailsScreen(
                fixtureId = fixtureId,
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
