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

    private val _pollPercentages = MutableStateFlow(Triple(42, 23, 35))
    val pollPercentages: StateFlow<Triple<Int, Int, Int>> = _pollPercentages

    private val _comments = MutableStateFlow<List<ChatMessage>>(emptyList())
    val comments: StateFlow<List<ChatMessage>> = _comments

    init {
        // startCommentSimulator()
    }

    fun loadFixtureDetails(fixtureId: Int) {
        viewModelScope.launch {
            _detailState.value = ApiResult.Loading
            _detailState.value = repository.getMatchDetail(fixtureId)
        }
    }

    fun submitVote(choice: Int) {
        _userVote.value = choice
        val current = _pollPercentages.value
        _pollPercentages.value = when (choice) {
            0 -> Triple(current.first + 3, maxOf(current.second - 1, 5), maxOf(current.third - 2, 5))
            1 -> Triple(maxOf(current.first - 1, 5), current.second + 3, maxOf(current.third - 2, 5))
            else -> Triple(maxOf(current.first - 2, 5), maxOf(current.second - 1, 5), current.third + 3)
        }
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

    private fun startCommentSimulator() {
        viewModelScope.launch {
            val userNames = listOf(
                "StrikerKing", "RedDevils_7", "TacticalFocus", "MidfieldMaestro", 
                "CleanSheetGoalie", "VAR_Official", "FootyGuru", "ElClasicoFan"
            )
            val commentary = listOf(
                "What a tactical battle we are seeing here! 🧠",
                "Are they going to review that potential penalty? 🧐",
                "The atmosphere is absolutely electric! 🔥",
                "Substitutions need to happen soon to inject energy",
                "Incredible pressing from the midfield line!",
                "VAR checked... no penalty. Controversy! 🤯",
                "That save was absolute world-class! 🧤",
                "Goal of the week contender if that had gone in!"
            )
            
            while (true) {
                kotlinx.coroutines.delay(12_000)
                val randomUser = userNames.random()
                val randomText = commentary.random()
                val msg = ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    username = randomUser,
                    text = randomText,
                    timestamp = System.currentTimeMillis()
                )
                _comments.value = _comments.value + msg
            }
        }
    }
}
