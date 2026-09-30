package org.wikipedia.search.semantic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.L10nUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemanticSearchFeedbackScreen(
    modifier: Modifier = Modifier,
    languageCode: String,
    selectedRating: Boolean?,
    onRatingClick: (Boolean) -> Unit,
    onCloseClick: () -> Unit,
    onSubmitClick: (isPositive: Boolean?, feedbackText: String) -> Unit
) {
    val layoutDirection =
        if (L10nUtil.isLangRTL(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                color = WikipediaTheme.colors.inactiveColor
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.semantic_search_feedback_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = WikipediaTheme.colors.primaryColor
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd),
                    onClick = onCloseClick
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_black_24dp),
                        contentDescription = stringResource(R.string.semantic_search_feedback_dialog_close_button_content_description),
                        tint = WikipediaTheme.colors.primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SemanticSearchFeedbackContent(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WikipediaTheme.colors.paperColor),
                isVisible = true,
                selectedRating = selectedRating,
                isInputAlwaysVisible = true,
                onRatingClick = onRatingClick,
                onSubmitClick = onSubmitClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SemanticSearchFeedbackScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SemanticSearchFeedbackScreen(
            languageCode = "en",
            selectedRating = null,
            onRatingClick = {},
            onCloseClick = {},
            onSubmitClick = { _, _ -> }
        )
    }
}
