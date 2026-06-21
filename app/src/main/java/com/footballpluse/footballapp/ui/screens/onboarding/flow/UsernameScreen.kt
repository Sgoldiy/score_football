package com.footballpluse.footballapp.ui.screens.onboarding.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingEvent
import com.footballpluse.footballapp.ui.screens.onboarding.OnboardingUiState
import com.footballpluse.footballapp.ui.screens.onboarding.UsernameStatus
import com.footballpluse.footballapp.ui.theme.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun UsernameScreen(
    state: OnboardingUiState,
    events: SharedFlow<OnboardingEvent>,
    onUsernameChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateNext: () -> Unit,
    onRandomizeUsername: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is OnboardingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    val borderColor = when (state.usernameStatus) {
        is UsernameStatus.Available -> Color(0xFF4ADE80)
        is UsernameStatus.Taken,
        is UsernameStatus.Invalid   -> Color(0xFFEF4444)
        is UsernameStatus.Checking  -> Color(0xFF4ADE80)
        else                        -> Color(0xFF2A2A2A)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkGrayBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append("Choose Your ")
                        }
                        withStyle(style = SpanStyle(color = GreenAccent, fontWeight = FontWeight.Bold)) {
                            append("Username")
                        }
                    },
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This is how other fans will see you. Make it unique.",
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = onUsernameChanged,
                        placeholder = {
                            Text("username", color = Color(0xFF444444))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = borderColor,
                            cursorColor = Color(0xFF4ADE80),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (state.usernameStatus is UsernameStatus.Available) {
                                    onSubmit()
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A1A1A))
                            .clickable { onRandomizeUsername() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Casino,
                            contentDescription = "Random username",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.username.isNotBlank()) {
                        val letter = state.username.first().uppercaseChar()
                        Text(
                            text = letter.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80)
                        )
                    } else {
                        Text(
                            text = "?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444444)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "@${state.username.ifBlank { "username" }}",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (val status = state.usernameStatus) {
                    is UsernameStatus.Idle -> {
                        Text(
                            text = "3-20 characters, letters, numbers, underscores only",
                            color = Color(0xFF555555),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is UsernameStatus.Invalid -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u26A0 ", color = Color(0xFFF59E0B), fontSize = 12.sp)
                            Text(status.reason, color = Color(0xFFF59E0B), fontSize = 12.sp)
                        }
                    }

                    is UsernameStatus.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFF4ADE80),
                                strokeWidth = 1.5.dp
                            )
                            Text(
                                "Checking availability...",
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                        }
                    }

                    is UsernameStatus.Available -> {
                        Text(
                            text = "\u2713 @${status.username} is available!",
                            color = Color(0xFF4ADE80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    is UsernameStatus.Taken -> {
                        Column {
                            Text(
                                text = "\u2717 @${status.username} is already taken.",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp
                            )

                            if (state.suggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Try one of these:",
                                    color = Color(0xFF555555),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.suggestions) { suggestion ->
                                        SuggestionChip(
                                            onClick = { onUsernameChanged(suggestion) },
                                            label = {
                                                Text(
                                                    "@$suggestion",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFAAAAAA)
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFF1E1E1E)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF333333))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is UsernameStatus.Typing -> {
                        Text(
                            text = "3-20 characters, letters, numbers, underscores only",
                            color = Color(0xFF555555),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            val canContinue = state.usernameStatus is UsernameStatus.Available

            Button(
                onClick = onSubmit,
                enabled = canContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canContinue) Color(0xFF4ADE80) else Color(0xFF1E3A1E),
                    contentColor = if (canContinue) Color(0xFF000000) else Color(0xFF4A7A4A),
                    disabledContainerColor = Color(0xFF1E3A1E),
                    disabledContentColor = Color(0xFF4A7A4A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun UsernameScreenPreview() {
    FootballPlusTheme(darkTheme = true) {
        UsernameScreen(
            state = OnboardingUiState(username = "striker", usernameStatus = UsernameStatus.Available("striker")),
            events = kotlinx.coroutines.flow.MutableSharedFlow(),
            onUsernameChanged = {},
            onSubmit = {},
            onNavigateNext = {},
            onRandomizeUsername = {}
        )
    }
}
