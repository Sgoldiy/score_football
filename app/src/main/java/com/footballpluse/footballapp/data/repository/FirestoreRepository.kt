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

    suspend fun signInAnonymously(): String {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid
            uid ?: throw Exception("Failed to retrieve Firebase user ID after anonymous authentication.")
        } catch (e: Exception) {
            throw Exception("Firebase authentication failed: ${e.localizedMessage}", e)
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        if (username.isBlank()) return false
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            val docRef = firestore.collection("usernames").document(username.lowercase())
            val snapshot = docRef.get().await()
            !snapshot.exists()
        } catch (e: Exception) {
            throw Exception("Failed to check username availability: ${e.localizedMessage}", e)
        }
    }

    suspend fun isUsernameOwnedByCurrentDevice(username: String): Boolean {
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
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
