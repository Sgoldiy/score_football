package com.footballpluse.footballapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private suspend fun ensureAuthenticated() {
        val user = auth.currentUser
        if (user == null) {
            signInAnonymously()
        } else {
            try {
                // Verify the session is still valid by attempting to get a token.
                // Using forceRefresh = false first as it's faster.
                user.getIdToken(false).await()
            } catch (e: Exception) {
                // If token retrieval fails (e.g., "Long live credential not available"),
                // the session is likely corrupted. Sign out and re-authenticate.
                auth.signOut()
                signInAnonymously()
            }
        }
    }

    suspend fun signInAnonymously(): String {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid
            uid ?: throw Exception("Failed to retrieve Firebase user ID after anonymous authentication.")
        } catch (e: Exception) {
            // In case of internal GMS/Firebase errors like "Long live credential not available",
            // a signOut followed by a retry can often resolve the stale state.
            try {
                auth.signOut()
                val result = auth.signInAnonymously().await()
                result.user?.uid ?: throw Exception("Failed to retrieve UID on retry.")
            } catch (retryException: Exception) {
                throw Exception("Firebase authentication failed: ${e.localizedMessage}", e)
            }
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        if (username.isBlank()) return false
        return try {
            ensureAuthenticated()
            val docRef = firestore.collection("usernames").document(username.lowercase())
            val snapshot = docRef.get().await()
            !snapshot.exists()
        } catch (e: Exception) {
            throw Exception("Failed to check username availability: ${e.localizedMessage}", e)
        }
    }

    suspend fun isUsernameOwnedByCurrentDevice(username: String): Boolean {
        return try {
            ensureAuthenticated()
            val uid = auth.currentUser?.uid ?: return false
            val docRef = firestore.collection("usernames").document(username.lowercase())
            val snapshot = docRef.get().await()
            snapshot.getString("uid") == uid
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveUsername(username: String, uid: String) {
        try {
            ensureAuthenticated()
            val docRef = firestore.collection("usernames").document(username.lowercase())
            val data = mapOf(
                "uid" to uid,
                "displayUsername" to username,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
        } catch (e: Exception) {
            throw Exception("Failed to register username in Firestore: ${e.localizedMessage}", e)
        }
    }

    suspend fun saveOnboardingData(
        uid: String,
        favoriteLeague: String,
        favoriteClubs: List<String>
    ) {
        try {
            ensureAuthenticated()
            val userRef = firestore.collection("users").document(uid)
            val data = mapOf(
                "favoriteLeague" to favoriteLeague,
                "favoriteClubs" to favoriteClubs,
                "onboardingComplete" to true
            )
            userRef.set(data).await()
        } catch (e: Exception) {
            throw Exception("Failed to update user profile in Firestore: ${e.localizedMessage}", e)
        }
    }
}
