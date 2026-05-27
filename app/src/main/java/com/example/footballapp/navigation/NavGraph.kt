package com.example.footballapp.navigation

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
import com.example.footballapp.ui.screens.competitions.CompetitionsScreen
import com.example.footballapp.ui.screens.competitions.ClubInfoScreen
import com.example.footballapp.ui.screens.home.HomeScreen
import com.example.footballapp.ui.screens.fixtures.FixturesScreen
import com.example.footballapp.ui.screens.leagues.LeaguesScreen
import com.example.footballapp.ui.screens.favorites.FavoritesScreen
import com.example.footballapp.ui.screens.settings.SettingsScreen
import com.example.footballapp.ui.screens.search.SearchScreen
import com.example.footballapp.ui.screens.details.MatchCenterScreen
import com.example.footballapp.ui.screens.players.PlayerProfileScreen
import com.example.footballapp.ui.screens.players.TopPlayersScreen
import com.example.footballapp.viewmodel.ThemeViewModel
import com.example.footballapp.ui.screens.leagues.LeagueDetailScreen
import com.example.footballapp.ui.screens.onboarding.flow.OnboardingClubsScreen
import com.example.footballapp.ui.screens.onboarding.flow.OnboardingLeagueScreen
import com.example.footballapp.ui.screens.onboarding.flow.OnboardingWelcomeScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object OnboardingWelcome : Screen("onboarding/welcome")
    object OnboardingLeague : Screen("onboarding/league")
    object OnboardingClubs : Screen("onboarding/clubs/{mode}") {
        fun createRoute(mode: String) = "onboarding/clubs/$mode"
    }
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
    object TopPlayers : Screen("top_players")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Competitions : Screen("competitions")
    object ClubInfo : Screen("club_info/{teamId}/{leagueId}") {
        fun createRoute(teamId: Int, leagueId: Int) = "club_info/$teamId/$leagueId"
    }
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
                onNavigateToNotifications = { /* TODO */ },
                onNavigateToMatchCenter = { matchId ->
                    navController.navigate(Screen.MatchCenter.createRoute(matchId))
                },
                onNavigateToLeagues = { navController.navigate(Screen.Leagues.route) },
                onNavigateToLeagueDetail = { leagueId ->
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId))
                },
                onNavigateToFixtures = { navController.navigate(Screen.Fixtures.route) },
                onNavigateToPlayerProfile = { playerId ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerId))
                },
                onNavigateToTopPlayers = {
                    navController.navigate(Screen.TopPlayers.route)
                },
                onNavigateToCompetitions = {
                    navController.navigate(Screen.Competitions.route)
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
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onLeagueClick = { leagueId ->
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId))
                },
                onTeamClick = { teamId ->
                    navController.navigate(Screen.ClubInfo.createRoute(teamId, 39))
                }
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
            arguments = listOf(navArgument("leagueId") { type = NavType.IntType })
        ) { backStackEntry ->
            val leagueId = backStackEntry.arguments?.getInt("leagueId") ?: 0
            LeagueDetailScreen(
                leagueId = leagueId,
                onBackClick = { navController.popBackStack() },
                onMatchClick = { matchId ->
                    navController.navigate(Screen.MatchCenter.createRoute(matchId.toString()))
                }
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

        composable(Screen.Competitions.route) {
            CompetitionsScreen(
                onBackClick = { navController.popBackStack() },
                onCompetitionClick = { leagueId, season ->
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId))
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
            val teamId = backStackEntry.arguments?.getInt("teamId") ?: return@composable
            val leagueId = backStackEntry.arguments?.getInt("leagueId") ?: 39
            ClubInfoScreen(
                teamId = teamId,
                leagueId = leagueId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
