package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.repository.CommunityFirebaseRepository
import com.footballpluse.footballapp.data.repository.LocalRepository
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.MatchDetail
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FixtureDetailViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val communityRepository: CommunityFirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    data class ChatMessage(
        val id: String,
        val userId: String = "",
        val username: String,
        val text: String,
        val timestamp: Long,
        val reactionType: String? = null,
        val isSystem: Boolean = false,
        val isMe: Boolean = false
    )

    private val _detailState = MutableStateFlow<ApiResult<MatchDetail>>(ApiResult.Loading)
    val detailState: StateFlow<ApiResult<MatchDetail>> = _detailState

    private val _userVote = MutableStateFlow<Int?>(null)
    val userVote: StateFlow<Int?> = _userVote

    private val _comments = MutableStateFlow<List<ChatMessage>>(emptyList())
    val comments: StateFlow<List<ChatMessage>> = _comments

    private var currentFixtureId: Int? = null
    private var commentsJob: Job? = null

    fun loadFixtureDetails(fixtureId: Int) {
        if (currentFixtureId != fixtureId) {
            currentFixtureId = fixtureId
            observeLiveComments(fixtureId)
        }
        viewModelScope.launch {
            _detailState.value = ApiResult.Loading
            _detailState.value = repository.getMatchDetail(fixtureId)
        }
    }

    private fun observeLiveComments(fixtureId: Int) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            val myUid = communityRepository.getCurrentUserId()
            communityRepository.observeMatchComments(fixtureId.toString())
                .collectLatest { fbMessages ->
                    _comments.value = fbMessages.map { msg ->
                        ChatMessage(
                            id = msg.id,
                            userId = msg.userId,
                            username = msg.username,
                            text = msg.text,
                            timestamp = msg.timestamp,
                            reactionType = msg.reactionType,
                            isSystem = msg.isSystem,
                            isMe = (myUid != null && msg.userId == myUid)
                        )
                    }
                }
        }
    }

    /** Records the user's own prediction (no fake crowd percentages — the API has no poll data). */
    fun submitVote(choice: Int) {
        _userVote.value = choice
    }

    fun getDisplayName(): String {
        return localRepository.getUsernamePref()?.takeIf { it.isNotBlank() } ?: "Fan"
    }

    fun sendComment(text: String, usernameOverride: String? = null, reactionType: String? = null) {
        val fid = currentFixtureId ?: return
        if (text.isBlank()) return

        val username = usernameOverride?.takeIf { it.isNotBlank() } ?: getDisplayName()

        viewModelScope.launch {
            communityRepository.sendMatchComment(
                matchId = fid.toString(),
                text = text,
                username = username,
                reactionType = reactionType
            )
        }
    }
}
