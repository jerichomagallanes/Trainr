package com.jericx.trainr.presentation.unstuck

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrOptionRow
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextAction
import com.jericx.trainr.presentation.common.components.core.TrainrTextArea
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun AdjustContextScreen(
    note: String,
    interpreter: InterpreterUi,
    modifier: Modifier = Modifier,
    isInterpreting: Boolean = false,
    hint: ContextHint? = null,
    onTypeNote: (String) -> Unit = {},
    onChoose: (DirectReason) -> Unit = {},
    onUseNote: () -> Unit = {},
    onInstallModel: () -> Unit = {},
    onCancelInstall: () -> Unit = {},
    onOpenLicence: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                if (interpreter == InterpreterUi.Ready) {
                    TrainrButton(
                        text = stringResource(
                            if (isInterpreting) R.string.context_reading_note else R.string.context_use_note
                        ),
                        onClick = {
                            focus.clearFocus()
                            keyboard?.hide()
                            onUseNote()
                        },
                        enabled = note.isNotBlank() && !isInterpreting
                    )
                }
                TrainrQuietButton(
                    text = stringResource(R.string.back_to_workout),
                    onClick = onBack
                )
            }
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.context_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.context_prompt),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.medium)
            )
            TrainrTextArea(
                value = note,
                onValueChange = onTypeNote,
                placeholder = stringResource(R.string.context_placeholder),
                modifier = Modifier.padding(top = Spacing.small)
            )
            Text(
                text = stringResource(R.string.context_private),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )

            PrivateCoaching(
                interpreter = interpreter,
                onInstall = onInstallModel,
                onCancel = onCancelInstall,
                onOpenLicence = onOpenLicence
            )

            Text(
                text = stringResource(R.string.context_which_first),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.large)
            )
            hint?.let {
                Text(
                    text = stringResource(
                        when (it) {
                            ContextHint.CHOOSER -> R.string.context_hint_chooser
                            ContextHint.GUIDE -> R.string.context_hint_guide
                            ContextHint.FAILED -> R.string.context_hint_failed
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                    modifier = Modifier.padding(top = Spacing.tight)
                )
            }
            Column(
                modifier = Modifier.padding(top = Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                TrainrOptionRow(
                    title = stringResource(R.string.context_option_time),
                    description = stringResource(R.string.context_option_time_hint),
                    onClick = { onChoose(DirectReason.LESS_TIME) },
                    enabled = !isInterpreting
                )
                TrainrOptionRow(
                    title = stringResource(R.string.context_option_equipment),
                    description = stringResource(R.string.context_option_equipment_hint),
                    onClick = { onChoose(DirectReason.EQUIPMENT) },
                    enabled = !isInterpreting
                )
            }
        }
    }
}

@Composable
private fun PrivateCoaching(
    interpreter: InterpreterUi,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onOpenLicence: () -> Unit
) {
    val colors = MaterialTheme.trainrColors

    when (interpreter) {
        InterpreterUi.NotInstalled -> {
            TrainrOptionRow(
                title = stringResource(R.string.private_coaching_setup_title),
                description = stringResource(R.string.private_coaching_setup_message),
                onClick = onInstall,
                modifier = Modifier.padding(top = Spacing.medium)
            )
            TrainrTextAction(
                text = stringResource(R.string.private_coaching_licence),
                onClick = onOpenLicence,
                modifier = Modifier.padding(top = Spacing.tight)
            )
        }

        is InterpreterUi.Downloading -> {
            Text(
                text = stringResource(R.string.private_coaching_downloading_format, interpreter.percent),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.medium)
            )
            TrainrProgress(
                currentStep = interpreter.percent,
                totalSteps = 100,
                modifier = Modifier.padding(top = Spacing.small)
            )
            TrainrTextAction(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.padding(top = Spacing.tight)
            )
        }

        InterpreterUi.Verifying -> Notice(R.string.private_coaching_verifying)

        InterpreterUi.InsufficientStorage -> Notice(R.string.private_coaching_storage_message)

        InterpreterUi.Failed -> {
            Notice(R.string.private_coaching_failed_message)
            TrainrTextAction(
                text = stringResource(R.string.try_again),
                onClick = onInstall,
                modifier = Modifier.padding(top = Spacing.tight)
            )
        }

        InterpreterUi.Unsupported, InterpreterUi.Ready -> Unit
    }
}

@Composable
private fun Notice(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.trainrColors.onSurface,
        modifier = Modifier.padding(top = Spacing.medium)
    )
}

@Preview(showBackground = true)
@Composable
private fun AdjustContextScreenPreview() {
    TrainrTheme {
        AdjustContextScreen(note = "", interpreter = InterpreterUi.NotInstalled)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdjustContextScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        AdjustContextScreen(
            note = "Gym is busy tonight.",
            interpreter = InterpreterUi.Ready,
            hint = ContextHint.CHOOSER
        )
    }
}
