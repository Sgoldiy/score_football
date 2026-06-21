package com.footballpluse.footballapp.data.repository

import android.content.SharedPreferences
import com.footballpluse.footballapp.data.local.db.FavoriteClubDao
import com.footballpluse.footballapp.data.local.db.FavoriteClubEntity
import com.footballpluse.footballapp.data.local.db.UserProfileDao
import com.footballpluse.footballapp.data.local.db.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val favoriteClubDao: FavoriteClubDao,
    private val sharedPrefs: SharedPreferences
) {

    // --- SharedPreferences Operations ---

    fun isOnboardingCompleted(): Boolean {
        return sharedPrefs.getBoolean("onboarding_complete", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPrefs.edit().putBoolean("onboarding_complete", completed).apply()
    }

    fun saveUsernamePrefs(username: String, uid: String) {
        sharedPrefs.edit().apply {
            putString("username", username)
            putString("uid", uid)
            putBoolean("onboarding_complete", false)
            apply()
        }
    }

    fun saveFavoriteLeaguePref(leagueId: String) {
        sharedPrefs.edit().putString("favorite_league", leagueId).apply()
    }

    fun saveFavoriteClubsPref(clubIds: List<String>) {
        val jsonArray = "[${clubIds.joinToString(",") { "\"$it\"" }}]"
        sharedPrefs.edit().putString("favorite_clubs", jsonArray).apply()
    }

    fun getUsernamePref(): String? {
        return sharedPrefs.getString("username", null)
    }

    fun getUidPref(): String? {
        return sharedPrefs.getString("uid", null)
    }

    fun saveOnboardingLeagueProgress(leagueId: String) {
        sharedPrefs.edit().putString("onboard_league", leagueId).apply()
    }

    fun getOnboardingLeagueProgress(): String? {
        return sharedPrefs.getString("onboard_league", null)
    }

    fun saveOnboardingClubProgress(clubIds: List<String>) {
        val json = clubIds.joinToString(",") { it }
        sharedPrefs.edit().putString("onboard_clubs", json).apply()
    }

    fun getOnboardingClubProgress(): List<String> {
        val raw = sharedPrefs.getString("onboard_clubs", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    // --- Room DB Operations ---

    fun getProfileFlow(): Flow<UserProfileEntity?> {
        return userProfileDao.getProfile()
    }

    suspend fun saveUserProfile(uid: String, username: String) {
        val entity = UserProfileEntity(
            id = 1,
            uid = uid,
            username = username.lowercase(),
            displayUsername = username,
            onboardingComplete = false
        )
        userProfileDao.insert(entity)
    }

    suspend fun updateFavoriteLeague(leagueId: String) {
        val profile = userProfileDao.getProfile().firstOrNull()
        if (profile != null) {
            userProfileDao.update(profile.copy(favoriteLeague = leagueId))
        } else {
            // Fallback: If profile doesn't exist, create it (should not happen in sequence)
            val uid = getUidPref() ?: ""
            val username = getUsernamePref() ?: ""
            val entity = UserProfileEntity(
                id = 1,
                uid = uid,
                username = username.lowercase(),
                displayUsername = username,
                favoriteLeague = leagueId,
                onboardingComplete = false
            )
            userProfileDao.insert(entity)
        }
    }

    suspend fun completeUserProfileOnboarding() {
        val profile = userProfileDao.getProfile().firstOrNull()
        if (profile != null) {
            userProfileDao.update(profile.copy(onboardingComplete = true))
        }
    }

    fun getFavoriteClubsFlow(): Flow<List<FavoriteClubEntity>> {
        return favoriteClubDao.getAllClubs()
    }

    suspend fun saveFavoriteClubs(clubs: List<FavoriteClubEntity>) {
        favoriteClubDao.deleteAll()
        favoriteClubDao.insertAll(clubs)
    }
}
