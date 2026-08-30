package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.layout.InfiniteHorizontalPager
import com.jericx.trainr.presentation.common.theme.Orange500
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.components.core.TrainrButton

data class OnboardingPage(
    val imageRes: Int,
    val title: String
)

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(R.drawable.img_skipping, stringResource(R.string.personalized_workout_plans)),
        OnboardingPage(R.drawable.img_exercising, stringResource(R.string.ai_generated_routines)),
        OnboardingPage(R.drawable.img_task_done, stringResource(R.string.track_your_progress))
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        WelcomeHeader()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(vertical = Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                InfiniteHorizontalPager(
                    items = pages,
                    modifier = Modifier.fillMaxWidth(),
                    onPageChanged = { page ->
                        currentPage = pages.indexOf(page)
                    }
                ) { page ->
                    OnboardingPageContent(page = page)
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                PageIndicator(
                    pageCount = pages.size,
                    currentPage = currentPage
                )

                Spacer(modifier = Modifier.weight(1f))

                TrainrButton(
                    text = stringResource(R.string.get_started),
                    onClick = onGetStartedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.large)
        ) {
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = page.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        Text(
            text = page.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

// Frame 325:2 measures the title 151dp from the physical screen top with the
// status bar floating inside that margin, but the host already consumes the
// status bar inset, so it is subtracted back out here.
private val HeaderTopMargin = 151.dp

@Composable
private fun WelcomeHeader() {
    val consumedInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large)
            .padding(top = (HeaderTopMargin - consumedInset).coerceAtLeast(0.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.welcome_to) + " ",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Image(
                painter = painterResource(id = R.drawable.img_trainr),
                contentDescription = stringResource(R.string.trainr),
                modifier = Modifier.height(52.dp),
                contentScale = ContentScale.FillHeight
            )
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.your) + " ")
                withStyle(style = SpanStyle(color = Orange500, fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.ai_powered))
                }
                append(" " + stringResource(R.string.personal_trainer))
            },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (index == currentPage)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
            if (index < pageCount - 1) {
                Spacer(modifier = Modifier.width(Spacing.tight))
            }
        }
    }
}