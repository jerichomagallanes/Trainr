package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.layout.InfiniteHorizontalPager
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.themedPainter
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.common.components.core.TrainrButton

private data class OnboardingPage(
    val imageRes: Int,
    val nightImageRes: Int,
    val title: String
)

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            R.drawable.img_skipping,
            R.drawable.img_skipping_night,
            stringResource(R.string.personalized_workout_plans)
        ),
        OnboardingPage(
            R.drawable.img_exercising,
            R.drawable.img_exercising_night,
            stringResource(R.string.routines_built_around_you)
        ),
        OnboardingPage(
            R.drawable.img_task_done,
            R.drawable.img_task_done_night,
            stringResource(R.string.track_your_progress)
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        WelcomeHeader()

        // The illustration is sized from the height as well as the width, so a
        // short screen shrinks the picture rather than pushing the page dots and
        // the button off the bottom. The height factor is loose enough that a
        // normal phone is unaffected and only genuinely short screens give way.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val illustration = minOf(maxWidth * 0.65f, maxHeight * 0.45f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(vertical = Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The carousel scrolls and the button does not: on a short
                // screen the illustration is what should give way, and a
                // primary action pushed past the bottom edge cannot be tapped
                // at all.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
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
                        OnboardingPageContent(page = page, illustration = illustration)
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    PageIndicator(
                        pageCount = pages.size,
                        currentPage = currentPage
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.large))

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
private fun OnboardingPageContent(page: OnboardingPage, illustration: Dp) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(illustration)
                .clip(MaterialTheme.shapes.large)
        ) {
            Image(
                painter = themedPainter(page.imageRes, page.nightImageRes),
                contentDescription = page.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        Text(
            text = page.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.trainrColors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

// Measured from the physical screen top, so the status bar inset the host
// already consumes is subtracted back out.
private val HeaderTopMargin = 151.dp

// On a short screen that margin is a quarter of the height, which is what
// pushed the page dots and the caption off the bottom. Below this it steps down.
private val ShortScreenHeight = 700.dp
private val ShortScreenTopMargin = 96.dp

// The width the title and the wordmark need side by side at full size. Below it
// both step down together, because a Row will let them overflow rather than
// shrink, and the wordmark is an image with no smaller size to fall back on.
private val HeaderFullWidth = 380.dp

@Composable
private fun WelcomeHeader() {
    val consumedInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val topMargin = if (maxHeight < ShortScreenHeight) {
            ShortScreenTopMargin
        } else {
            HeaderTopMargin
        }
        val isNarrow = maxWidth < HeaderFullWidth
        val titleSize = if (isNarrow) 24.sp else 30.sp
        val markHeight = if (isNarrow) 42.dp else 52.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(top = (topMargin - consumedInset).coerceAtLeast(0.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.welcome_to) + " ",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = titleSize),
                    color = MaterialTheme.trainrColors.onSurface,
                    maxLines = 1
                )

                Image(
                    painter = themedPainter(R.drawable.img_trainr, R.drawable.img_trainr_night),
                    contentDescription = stringResource(R.string.trainr),
                    modifier = Modifier.height(markHeight),
                    contentScale = ContentScale.FillHeight
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.your) + " ")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.trainrColors.brandStrong,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(stringResource(R.string.trainer_adjective))
                    }
                    append(" " + stringResource(R.string.personal_trainer))
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.trainrColors.onSurface,
                textAlign = TextAlign.Center
            )
        }
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
                            MaterialTheme.trainrColors.onSurface
                        else
                            MaterialTheme.trainrColors.dotInactive,
                        shape = CircleShape
                    )
            )
            if (index < pageCount - 1) {
                Spacer(modifier = Modifier.width(Spacing.tight))
            }
        }
    }
}
