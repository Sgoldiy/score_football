package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.local.AppSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val matchStart: Boolean = true,
    val goal: Boolean = true,
    val halftimeFulltime: Boolean = true,
    val redCard: Boolean = true,
    val varDecisions: Boolean = true,
    val lineupReleased: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = combine(
        appSettingsDataStore.notifMatchStart,
        appSettingsDataStore.notifGoal,
        appSettingsDataStore.notifHalftimeFulltime,
        appSettingsDataStore.notifRedCard,
        appSettingsDataStore.notifVarDecisions,
        appSettingsDataStore.notifLineupReleased
    ) { flows: Array<Boolean> ->
        NotificationsUiState(
            matchStart = flows[0],
            goal = flows[1],
            halftimeFulltime = flows[2],
            redCard = flows[3],
            varDecisions = flows[4],
            lineupReleased = flows[5]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState()
    )

    fun updateNotificationSetting(key: String, enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.saveNotificationSetting(key, enabled)
        }
    }
}
