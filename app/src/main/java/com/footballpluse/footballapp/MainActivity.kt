package com.footballpluse.footballapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.footballpluse.footballapp.navigation.ROUTE_CLUBS
import com.footballpluse.footballapp.navigation.ROUTE_LEAGUE
import com.footballpluse.footballapp.navigation.ROUTE_USERNAME
import com.footballpluse.footballapp.navigation.ROUTE_WELCOME
import com.footballpluse.footballapp.navigation.Screen
import com.footballpluse.footballapp.navigation.SetupNavGraph
import com.footballpluse.footballapp.ui.components.BottomNavigationBar
import com.footballpluse.footballapp.ui.screens.splash.SplashScreen
import com.footballpluse.footballapp.ui.screens.splash.SplashViewModel
import com.footballpluse.footballapp.ui.theme.FootballPlusTheme
import com.footballpluse.footballapp.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDark by themeViewModel.isDark.collectAsState()

            FootballPlusTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val splashViewModel: SplashViewModel = hiltViewModel()
                val startDestination by splashViewModel.startDestination.collectAsState()

                val noBottomBarScreens = listOf(
                    ROUTE_WELCOME,
                    ROUTE_USERNAME,
                    ROUTE_LEAGUE,
                    ROUTE_CLUBS,
                    Screen.Onboarding.route,
                    Screen.MatchCenter.route,
                    Screen.Search.route,
                    Screen.LeagueDetail.route,
                    Screen.PlayerProfile.route,
                )

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        val currentBase = currentRoute?.split("/")?.firstOrNull()
                        val show = currentBase != null && noBottomBarScreens.none { 
                            it.split("/")[0] == currentBase
                        }
                        if (show) {
                            BottomNavigationBar(
                                currentRoute = currentRoute,
                                onItemClick = { item ->
                                    navController.navigate(item.route) {
                                        navController.graph.startDestinationRoute?.let { route ->
                                            popUpTo(route) { saveState = true }
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = androidx.compose.ui.Modifier.padding(innerPadding)) {
                        val destination = startDestination
                        if (destination == null) {
                            // Prevent any nav graph from composing until we know where to start.
                            SplashScreen()
                        } else {
                            SetupNavGraph(
                                navController = navController,
                                themeViewModel = themeViewModel,
                                startDestination = destination
                            )
                        }
                    }
                }
            }
        }
    }
}
