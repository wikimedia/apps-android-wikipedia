package org.wikipedia.createaccount

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.AppTextButton
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun CreateAccountEncourageScreen(
    modifier: Modifier = Modifier,
    uiState: CreateAccountEncourageViewModel.UiState,
    onCloseClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onMaybeLaterClick: () -> Unit
) {
    val cards = encourageCards(uiState)
    val pagerState = rememberPagerState(pageCount = { cards.size })

    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                    pagerState = pagerState
                )

                AppButton(
                    onClick = onCreateAccountClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.create_account_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                AppTextButton(
                    onClick = onMaybeLaterClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_maybe_later),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            IconButton(
                modifier = Modifier.padding(start = 8.dp),
                onClick = onCloseClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_black_24dp),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = stringResource(R.string.dialog_close_description)
                )
            }

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                text = stringResource(R.string.create_account_encourage_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W500),
                color = WikipediaTheme.colors.primaryColor
            )

            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState,
                contentPadding = PaddingValues(start = 16.dp, end = 32.dp),
                pageSpacing = 8.dp
            ) { pageIndex ->
                EncourageCardView(
                    modifier = Modifier.fillMaxHeight(),
                    card = cards[pageIndex]
                )
            }
        }
    }
}

@Composable
private fun EncourageCardView(
    modifier: Modifier = Modifier,
    card: EncourageCard
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = card.backgroundColor,
            contentColor = card.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    modifier = Modifier.size(200.dp),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(card.imageRes)
                        .build(),
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W500),
                color = ComposeColors.Gray700
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = card.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ComposeColors.Gray700
            )
        }
    }
}

@Composable
private fun encourageCards(uiState: CreateAccountEncourageViewModel.UiState): List<EncourageCard> {
    return listOf(
        EncourageCard(
            backgroundColor = ComposeColors.Yellow100,
            imageRes = R.drawable.yir_puzzle_clock,
            title = pluralStringResource(R.plurals.create_account_encourage_year_in_review_title, uiState.readingDays, uiState.readingDays),
            description = stringResource(R.string.create_account_encourage_year_in_review_description)
        ),
        EncourageCard(
            backgroundColor = ComposeColors.Green200,
            imageRes = R.drawable.yir_puzzle_cloud,
            title = pluralStringResource(R.plurals.create_account_encourage_saved_articles_title, uiState.savedArticles, uiState.savedArticles),
            description = stringResource(R.string.create_account_encourage_saved_articles_description)
        ),
        EncourageCard(
            backgroundColor = ComposeColors.Blue200,
            imageRes = R.drawable.ic_yir_puzzle,
            title = pluralStringResource(R.plurals.create_account_encourage_activity_title, uiState.recentReads, uiState.recentReads),
            description = stringResource(R.string.create_account_encourage_activity_description)
        ),
        EncourageCard(
            backgroundColor = ComposeColors.Lime200,
            imageRes = R.drawable.yir_puzzle_worker,
            title = stringResource(R.string.create_account_encourage_edits_title),
            description = stringResource(R.string.create_account_encourage_edits_description)
        )
    )
}

private data class EncourageCard(
    val backgroundColor: Color,
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CreateAccountEncourageScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CreateAccountEncourageScreen(
            uiState = CreateAccountEncourageViewModel.UiState(
                readingDays = 32,
                savedArticles = 14,
                recentReads = 57
            ),
            onCloseClick = {},
            onCreateAccountClick = {},
            onMaybeLaterClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CreateAccountEncourageScreenDarkPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        CreateAccountEncourageScreen(
            uiState = CreateAccountEncourageViewModel.UiState(
                readingDays = 32,
                savedArticles = 14,
                recentReads = 57
            ),
            onCloseClick = {},
            onCreateAccountClick = {},
            onMaybeLaterClick = {}
        )
    }
}
