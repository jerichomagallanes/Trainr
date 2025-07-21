package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun GeneratingScreen(
    onGenerationComplete: () -> Unit
) {
    var activeIndicator by remember { mutableIntStateOf(0) }
    val totalIndicators = 14

    LaunchedEffect(Unit) {
        while (true) {
            for (i in 0 until totalIndicators) {
                activeIndicator = i
                delay(100)
            }
            delay(200)
        }
    }

    LaunchedEffect(Unit) {
        delay(8000)
        onGenerationComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_trainr),
                contentDescription = stringResource(R.string.trainr),
                modifier = Modifier
                    .height(48.dp),
                contentScale = ContentScale.FillHeight
            )
            
            Spacer(modifier = Modifier.height(Spacing.extraLarge * 2))

            Text(
                text = stringResource(R.string.generating_your_workout_routine),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Spacing.extraLarge))

            LoadingIndicator(
                activeIndex = activeIndicator,
                totalCount = totalIndicators
            )
        }
    }
}

@Composable
private fun LoadingIndicator(
    activeIndex: Int,
    totalCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalCount) { index ->
            val isActive = index <= activeIndex
            val alpha = when {
                index == activeIndex -> 1f
                index == activeIndex - 1 -> 0.8f
                index == activeIndex - 2 -> 0.6f
                isActive -> 1f
                else -> 0.3f
            }
            
            Image(
                painter = painterResource(
                    id = if (isActive) R.drawable.img_rectangle_27 else R.drawable.img_rectangle_37
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 12.dp, height = 24.dp)
                    .alpha(alpha)
            )
        }
    }
}