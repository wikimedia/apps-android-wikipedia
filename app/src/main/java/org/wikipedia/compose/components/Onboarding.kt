package org.wikipedia.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun OnboardingListItem(
    modifier: Modifier = Modifier,
    item: OnboardingItem
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            modifier = Modifier
                .padding(top = (2.5).dp),
            painter = painterResource(item.icon),
            tint = WikipediaTheme.colors.progressiveColor,
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = stringResource(item.subTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
        }
    }
}

@Composable
fun TwoButtonBottomBar(
    primaryButtonText: String,
    secondaryButtonText: String,
    onPrimaryOnClick: () -> Unit,
    onSecondaryOnClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            modifier = Modifier
                .weight(1f),
            border = BorderStroke(
                width = 1.dp,
                color = WikipediaTheme.colors.borderColor
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.paperColor
            ),
            onClick = onSecondaryOnClick
        ) {
            Text(
                text = secondaryButtonText,
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.progressiveColor
            )
        }

        Button(
            modifier = Modifier
                .weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor
            ),
            onClick = onPrimaryOnClick
        ) {
            AnimatedContent(
                targetState = primaryButtonText,
            ) { targetText ->
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.labelLarge,
                    color = WikipediaTheme.colors.paperColor
                )
            }
        }
    }
}

data class OnboardingItem(
    val icon: Int,
    val title: Int,
    val subTitle: Int
)

@Preview
@Composable
private fun OnboardingListItemPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnboardingListItem(
            item = OnboardingItem(
                icon = R.drawable.ic_newsstand_24,
                title = R.string.activity_tab_onboarding_reading_patterns_title,
                subTitle = R.string.activity_tab_onboarding_reading_patterns_message
            )
        )
    }
}
