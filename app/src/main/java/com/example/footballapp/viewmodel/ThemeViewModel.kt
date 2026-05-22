package com.example.footballapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.local.AppSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    application: Application
) : AndroidViewModel(application) {

    val isDark: StateFlow<Boolean> = appSettingsDataStore.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun toggle() {
        viewModelScope.launch {
            appSettingsDataStore.saveThemeMode(!isDark.value)
        }
    }
}
