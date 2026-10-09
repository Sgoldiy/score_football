package com.footballpluse.footballapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class FirebaseChatMessage(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val reactionType: String? = null,
    val isSystem: Boolean = false
)

data class FirebaseSocialPost(
    val id: String = "",
    val clubId: Int = 0,
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String? = null,
    val timestamp: Long = 0L,
    val content: String = "",
    val imageUrl: String? = null,
    val hotTakeTag: String? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<String> = emptyList(),
    val pollVotes: List<Int> = emptyList(),
    val voters: Map<String, Int> = emptyMap(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentCount: Int = 0
)

@Singleton
class CommunityFirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val firestoreRepository: FirestoreRepository
) {

    private suspend fun getOrCreateUserId(): String {
        return auth.currentUser?.uid ?: run {
            firestoreRepository.signInAnonymously()
        }
    }

    // --- MATCH LIVE CHAT ---

    fun observeMatchComments(matchId: String): Flow<List<FirebaseChatMessage>> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("matches")
            .document(matchId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val username = doc.getString("username") ?: "Fan"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val userId = doc.getString("userId") ?: ""
                    val reactionType = doc.getString("reactionType")
                    val isSystem = doc.getBoolean("isSystem") ?: false

                    FirebaseChatMessage(
                        id = id,
                        userId = userId,
                        username = username,
                        text = text,
                        timestamp = timestamp,
                        reactionType = reactionType,
                        isSystem = isSystem
                    )
                }.orEmpty()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMatchComment(
        matchId: String,
        text: String,
        username: String,
        reactionType: String? = null
    ): Result<Unit> = runCatching {
        if (matchId.isBlank() || text.isBlank()) return@runCatching

        val uid = getOrCreateUserId()
        val commentId = UUID.randomUUID().toString()
        val data = hashMapOf(
            "id" to commentId,
            "matchId" to matchId,
            "userId" to uid,
            "username" to username.ifBlank { "Fan" },
            "text" to text.trim(),
            "timestamp" to System.currentTimeMillis(),
            "reactionType" to reactionType,
            "isSystem" to false
        )

        firestore.collection("matches")
            .document(matchId)
            .collection("comments")
            .document(commentId)
            .set(data)
            .await()
    }

    // --- CLUB COMMUNITY POSTS ---

    fun observeClubPosts(clubId: Int): Flow<List<FirebaseSocialPost>> = callbackFlow {
        if (clubId <= 0) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("clubs")
            .document(clubId.toString())
            .collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val content = doc.getString("content") ?: return@mapNotNull null
                    val username = doc.getString("username") ?: "Fan"
                    val userId = doc.getString("userId") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val hotTakeTag = doc.getString("hotTakeTag")
                    val pollQuestion = doc.getString("pollQuestion")
                    @Suppress("UNCHECKED_CAST")
                    val pollOptions = (doc.get("pollOptions") as? List<String>) ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val pollVotesRaw = (doc.get("pollVotes") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val voters = (doc.get("voters") as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap()
                    val likes = (doc.getLong("likes") ?: 0L).toInt()
                    @Suppress("UNCHECKED_CAST")
                    val likedBy = (doc.get("likedBy") as? List<String>) ?: emptyList()
                    val commentCount = (doc.getLong("commentCount") ?: 0L).toInt()

                    FirebaseSocialPost(
                        id = id,
                        clubId = clubId,
                        userId = userId,
                        username = username,
                        userAvatarUrl = doc.getString("userAvatarUrl"),
                        timestamp = timestamp,
                        content = content,
                        imageUrl = doc.getString("imageUrl"),
                        hotTakeTag = hotTakeTag,
                        pollQuestion = pollQuestion,
                        pollOptions = pollOptions,
                        pollVotes = pollVotesRaw.ifEmpty { List(pollOptions.size) { 0 } },
                        voters = voters,
                        likes = likes,
                        likedBy = likedBy,
                        commentCount = commentCount
                    )
                }.orEmpty()

                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createClubPost(
        clubId: Int,
        content: String,
        username: String,
        hotTakeTag: String?,
        pollOptions: List<String>?
    ): Result<Unit> = runCatching {
        if (clubId <= 0 || content.isBlank()) return@runCatching

        val uid = getOrCreateUserId()

        // 1 message per user per day check
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getDefault()).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        val recentPostsSnapshot = firestore.collection("clubs")
            .document(clubId.toString())
            .collection("posts")
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .limit(1)
            .get()
            .await()

        if (!recentPostsSnapshot.isEmpty) {
            throw IllegalStateException("You can only post once per day in this club's community.")
        }

        val postId = UUID.randomUUID().toString()
        val validOptions = pollOptions?.filter { it.isNotBlank() } ?: emptyList()

        val data = hashMapOf(
            "id" to postId,
            "clubId" to clubId,
            "userId" to uid,
            "username" to username.ifBlank { "Fan" },
            "timestamp" to System.currentTimeMillis(),
            "content" to content.trim(),
            "hotTakeTag" to hotTakeTag,
            "pollQuestion" to if (validOptions.isNotEmpty()) "Poll" else null,
            "pollOptions" to validOptions,
            "pollVotes" to List(validOptions.size) { 0 },
            "voters" to emptyMap<String, Int>(),
            "likes" to 0,
            "likedBy" to emptyList<String>(),
            "commentCount" to 0
        )

        firestore.collection("clubs")
            .document(clubId.toString())
            .collection("posts")
            .document(postId)
            .set(data)
            .await()
    }

    suspend fun toggleLikePost(clubId: Int, postId: String): Result<Unit> = runCatching {
        val uid = getOrCreateUserId()
        val postRef = firestore.collection("clubs")
            .document(clubId.toString())
            .collection("posts")
            .document(postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            @Suppress("UNCHECKED_CAST")
            val likedBy = (snapshot.get("likedBy") as? List<String>)?.toMutableList() ?: mutableListOf()
            val currentLikes = (snapshot.getLong("likes") ?: 0L).toInt()

            if (likedBy.contains(uid)) {
                likedBy.remove(uid)
                transaction.update(postRef, "likedBy", likedBy)
                transaction.update(postRef, "likes", (currentLikes - 1).coerceAtLeast(0))
            } else {
                likedBy.add(uid)
                transaction.update(postRef, "likedBy", likedBy)
                transaction.update(postRef, "likes", currentLikes + 1)
            }
        }.await()
    }

    suspend fun voteInPoll(clubId: Int, postId: String, optionIndex: Int): Result<Unit> = runCatching {
        val uid = getOrCreateUserId()
        val postRef = firestore.collection("clubs")
            .document(clubId.toString())
            .collection("posts")
            .document(postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            @Suppress("UNCHECKED_CAST")
            val voters = (snapshot.get("voters") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()
            if (voters.containsKey(uid)) {
                return@runTransaction
            }

            @Suppress("UNCHECKED_CAST")
            val pollVotes = (snapshot.get("pollVotes") as? List<Long>)?.toMutableList() ?: mutableListOf()
            if (optionIndex in pollVotes.indices) {
                pollVotes[optionIndex] = pollVotes[optionIndex] + 1
                voters[uid] = optionIndex.toLong()
                transaction.update(postRef, "pollVotes", pollVotes)
                transaction.update(postRef, "voters", voters)
            }
        }.await()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
