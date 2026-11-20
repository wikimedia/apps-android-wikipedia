package org.wikipedia.search.semantic

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.getString
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

    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                    text = context.getString(languageCode, R.string.semantic_search_feedback_dialog_title),
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
                        contentDescription = context.getString(languageCode, R.string.semantic_search_feedback_dialog_close_button_content_description),
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
                languageCode = languageCode,
                isVisible = true,
                selectedRating = selectedRating,
                isInputAlwaysVisible = true,
                onRatingClick = onRatingClick,
                onSubmitClick = onSubmitClick
            )
        }
    }
}

@Composable
fun SemanticSearchFeedbackContent(
    modifier: Modifier,
    languageCode: String,
    isVisible: Boolean,
    selectedRating: Boolean?,
    isInputAlwaysVisible: Boolean = false,
    onRatingClick: (Boolean) -> Unit,
    onSubmitClick: (isPositive: Boolean?, feedbackText: String) -> Unit
) {
    val context = LocalContext.current
    val feedbackTextState = rememberTextFieldState()
    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = context.getString(languageCode, R.string.semantic_search_results_feedback_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                SemanticSearchFeedbackRatingButton(
                    iconRes = R.drawable.ic_thumb_up,
                    selectedIconRes = R.drawable.ic_thumb_up_filled,
                    contentDescription = context.getString(languageCode, R.string.semantic_search_results_feedback_thumb_up_content_description),
                    isSelected = selectedRating == true,
                    onClick = { onRatingClick(true) }
                )
                SemanticSearchFeedbackRatingButton(
                    iconRes = R.drawable.ic_thumb_down,
                    selectedIconRes = R.drawable.ic_thumb_down_filled,
                    contentDescription = context.getString(languageCode, R.string.semantic_search_results_feedback_thumb_down_content_description),
                    isSelected = selectedRating == false,
                    onClick = { onRatingClick(false) }
                )
            }

            AnimatedVisibility(
                visible = isInputAlwaysVisible || selectedRating != null
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = WikipediaTheme.colors.paperColor),
                        state = feedbackTextState,
                        inputTransformation = InputTransformation.maxLength(230), // required by the API
                        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 4),
                        shape = RoundedCornerShape(4.dp),
                        placeholder = {
                            Text(
                                text = context.getString(languageCode, R.string.semantic_search_results_feedback_input_hint)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WikipediaTheme.colors.primaryColor,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = WikipediaTheme.colors.primaryColor,
                            errorTextColor = WikipediaTheme.colors.primaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AppButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = selectedRating != null,
                        onClick = {
                            focusManager.clearFocus()
                            onSubmitClick(selectedRating, feedbackTextState.text.toString().trim())
                        }
                    ) {
                        Text(
                            text = context.getString(languageCode, R.string.semantic_search_results_feedback_submit_button_text),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SemanticSearchFeedbackRatingButton(
    @DrawableRes iconRes: Int,
    @DrawableRes selectedIconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier.semantics { selected = isSelected },
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(if (isSelected) selectedIconRes else iconRes),
            contentDescription = contentDescription,
            tint = WikipediaTheme.colors.primaryColor
        )
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
