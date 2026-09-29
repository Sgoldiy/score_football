package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.MatchDetail
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FixtureDetailViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    data class ChatMessage(
        val id: String,
        val username: String,
        val text: String,
        val timestamp: Long,
        val isSystem: Boolean = false
    )

    private val _detailState = MutableStateFlow<ApiResult<MatchDetail>>(ApiResult.Loading)
    val detailState: StateFlow<ApiResult<MatchDetail>> = _detailState

    private val _userVote = MutableStateFlow<Int?>(null)
    val userVote: StateFlow<Int?> = _userVote

    private val _comments = MutableStateFlow<List<ChatMessage>>(emptyList())
    val comments: StateFlow<List<ChatMessage>> = _comments

    fun loadFixtureDetails(fixtureId: Int) {
        viewModelScope.launch {
            _detailState.value = ApiResult.Loading
            _detailState.value = repository.getMatchDetail(fixtureId)
        }
    }

    /** Records the user's own prediction (no fake crowd percentages — the API has no poll data). */
    fun submitVote(choice: Int) {
        _userVote.value = choice
    }

    fun sendComment(text: String, username: String) {
        val newMsg = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            username = username.ifBlank { "AnonymousFan" },
            text = text,
            timestamp = System.currentTimeMillis()
        )
        _comments.value = _comments.value + newMsg
    }
}
