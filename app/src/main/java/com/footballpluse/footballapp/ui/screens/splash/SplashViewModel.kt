package com.footballpluse.footballapp.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.local.DataStoreManager
import com.footballpluse.footballapp.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private companion object {
        /**
         * The animated splash runs ~1.5 s; holding at least this long lets it
         * read as intentional branding instead of a flash, without slowing
         * startup further (destination resolution runs concurrently).
         */
        const val MIN_SPLASH_MS = 2_000L
    }

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            coroutineScope {
                val destination = async {
                    val isCompleted = dataStoreManager.isOnboardingCompleted.first()
                    if (isCompleted) Screen.Home.route else Screen.Onboarding.route
                }
                val minDisplay = async { delay(MIN_SPLASH_MS) }
                destination.await()
                minDisplay.await()
                _startDestination.value = destination.getCompleted()
            }
        }
    }
}
