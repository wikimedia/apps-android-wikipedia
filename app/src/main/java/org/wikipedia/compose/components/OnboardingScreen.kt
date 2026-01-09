package org.wikipedia.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    primaryButtonText: String,
    secondaryButtonText: String,
    onPrimaryOnClick: () -> Unit,
    onSecondaryOnClick: () -> Unit
) {
    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 24.dp,
                    alignment = Alignment.CenterHorizontally
                )
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
                    Text(
                        text = primaryButtonText,
                        style = MaterialTheme.typography.labelLarge,
                        color = WikipediaTheme.colors.paperColor
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            headerContent()
            content()
        }
    }
}

@Composable
fun OnboardingListItem(
    modifier: Modifier = Modifier,
    item: OnboardingItem) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        headlineContent = {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = WikipediaTheme.colors.primaryColor
            )
        },
        supportingContent = {
            Text(
                text = stringResource(item.subTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
        },
        leadingContent = {
            Icon(
                modifier = Modifier
                    .padding(top = 2.dp),
                painter = painterResource(item.icon),
                tint = WikipediaTheme.colors.progressiveColor,
                contentDescription = null
            )
        }
    )
}

data class OnboardingItem(
    val icon: Int,
    val title: Int,
    val subTitle: Int
)
