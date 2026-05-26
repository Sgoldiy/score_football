package com.example.footballapp.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small façade over [AppSettingsDataStore] to match the app architecture naming.
 */
@Singleton
class DataStoreManager @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore
) {
    val isOnboardingCompleted: Flow<Boolean> = appSettingsDataStore.isOnboardingCompleted
    val favouriteLeagueId: Flow<Int> = appSettingsDataStore.favouriteLeagueId
    val favouriteLeagueName: Flow<String> = appSettingsDataStore.favouriteLeagueName

    suspend fun saveOnboardingCompleted(completed: Boolean) =
        appSettingsDataStore.saveOnboardingCompleted(completed)

    suspend fun saveFavouriteLeague(id: Int, name: String) =
        appSettingsDataStore.saveFavouriteLeague(id, name)
}

