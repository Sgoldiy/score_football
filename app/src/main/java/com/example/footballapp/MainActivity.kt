package com.example.footballapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.footballapp.navigation.SetupNavGraph
import com.example.footballapp.ui.theme.FootballAppTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.footballapp.ui.components.BottomNavigationBar
import com.example.footballapp.navigation.Screen
import androidx.compose.ui.graphics.Color

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FootballAppTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Screens that should NOT show the bottom bar
                val noBottomBarScreens = listOf(
                    Screen.Splash.route,
                    Screen.Onboarding.route,
                    Screen.Details.route,
                    Screen.TopPlayers.route
                )

                Scaffold(
                    bottomBar = {
                        if (currentRoute !in noBottomBarScreens) {
                            BottomNavigationBar(
                                currentRoute = currentRoute,
                                onItemClick = { item ->
                                    navController.navigate(item.route) {
                                        navController.graph.startDestinationRoute?.let { route ->
                                            popUpTo(route) {
                                                saveState = true
                                            }
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    },
                    containerColor = Color.Black
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                        SetupNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
