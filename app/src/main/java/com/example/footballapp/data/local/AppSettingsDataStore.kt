package com.example.footballapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "football_app_settings")

@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")
        // Onboarding favourites (used across the app)
        private val FAVOURITE_LEAGUE_ID_KEY = intPreferencesKey("favourite_league_id")
        private val FAVOURITE_LEAGUE_NAME_KEY = stringPreferencesKey("favourite_league_name")
        private val FOLLOWED_LEAGUES_KEY = stringSetPreferencesKey("followed_leagues")
        private val FOLLOWED_TEAMS_KEY = stringSetPreferencesKey("followed_teams")
        private val FOLLOWED_PLAYERS_KEY = stringSetPreferencesKey("followed_players")
        private val RECENT_SEARCHES_KEY = stringSetPreferencesKey("recent_searches")
        
        // Notification settings
        private val NOTIF_MATCH_START_KEY = booleanPreferencesKey("notif_match_start")
        private val NOTIF_GOAL_KEY = booleanPreferencesKey("notif_goal")
        private val NOTIF_HALFTIME_FULLTIME_KEY = booleanPreferencesKey("notif_halftime_fulltime")
        private val NOTIF_RED_CARD_KEY = booleanPreferencesKey("notif_red_card")
        private val NOTIF_VAR_DECISIONS_KEY = booleanPreferencesKey("notif_var_decisions")
        private val NOTIF_LINEUP_RELEASED_KEY = booleanPreferencesKey("notif_lineup_released")
    }

    // Onboarding Completed
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    // Favourite league (DataStore keys required by onboarding)
    val favouriteLeagueId: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[FAVOURITE_LEAGUE_ID_KEY] ?: 39 } // default EPL

    val favouriteLeagueName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[FAVOURITE_LEAGUE_NAME_KEY] ?: "Premier League" }

    suspend fun saveFavouriteLeague(id: Int, name: String) {
        context.dataStore.edit { preferences ->
            preferences[FAVOURITE_LEAGUE_ID_KEY] = id
            preferences[FAVOURITE_LEAGUE_NAME_KEY] = name
        }
    }

    // Theme Mode
    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_DARK_THEME_KEY] ?: true }

    suspend fun saveThemeMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_THEME_KEY] = isDark
        }
    }

    // Followed Leagues
    val followedLeagues: Flow<Set<Int>> = context.dataStore.data
        .map { preferences ->
            (preferences[FOLLOWED_LEAGUES_KEY] ?: emptySet())
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        }

    suspend fun toggleLeagueFollowed(leagueId: Int) {
        context.dataStore.edit { preferences ->
            val current = (preferences[FOLLOWED_LEAGUES_KEY] ?: emptySet()).toMutableSet()
            val idStr = leagueId.toString()
            if (current.contains(idStr)) {
                current.remove(idStr)
            } else {
                current.add(idStr)
            }
            preferences[FOLLOWED_LEAGUES_KEY] = current
        }
    }

    suspend fun setLeaguesFollowed(leagueIds: Set<Int>) {
        context.dataStore.edit { preferences ->
            preferences[FOLLOWED_LEAGUES_KEY] = leagueIds.map { it.toString() }.toSet()
        }
    }

    // Followed Teams
    val followedTeams: Flow<Set<Int>> = context.dataStore.data
        .map { preferences ->
            (preferences[FOLLOWED_TEAMS_KEY] ?: emptySet())
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        }

    suspend fun toggleTeamFollowed(teamId: Int) {
        context.dataStore.edit { preferences ->
            val current = (preferences[FOLLOWED_TEAMS_KEY] ?: emptySet()).toMutableSet()
            val idStr = teamId.toString()
            if (current.contains(idStr)) {
                current.remove(idStr)
            } else {
                current.add(idStr)
            }
            preferences[FOLLOWED_TEAMS_KEY] = current
        }
    }

    suspend fun setTeamsFollowed(teamIds: Set<Int>) {
        context.dataStore.edit { preferences ->
            preferences[FOLLOWED_TEAMS_KEY] = teamIds.map { it.toString() }.toSet()
        }
    }

    // Followed Players
    val followedPlayers: Flow<Set<Int>> = context.dataStore.data
        .map { preferences ->
            (preferences[FOLLOWED_PLAYERS_KEY] ?: emptySet())
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        }

    suspend fun togglePlayerFollowed(playerId: Int) {
        context.dataStore.edit { preferences ->
            val current = (preferences[FOLLOWED_PLAYERS_KEY] ?: emptySet()).toMutableSet()
            val idStr = playerId.toString()
            if (current.contains(idStr)) {
                current.remove(idStr)
            } else {
                current.add(idStr)
            }
            preferences[FOLLOWED_PLAYERS_KEY] = current
        }
    }

    // Recent Searches
    val recentSearches: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            (preferences[RECENT_SEARCHES_KEY] ?: emptySet()).toList()
        }

    suspend fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = (preferences[RECENT_SEARCHES_KEY] ?: emptySet()).toMutableSet()
            current.remove(query)
            current.add(query)
            // Limit to last 5 searches
            if (current.size > 5) {
                val toKeep = current.toList().takeLast(5).toSet()
                preferences[RECENT_SEARCHES_KEY] = toKeep
            } else {
                preferences[RECENT_SEARCHES_KEY] = current
            }
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.dataStore.edit { preferences ->
            val current = (preferences[RECENT_SEARCHES_KEY] ?: emptySet()).toMutableSet()
            current.remove(query)
            preferences[RECENT_SEARCHES_KEY] = current
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences[RECENT_SEARCHES_KEY] = emptySet()
        }
    }

    // Notification Toggles
    val notifMatchStart: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_MATCH_START_KEY] ?: true }
    val notifGoal: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_GOAL_KEY] ?: true }
    val notifHalftimeFulltime: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_HALFTIME_FULLTIME_KEY] ?: true }
    val notifRedCard: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_RED_CARD_KEY] ?: true }
    val notifVarDecisions: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_VAR_DECISIONS_KEY] ?: true }
    val notifLineupReleased: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_LINEUP_RELEASED_KEY] ?: true }

    suspend fun saveNotificationSetting(key: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            when (key) {
                "match_start" -> preferences[NOTIF_MATCH_START_KEY] = enabled
                "goal" -> preferences[NOTIF_GOAL_KEY] = enabled
                "halftime_fulltime" -> preferences[NOTIF_HALFTIME_FULLTIME_KEY] = enabled
                "red_card" -> preferences[NOTIF_RED_CARD_KEY] = enabled
                "var_decisions" -> preferences[NOTIF_VAR_DECISIONS_KEY] = enabled
                "lineup_released" -> preferences[NOTIF_LINEUP_RELEASED_KEY] = enabled
            }
        }
    }
}
