package com.footballpluse.footballapp.ui.screens.onboarding.flow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.ui.theme.DarkGrayBackground
import com.footballpluse.footballapp.ui.theme.FootballPlusTheme
import com.footballpluse.footballapp.ui.theme.GreenAccent
import com.footballpluse.footballapp.ui.theme.TextMutedGray

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    BackHandler(enabled = true) { /* no-op: start of onboarding flow */ }

    // Dimensions
    val topSpacerFraction = 0.3f
    val iconSize = 80.dp
    val iconSpace = 18.dp
    val textSpace = 8.dp
    val textFontSize = 32.sp
    val subtitleFontSize = 14.sp
    val paddingHorizontal = 24.dp
    val paddingBottom = 48.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGrayBackground)
            .padding(horizontal = paddingHorizontal)
            .padding(bottom = paddingBottom),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(topSpacerFraction))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.wrapContentSize()
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsSoccer,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(iconSpace))
            
            val appNameString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append(stringResource(id = R.string.app_name_football))
                }
                append(" ")
                withStyle(style = SpanStyle(color = GreenAccent, fontWeight = FontWeight.Bold)) {
                    append(stringResource(id = R.string.app_name_plus))
                }
            }

            Text(
                text = appNameString,
                fontSize = textFontSize,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(textSpace))
            Text(
                text = stringResource(id = R.string.onboarding_welcome_subtitle),
                fontSize = subtitleFontSize,
                color = TextMutedGray,
                textAlign = TextAlign.Center
            )
        }

        OnboardingPrimaryButton(
            text = stringResource(id = R.string.onboarding_btn_get_started),
            enabled = true,
            onClick = onGetStarted
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun WelcomeScreenPreview() {
    FootballPlusTheme(darkTheme = true) {
        WelcomeScreen(onGetStarted = {})
    }
}
