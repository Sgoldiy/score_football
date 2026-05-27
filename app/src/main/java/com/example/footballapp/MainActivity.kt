package com.example.footballapp

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
import com.example.footballapp.navigation.Screen
import com.example.footballapp.navigation.SetupNavGraph
import com.example.footballapp.ui.components.BottomNavigationBar
import com.example.footballapp.ui.screens.splash.SplashScreen
import com.example.footballapp.ui.screens.splash.SplashViewModel
import com.example.footballapp.ui.theme.FootballPlusTheme
import com.example.footballapp.viewmodel.ThemeViewModel
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
                    Screen.Onboarding.route,
                    Screen.MatchCenter.route,
                    Screen.Search.route,
                    Screen.LeagueDetail.route,
                    Screen.PlayerProfile.route,
                )

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        val show = currentRoute != null && noBottomBarScreens.none { 
                            currentRoute.startsWith(it.split("/")[0]) 
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
                        if (startDestination == null) {
                            // Prevent any nav graph from composing until we know where to start.
                            SplashScreen()
                        } else {
                            SetupNavGraph(
                                navController = navController,
                                themeViewModel = themeViewModel,
                                startDestination = startDestination!!
                            )
                        }
                    }
                }
            }
        }
    }
}
