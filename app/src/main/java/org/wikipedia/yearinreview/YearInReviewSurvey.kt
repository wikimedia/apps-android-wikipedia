package org.wikipedia.yearinreview

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewSurvey(
    onSubmitButtonClick: (String, String) -> Unit,
    onCancelButtonClick: () -> Unit
) {
    val radioOptions = listOf(
        stringResource(R.string.year_in_review_survey_very_satisfied),
        stringResource(R.string.year_in_review_survey_satisfied),
        stringResource(R.string.year_in_review_survey_neutral),
        stringResource(R.string.year_in_review_survey_unsatisfied),
        stringResource(R.string.year_in_review_survey_very_unsatisfied)
    )

    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[2]) }
    var userInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    BasicAlertDialog(
        onDismissRequest = { /* no dismissal unless SurveyButton used */ }
    ) {
        Surface(
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(28.dp))
                .background(color = WikipediaTheme.colors.paperColor)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .background(color = WikipediaTheme.colors.paperColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .wrapContentHeight()
                            .align(Alignment.Center)
                    ) {
                        Text(
                            text = stringResource(R.string.year_in_review_survey_title),
                            style = WikipediaTheme.typography.h2,
                            color = WikipediaTheme.colors.primaryColor,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                        Text(
                            text = stringResource(R.string.year_in_review_survey_subtitle),
                            style = WikipediaTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.secondaryColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = WikipediaTheme.colors.inactiveColor
                    )
                }
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    radioOptions.forEach { text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (text == selectedOption),
                                    onClick = { onOptionSelected(text) },
                                    role = Role.RadioButton
                                )
                                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = (text == selectedOption),
                                colors = RadioButtonColors(
                                    selectedColor = WikipediaTheme.colors.progressiveColor,
                                    unselectedColor = WikipediaTheme.colors.progressiveColor,
                                    disabledSelectedColor = WikipediaTheme.colors.inactiveColor,
                                    disabledUnselectedColor = WikipediaTheme.colors.inactiveColor
                                ),
                                onClick = null,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .scale(1.2f)
                            )
                            Text(
                                text = text,
                                style = WikipediaTheme.typography.bodyLarge,
                                color = WikipediaTheme.colors.primaryColor,
                            )
                        }
                    }
                }
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    textStyle = WikipediaTheme.typography.button,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WikipediaTheme.colors.progressiveColor,
                        unfocusedIndicatorColor = WikipediaTheme.colors.progressiveColor,
                        focusedContainerColor = WikipediaTheme.colors.backgroundColor,
                        unfocusedContainerColor = WikipediaTheme.colors.backgroundColor,
                        focusedTextColor = WikipediaTheme.colors.primaryColor
                    ),
                    placeholder = {
                        if (userInput.isEmpty()) {
                            Text(
                                text = stringResource(R.string.year_in_review_survey_placeholder_text),
                                color = WikipediaTheme.colors.secondaryColor,
                                style = WikipediaTheme.typography.bodyLarge
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = WikipediaTheme.colors.inactiveColor
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .align(Alignment.Center)
                            .padding(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                    ) {
                        SurveyButton(
                            buttonText = R.string.year_in_review_survey_cancel,
                            onClick = { onCancelButtonClick() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SurveyButton(
                            buttonText = R.string.year_in_review_survey_submit,
                            onClick = { onSubmitButtonClick(selectedOption, userInput) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurveyButton(
    @StringRes buttonText: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = WikipediaTheme.colors.paperColor,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Text(
            text = stringResource(buttonText),
            style = WikipediaTheme.typography.button,
            color = WikipediaTheme.colors.progressiveColor
        )
    }
}

@Preview
@Composable
fun PreviewSurvey() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        Column(
           modifier = Modifier
               .fillMaxSize()
               .background(WikipediaTheme.colors.paperColor)
        ) {
            YearInReviewSurvey(
                onSubmitButtonClick = { _, _ ->
                    /*No logic, preview only*/
                 },
                onCancelButtonClick = { /*No logic, preview only*/ },
            )
        }
    }
}
