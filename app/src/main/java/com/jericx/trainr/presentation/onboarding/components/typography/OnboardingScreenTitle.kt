package com.jericx.trainr.presentation.onboarding.components.typography

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R

@Composable
fun OnboardingScreenTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val fugazOne = FontFamily(Font(R.font.fugazone_regular))

    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = fugazOne,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            letterSpacing = 0.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}