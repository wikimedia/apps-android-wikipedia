package org.wikipedia.yearinreview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.wikipedia.compose.theme.WikipediaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewSurvey() {
    val radioOptions = listOf(
        "Very satisfied",
        "Satisfied",
        "Neutral",
        "Unsatisfied",
        "Very unsatisfied"
    )
    val openSurvey = remember { mutableStateOf(true) }
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
    var userInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    if (openSurvey.value) {
        BasicAlertDialog(
            onDismissRequest = { openSurvey.value = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
        ) {
            Surface(
                tonalElevation = AlertDialogDefaults.TonalElevation,
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(16.dp))
                    .background(color = WikipediaTheme.colors.paperColor)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(scrollState)

                ) {
                    Text(
                        text = "Help Improve Year In Review",
                        style = WikipediaTheme.typography.h2,
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.padding(top = 20.dp, bottom = 5.dp)
                    )
                    Text(
                        text = "Are you satisfied with this feature?",
                        style = WikipediaTheme.typography.p,
                        color = WikipediaTheme.colors.placeholderColor,
                        modifier = Modifier.padding(top = 5.dp, bottom = 10.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth(),
                        color = WikipediaTheme.colors.inactiveColor
                    )

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
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (text == selectedOption),
                                    colors = RadioButtonColors(
                                        selectedColor = WikipediaTheme.colors.progressiveColor,
                                        unselectedColor = WikipediaTheme.colors.progressiveColor,
                                        disabledSelectedColor = WikipediaTheme.colors.inactiveColor,
                                        disabledUnselectedColor = WikipediaTheme.colors.inactiveColor
                                    ),
                                    onClick = null
                                )
                                Text(
                                    text = text,
                                    style = WikipediaTheme.typography.p,
                                    modifier = Modifier.padding(start = 16.dp)
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
                            unfocusedIndicatorColor = WikipediaTheme.colors.progressiveColor
                        ),
                        placeholder = {
                            if (userInput.isEmpty()) {
                                Text(
                                    text = "Any additional thoughts?",
                                    color = WikipediaTheme.colors.placeholderColor,
                                    style = WikipediaTheme.typography.p

                                )
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth(),
                        color = WikipediaTheme.colors.inactiveColor
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 32.dp)
                            .fillMaxWidth()
                            .height(72.dp)

                    ){
                        Text(
                            text = "Cancel",
                            style = WikipediaTheme.typography.button,
                            color = WikipediaTheme.colors.progressiveColor,
                            modifier = Modifier.clickable {
                                openSurvey.value = false
                            }
                        )
                        Spacer(modifier = Modifier.width(32.dp))
                        Text(
                            text = "Submit",
                            style = WikipediaTheme.typography.button,
                            color = WikipediaTheme.colors.progressiveColor,
                            modifier = Modifier.clickable {
                                /* TODO: Add click behavior */
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewSurvey() {
    YearInReviewSurvey()
}