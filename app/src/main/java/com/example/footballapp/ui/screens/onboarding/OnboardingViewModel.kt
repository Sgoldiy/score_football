package com.example.footballapp.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.local.AppSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val _selectedLeagues = MutableStateFlow<Set<Int>>(emptySet())
    val selectedLeagues: StateFlow<Set<Int>> = _selectedLeagues.asStateFlow()

    private val _selectedTeams = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTeams: StateFlow<Set<Int>> = _selectedTeams.asStateFlow()

    private val _notificationSettings = MutableStateFlow<Map<String, Boolean>>(
        mapOf(
            "match_start" to true,
            "goal" to true,
            "halftime_fulltime" to true,
            "red_card" to true,
            "var_decisions" to true,
            "lineup_released" to true
        )
    )
    val notificationSettings: StateFlow<Map<String, Boolean>> = _notificationSettings.asStateFlow()

    fun toggleLeague(leagueId: Int) {
        val current = _selectedLeagues.value
        _selectedLeagues.value = if (current.contains(leagueId)) {
            current - leagueId
        } else {
            current + leagueId
        }
    }

    fun toggleTeam(teamId: Int) {
        val current = _selectedTeams.value
        _selectedTeams.value = if (current.contains(teamId)) {
            current - teamId
        } else {
            current + teamId
        }
    }

    fun toggleNotificationSetting(key: String, enabled: Boolean) {
        val current = _notificationSettings.value.toMutableMap()
        current[key] = enabled
        _notificationSettings.value = current
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            appSettingsDataStore.saveOnboardingCompleted(true)
            appSettingsDataStore.setLeaguesFollowed(_selectedLeagues.value)
            appSettingsDataStore.setTeamsFollowed(_selectedTeams.value)
            _notificationSettings.value.forEach { (key, enabled) ->
                appSettingsDataStore.saveNotificationSetting(key, enabled)
            }
        }
    }
}
