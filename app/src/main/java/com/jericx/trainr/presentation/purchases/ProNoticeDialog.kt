package com.jericx.trainr.presentation.purchases

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.trainrColors

// A restore that found nothing looks identical to one that worked unless it is
// said out loud, so both outcomes are spoken here.
@Composable
fun ProNoticeDialog(
    @StringRes noticeRes: Int?,
    onDismiss: () -> Unit
) {
    if (noticeRes == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = stringResource(noticeRes)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    color = MaterialTheme.trainrColors.brandStrong
                )
            }
        }
    )
}
