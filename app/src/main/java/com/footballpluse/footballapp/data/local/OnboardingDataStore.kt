package com.footballpluse.footballapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.ui.screens.onboarding.ClubItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

@Singleton
class OnboardingDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val FAVORITE_LEAGUE_ID_KEY = intPreferencesKey("favorite_league_id")
        private val FAVORITE_LEAGUE_NAME_KEY = stringPreferencesKey("favorite_league_name")
        private val PRIMARY_CLUB_ID_KEY = intPreferencesKey("primary_club_id")
        private val PRIMARY_CLUB_NAME_KEY = stringPreferencesKey("primary_club_name")
        private val FOLLOWED_CLUB_IDS_KEY = stringSetPreferencesKey("followed_club_ids")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun saveOnboardingResult(
        league: LeagueInfo?,
        primaryClub: ClubItem?,
        followedClubIds: Set<Int>
    ) {
        context.dataStore.edit { preferences ->
            league?.let {
                preferences[FAVORITE_LEAGUE_ID_KEY] = it.id
                preferences[FAVORITE_LEAGUE_NAME_KEY] = it.name
            }
            primaryClub?.let {
                preferences[PRIMARY_CLUB_ID_KEY] = it.id
                preferences[PRIMARY_CLUB_NAME_KEY] = it.name
            }
            preferences[FOLLOWED_CLUB_IDS_KEY] = followedClubIds.map { it.toString() }.toSet()
        }
    }
}
