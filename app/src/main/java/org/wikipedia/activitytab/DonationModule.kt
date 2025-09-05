package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState

@Composable
fun DonationModule(
    modifier: Modifier = Modifier,
    uiState: UiState<String?>,
    onClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        onClick = onClick
    ) {
        if (uiState == UiState.Loading) {
            ActivityTabShimmerView()
        } else if (uiState is UiState.Success) {
            val lastDonationTime = uiState.data ?: stringResource(R.string.activity_tab_donation_unknown)
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(R.drawable.outline_credit_card_heart_24),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                        HtmlText(
                            text = stringResource(R.string.activity_tab_donation_last_donation),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                            color = WikipediaTheme.colors.primaryColor,
                            lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                        )
                    }
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                        tint = WikipediaTheme.colors.secondaryColor,
                        contentDescription = null
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = lastDonationTime,
                    style = MaterialTheme.typography.titleLarge,
                    color = WikipediaTheme.colors.progressiveColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview
@Composable
private fun DonationModulePreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        DonationModule(
            uiState = UiState.Success("5 days ago")
        )
    }
}
