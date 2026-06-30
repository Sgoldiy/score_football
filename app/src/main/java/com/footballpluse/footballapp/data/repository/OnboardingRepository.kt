package com.footballpluse.footballapp.data.repository

import com.footballpluse.footballapp.data.local.DataStoreManager
import com.footballpluse.footballapp.data.local.db.FavoriteClubEntity
import com.footballpluse.footballapp.data.local.db.FavouriteClubDao
import com.footballpluse.footballapp.data.local.db.FavouriteClubEntity
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
    private val favouriteClubDao: FavouriteClubDao
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

    suspend fun saveFavoriteLeague(leagueId: String) {
        // Room DB update
        localRepository.updateFavoriteLeague(leagueId)
        // SharedPreferences save
        localRepository.saveFavoriteLeaguePref(leagueId)
        // DataStore save: used by HomeViewModel for favouriteLeagueId/Name
        val leagueName = OnboardingDefaults.leagueName(leagueId)
        if (leagueName != null) {
            dataStoreManager.saveFavouriteLeague(leagueId.toIntOrNull() ?: 39, leagueName)
        }
    }

    suspend fun completeOnboarding(
        uid: String,
        leagueId: String,
        leagueName: String,
        clubs: List<FavoriteClubEntity>
    ) {
        // Room DB: Save Favorite Clubs
        localRepository.saveFavoriteClubs(clubs)

        // App Database: Sync to favourite_clubs so FavouriteRepository can read them
        favouriteClubDao.clear()
        favouriteClubDao.insertAll(clubs.toFavouriteClubEntities(leagueName))

        // Room DB: Mark User Profile complete
        localRepository.completeUserProfileOnboarding()

        // SharedPreferences: Save favorite clubs list
        localRepository.saveFavoriteClubsPref(clubs.map { it.clubId })

        // Firestore: Update remote user document (non-fatal if it fails)
        try {
            firestoreRepository.saveOnboardingData(
                uid = uid,
                favoriteLeague = leagueId,
                favoriteClubs = clubs.map { it.clubId }
            )
        } catch (_: Exception) {
            // Remote sync is best-effort; local onboarding completes regardless
        }

        // DataStore: Save league preference (HomeViewModel reads this)
        dataStoreManager.saveFavouriteLeague(leagueId.toIntOrNull() ?: 39, leagueName)

        // SharedPreferences: Mark onboarding complete
        localRepository.setOnboardingCompleted(true)

        // DataStore: Mark onboarding complete (SplashViewModel reads this)
        dataStoreManager.saveOnboardingCompleted(true)
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
        dataStoreManager.saveFavouriteLeague(leagueId.toIntOrNull() ?: 39, leagueName)
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

        val entities = clubs.map { club ->
            FavoriteClubEntity(
                clubId = club.clubId.toString(),
                clubName = club.clubName,
                leagueId = club.leagueId.toString(),
                logoUrl = OnboardingDefaults.clubLogoUrl(club.clubId),
                addedAt = System.currentTimeMillis()
            )
        }

        localRepository.saveFavoriteClubs(entities)
        localRepository.updateFavoriteLeague(favouriteLeague.id.toString())
        localRepository.saveFavoriteLeaguePref(favouriteLeague.id.toString())

        favouriteClubDao.clear()
        favouriteClubDao.insertAll(entities.toFavouriteClubEntities(favouriteLeague.name))

        if (uid.isNotBlank()) {
            try {
                firestoreRepository.saveOnboardingData(
                    uid = uid,
                    favoriteLeague = favouriteLeague.id.toString(),
                    favoriteClubs = clubs.map { it.clubId.toString() }
                )
            } catch (_: Exception) {
                // Remote sync is best-effort
            }
        }

        if (markOnboardingCompleted) {
            localRepository.completeUserProfileOnboarding()
            localRepository.setOnboardingCompleted(true)
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
