package com.footballpluse.footballapp.data.repository

import com.footballpluse.footballapp.data.local.DataStoreManager
import com.footballpluse.footballapp.data.local.db.FavoriteClubEntity
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
    private val dataStoreManager: DataStoreManager
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
        return firestoreRepository.checkUsernameAvailable(username)
    }

    suspend fun isUsernameOwnedByCurrentDevice(username: String): Boolean {
        return firestoreRepository.isUsernameOwnedByCurrentDevice(username)
    }

    suspend fun signInAndRegisterUsername(username: String) {
        val uid = firestoreRepository.signInAnonymously()

        val isAvailable = firestoreRepository.checkUsernameAvailable(username)
        if (!isAvailable) {
            val isOwnDevice = firestoreRepository.isUsernameOwnedByCurrentDevice(username)
            if (!isOwnDevice) {
                throw UsernameTakenException("Someone just took that username! Please choose another.")
            }
        }

        firestoreRepository.saveUsername(username, uid)
        localRepository.saveUserProfile(uid, username)
        localRepository.saveUsernamePrefs(username, uid)
    }

    suspend fun saveFavoriteLeague(leagueId: String) {
        // Room DB update
        localRepository.updateFavoriteLeague(leagueId)
        // SharedPreferences save
        localRepository.saveFavoriteLeaguePref(leagueId)
    }

    suspend fun completeOnboarding(
        uid: String,
        leagueId: String,
        clubs: List<FavoriteClubEntity>
    ) {
        // Room DB: Save Favorite Clubs
        localRepository.saveFavoriteClubs(clubs)

        // Room DB: Mark User Profile complete
        localRepository.completeUserProfileOnboarding()

        // SharedPreferences: Save favorite clubs list
        localRepository.saveFavoriteClubsPref(clubs.map { it.clubId })

        // Firestore: Update remote user document
        firestoreRepository.saveOnboardingData(
            uid = uid,
            favoriteLeague = leagueId,
            favoriteClubs = clubs.map { it.clubId }
        )

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
        clubs: List<FavoriteClubEntity>
    ) {
        localRepository.saveFavoriteClubs(clubs)
        localRepository.updateFavoriteLeague(leagueId)
        localRepository.saveFavoriteClubsPref(clubs.map { it.clubId })
        firestoreRepository.saveOnboardingData(
            uid = uid,
            favoriteLeague = leagueId,
            favoriteClubs = clubs.map { it.clubId }
        )
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

        if (uid.isNotBlank()) {
            firestoreRepository.saveOnboardingData(
                uid = uid,
                favoriteLeague = favouriteLeague.id.toString(),
                favoriteClubs = clubs.map { it.clubId.toString() }
            )
        }

        if (markOnboardingCompleted) {
            localRepository.completeUserProfileOnboarding()
            localRepository.setOnboardingCompleted(true)
        }
    }
}

class UsernameTakenException(message: String) : Exception(message)
