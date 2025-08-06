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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
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
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.LanguagePreferences
import com.jericx.trainr.presentation.common.LocaleManager
import com.jericx.trainr.presentation.common.NavigationStateManager
import com.jericx.trainr.presentation.common.components.InfiniteHorizontalPager
import com.jericx.trainr.presentation.common.components.LanguageSelector
import com.jericx.trainr.presentation.common.theme.Orange500
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingButton

data class OnboardingPage(
    val imageRes: Int,
    val title: String
)

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    onLanguageChanged: ((LanguagePreferences.Language) -> Unit)? = null
) {
    val context = LocalContext.current
    val languagePreferences = remember { LanguagePreferences(context) }
    var currentLanguage by remember { mutableStateOf(languagePreferences.getCurrentLanguageObject(context)) }
    val pages = listOf(
        OnboardingPage(R.drawable.img_skipping, stringResource(R.string.personalized_workout_plans)),
        OnboardingPage(R.drawable.img_exercising, stringResource(R.string.ai_generated_routines)),
        OnboardingPage(R.drawable.img_task_done, stringResource(R.string.track_your_progress))
    )

    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            WelcomeHeader()

        Spacer(modifier = Modifier.height(Spacing.large))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_rectangle),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(vertical = Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

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

                OnboardingButton(
                    text = stringResource(R.string.get_started),
                    onClick = onGetStartedClick,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                )
            }
        }
        }

        LanguageSelector(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                languagePreferences.setLanguage(context, language.code)
                currentLanguage = language
                onLanguageChanged?.invoke(language)

                NavigationStateManager.setLanguageChangePending(context, true)
                val activity = context as? ComponentActivity
                activity?.let {
                    LocaleManager.setAppLocale(it, language.code)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = Spacing.medium,
                    end = Spacing.medium
                ),
            compact = true
        )
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
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@Composable
private fun WelcomeHeader() {
    val fugazOne = FontFamily(Font(R.font.fugazone_regular))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large)
            .padding(top = Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.welcome_to) + " ",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = fugazOne,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Image(
                painter = painterResource(id = R.drawable.img_trainr),
                contentDescription = stringResource(R.string.trainr),
                modifier = Modifier.height(42.dp),
                contentScale = ContentScale.FillHeight
            )
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.your) + " ")
                withStyle(style = SpanStyle(color = Orange500)) {
                    append(stringResource(R.string.ai_powered))
                }
                append(" " + stringResource(R.string.personal_trainer))
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    .size(8.dp)
                    .background(
                        if (index == currentPage)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small
                    )
            )
            if (index < pageCount - 1) {
                Spacer(modifier = Modifier.width(Spacing.small))
            }
        }
    }
}