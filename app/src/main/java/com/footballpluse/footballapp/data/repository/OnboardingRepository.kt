package com.footballpluse.footballapp.data.repository

import com.footballpluse.footballapp.data.local.DataStoreManager
import com.footballpluse.footballapp.data.local.db.FavoriteClubEntity
import com.footballpluse.footballapp.data.local.db.FavouriteClubDao
import com.footballpluse.footballapp.data.local.db.FavouriteClubEntity
import com.footballpluse.footballapp.data.local.db.FavouriteLeagueEntity
import com.footballpluse.footballapp.data.local.db.UserProfileEntity
import com.footballpluse.footballapp.domain.model.OnboardingClub
import com.footballpluse.footballapp.domain.model.OnboardingDefaults
import com.footballpluse.footballapp.domain.model.OnboardingLeague
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepository @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val localRepository: LocalRepository,
    private val dataStoreManager: DataStoreManager,
    private val favouriteClubDao: FavouriteClubDao,
    private val favouriteLeagueDao: com.footballpluse.footballapp.data.local.db.FavouriteLeagueDao
) {

    fun isOnboardingCompleted(): Boolean {
        return localRepository.isOnboardingCompleted()
    }

    fun getUsernamePref(): String? {
        return localRepository.getUsernamePref()
    }

    fun getUidPref(): String? {
        return localRepository.getUidPref()
    }

    fun saveOnboardingLeagueProgress(leagueId: String) {
        localRepository.saveOnboardingLeagueProgress(leagueId)
    }

    fun getOnboardingLeagueProgress(): String? {
        return localRepository.getOnboardingLeagueProgress()
    }

    fun saveOnboardingClubProgress(clubIds: List<String>) {
        localRepository.saveOnboardingClubProgress(clubIds)
    }

    fun getOnboardingClubProgress(): List<String> {
        return localRepository.getOnboardingClubProgress()
    }

    fun getProfileFlow(): Flow<UserProfileEntity?> {
        return localRepository.getProfileFlow()
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        return try {
            firestoreRepository.checkUsernameAvailable(username)
        } catch (_: Exception) {
            true // Assume available when Firebase is unreachable
        }
    }

    suspend fun isUsernameOwnedByCurrentDevice(username: String): Boolean {
        return try {
            firestoreRepository.isUsernameOwnedByCurrentDevice(username)
        } catch (_: Exception) {
            false // Can't confirm ownership without Firebase
        }
    }

    suspend fun signInAndRegisterUsername(username: String) {
        // Attempt Firebase auth — if it fails, generate a local UID so onboarding can continue offline
        val uid: String = try {
            firestoreRepository.signInAnonymously()
        } catch (_: Exception) {
            "local_${username.lowercase()}_${System.currentTimeMillis()}"
        }

        // Firebase availability check is best-effort; assume available on failure
        var isAvailable = true
        var isOwnDevice = false
        try {
            isAvailable = firestoreRepository.checkUsernameAvailable(username)
            if (!isAvailable) {
                isOwnDevice = firestoreRepository.isUsernameOwnedByCurrentDevice(username)
            }
        } catch (_: Exception) {
            // Assume username is available when Firebase is unreachable
        }
        if (!isAvailable && !isOwnDevice) {
            throw UsernameTakenException("Someone just took that username! Please choose another.")
        }

        // Save locally FIRST so UID is persisted regardless of Firestore outcome
        localRepository.saveUserProfile(uid, username)
        localRepository.saveUsernamePrefs(username, uid)

        // Firestore write is best-effort
        try {
            firestoreRepository.saveUsername(username, uid)
        } catch (_: Exception) {
            // Remote sync will be retried on clubs save
        }
    }

    suspend fun saveFavoriteLeague(leagueIdStr: String) {
        // Room DB update (User Profile)
        localRepository.updateFavoriteLeague(leagueIdStr)
        
        val leagueId = leagueIdStr.toIntOrNull() ?: 152
        // Room DB update (Favourite League Table)
        val leagueName = OnboardingDefaults.leagueName(leagueIdStr) ?: "Premier League"
        val leagueLogo = OnboardingDefaults.leagueLogoUrl(leagueId, leagueName)
        favouriteLeagueDao.clear()
        favouriteLeagueDao.insertAll(listOf(
            FavouriteLeagueEntity(
                leagueId = leagueId,
                leagueName = leagueName,
                country = null,
                logoUrl = leagueLogo
            )
        ))

        // SharedPreferences save
        localRepository.saveFavoriteLeaguePref(leagueIdStr)
        // DataStore save
        dataStoreManager.saveFavouriteLeague(leagueId, leagueName)
    }

    suspend fun completeOnboarding(
        uid: String,
        leagueId: String,
        leagueName: String,
        clubs: List<com.footballpluse.footballapp.data.local.db.FavoriteClubEntity>
    ) {
        // Room DB: Save Favorite Clubs (using consolidated dao)
        val consolidatedClubs = clubs.map { 
            FavouriteClubEntity(
                clubId = it.clubId.toIntOrNull() ?: 0,
                clubName = it.clubName,
                leagueId = it.leagueId.toIntOrNull() ?: 0,
                leagueName = leagueName,
                logoUrl = it.logoUrl ?: ""
            )
        }
        favouriteClubDao.clear()
        favouriteClubDao.insertAll(consolidatedClubs)

        // Save also to the "Favorite" legacy table if still used by some UI
        localRepository.saveFavoriteClubs(clubs)

        // Mark User Profile complete
        localRepository.completeUserProfileOnboarding()

        // SharedPreferences: Save favorite clubs list
        localRepository.saveFavoriteClubsPref(clubs.map { it.clubId })

        // DataStore: Save league preference
        dataStoreManager.saveFavouriteLeague(leagueId.toIntOrNull() ?: 152, leagueName)

        // Mark onboarding complete
        localRepository.setOnboardingCompleted(true)
        dataStoreManager.saveOnboardingCompleted(true)

        // Sync to Firestore
        try {
            firestoreRepository.saveOnboardingData(
                uid = uid,
                favoriteLeague = leagueId,
                favoriteClubs = clubs.map { it.clubId }
            )
        } catch (_: Exception) {}
    }

    fun saveFavoriteClubsPref(clubIds: List<String>) {
        localRepository.saveFavoriteClubsPref(clubIds)
    }

    suspend fun saveSelectedClubsOnly(
        uid: String,
        leagueId: String,
        leagueName: String,
        clubs: List<FavoriteClubEntity>
    ) {
        localRepository.saveFavoriteClubs(clubs)
        localRepository.updateFavoriteLeague(leagueId)
        localRepository.saveFavoriteClubsPref(clubs.map { it.clubId })
        dataStoreManager.saveFavouriteLeague(leagueId.toIntOrNull() ?: 152, leagueName)
        favouriteClubDao.clear()
        favouriteClubDao.insertAll(clubs.toFavouriteClubEntities(leagueName))
        try {
            firestoreRepository.saveOnboardingData(
                uid = uid,
                favoriteLeague = leagueId,
                favoriteClubs = clubs.map { it.clubId }
            )
        } catch (_: Exception) {
            // Remote sync is best-effort
        }
    }

    suspend fun saveSelections(
        favouriteLeague: OnboardingLeague,
        clubs: List<OnboardingClub>,
        markOnboardingCompleted: Boolean
    ) {
        val uid = localRepository.getUidPref() ?: ""

        // Standardized Favourite entities
        val consolidatedClubs = clubs.map { club ->
            FavouriteClubEntity(
                clubId = club.clubId,
                clubName = club.clubName,
                leagueId = club.leagueId,
                leagueName = favouriteLeague.name,
                logoUrl = OnboardingDefaults.clubLogoUrl(club.clubId, club.clubName),
                addedAt = System.currentTimeMillis()
            )
        }

        favouriteClubDao.clear()
        favouriteClubDao.insertAll(consolidatedClubs)

        val leagueLogo = OnboardingDefaults.leagueLogoUrl(favouriteLeague.id, favouriteLeague.name)
        favouriteLeagueDao.clear()
        favouriteLeagueDao.insertAll(listOf(
            FavouriteLeagueEntity(
                leagueId = favouriteLeague.id,
                leagueName = favouriteLeague.name,
                country = favouriteLeague.country,
                logoUrl = leagueLogo
            )
        ))

        // Update profile
        localRepository.updateFavoriteLeague(favouriteLeague.id.toString())
        localRepository.saveFavoriteLeaguePref(favouriteLeague.id.toString())
        dataStoreManager.saveFavouriteLeague(favouriteLeague.id, favouriteLeague.name)

        if (uid.isNotBlank()) {
            try {
                firestoreRepository.saveOnboardingData(
                    uid = uid,
                    favoriteLeague = favouriteLeague.id.toString(),
                    favoriteClubs = clubs.map { it.clubId.toString() }
                )
            } catch (_: Exception) {
            }
        }

        if (markOnboardingCompleted) {
            localRepository.completeUserProfileOnboarding()
            localRepository.setOnboardingCompleted(true)
            dataStoreManager.saveOnboardingCompleted(true)
        }
    }
}

class UsernameTakenException(message: String) : Exception(message)

private fun List<FavoriteClubEntity>.toFavouriteClubEntities(leagueName: String): List<FavouriteClubEntity> {
    return map { entity ->
        FavouriteClubEntity(
            clubId = entity.clubId.toIntOrNull() ?: 0,
            clubName = entity.clubName,
            leagueId = entity.leagueId.toIntOrNull() ?: 0,
            leagueName = leagueName,
            logoUrl = entity.logoUrl ?: "",
            addedAt = entity.addedAt
        )
    }
}
