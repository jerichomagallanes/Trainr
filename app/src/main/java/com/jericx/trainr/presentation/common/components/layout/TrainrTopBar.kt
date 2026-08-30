package com.jericx.trainr.presentation.common.components.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainrTopBar(
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showLogo: Boolean = true,
    // A close is a way OUT of a detour, where a back arrow would promise a
    // step backwards through a flow that is not there.
    closeInsteadOfBack: Boolean = false
) {
    TopAppBar(
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = if (closeInsteadOfBack) {
                            Icons.Filled.Close
                        } else {
                            Icons.AutoMirrored.Filled.ArrowBack
                        },
                        contentDescription = stringResource(
                            if (closeInsteadOfBack) R.string.close else R.string.back
                        ),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        title = {
            if (showLogo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_trainr),
                        contentDescription = stringResource(R.string.trainr),
                        modifier = Modifier
                            .height(32.dp)
                            .offset(x = (-Spacing.large)),
                        contentScale = ContentScale.FillHeight
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun TrainrTopBarPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrTopBar(onBackClick = {})
            TrainrTopBar(onBackClick = {}, showLogo = false)
        }
    }
}
