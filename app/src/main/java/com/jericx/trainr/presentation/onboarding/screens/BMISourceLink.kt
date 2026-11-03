package com.jericx.trainr.presentation.onboarding.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

// Shared by the result and explanation. No profile values enter the URLs.
internal enum class BMISource(@param:StringRes val title: Int, val url: String) {
    Calculation(R.string.bmi_cdc_calculation, "https://www.cdc.gov/bmi/about/index.html"),
    Categories(R.string.bmi_cdc_categories, "https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html"),
    Limitations(R.string.bmi_cdc_about, "https://www.cdc.gov/bmi/about/index.html")
}

@Composable
internal fun BMISourceLink(source: BMISource) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = stringResource(source.title),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.trainrColors.brandStrong,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) { uriHandler.openUri(source.url) }
            .padding(vertical = Spacing.small)
    )
}
