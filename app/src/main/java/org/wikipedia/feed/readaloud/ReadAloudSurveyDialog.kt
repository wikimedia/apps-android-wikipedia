package org.wikipedia.feed.readaloud

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.wikipedia.R
import org.wikipedia.compose.components.AppTextButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.instrument
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import java.time.LocalDate

private val SURVEY_CHOICES = listOf(
    R.string.read_aloud_lead_section_survey_choice1,
    R.string.read_aloud_lead_section_survey_choice2,
    R.string.read_aloud_lead_section_survey_choice3,
    R.string.read_aloud_lead_section_survey_choice4,
    R.string.read_aloud_lead_section_survey_choice5
)
private val OTHER_CHOICE_INDEX = SURVEY_CHOICES.lastIndex

object ReadAloudSurveyDialog {
    fun shouldShow(byDate: Boolean): Boolean {
        if (Prefs.readAloudLeadSectionSurveyShown) {
            return false
        }
        val lastPlayedDate = if (Prefs.readAloudLeadSectionLastPlayedDate.isEmpty()) null else runCatching { LocalDate.parse(Prefs.readAloudLeadSectionLastPlayedDate) }.getOrNull()
        if (byDate) {
            if (lastPlayedDate == null || lastPlayedDate.isAfter(LocalDate.now().minusDays(7))) {
                return false
            }
        }
        return true
    }
}

@Composable
fun ReadAloudSurveyDialog(
    onDismissRequest: () -> Unit
) {
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        activity?.instrument?.submitInteraction("impression", actionSource = "read_aloud_lead_section_survey")
        Prefs.readAloudLeadSectionSurveyShown = true
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ReadAloudSurveyContent(
                onCancelClick = {
                    activity?.instrument?.submitInteraction("click", actionSource = "read_aloud_lead_section_survey", elementId = "cancel")
                    onDismissRequest()
                },
                onSubmitClick = { choiceIndex, otherText ->
                    val actionContext = mutableMapOf<String, Any>()
                    if (choiceIndex != null) {
                        actionContext["choice"] = choiceIndex
                    }
                    if (otherText.isNotEmpty()) {
                        actionContext["text"] = otherText
                    }
                    activity?.let {
                        it.instrument?.submitInteraction("click", actionSource = "read_aloud_lead_section_survey", elementId = "submit", actionContext = actionContext)
                        FeedbackUtil.showMessage(it, R.string.survey_dialog_submitted_snackbar)
                    }
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
private fun ReadAloudSurveyContent(
    onCancelClick: () -> Unit = {},
    onSubmitClick: (choiceIndex: Int?, otherText: String) -> Unit = { _, _ -> }
) {
    var selectedChoiceIndex by remember { mutableStateOf<Int?>(null) }
    val otherTextState = rememberTextFieldState()

    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
        shape = AlertDialogDefaults.shape,
        color = WikipediaTheme.colors.paperColor
    ) {
        Column(
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp),
                text = stringResource(R.string.read_aloud_lead_section_survey_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = WikipediaTheme.colors.primaryColor
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .selectableGroup()
                ) {
                    SURVEY_CHOICES.forEachIndexed { index, choice ->
                        SurveyChoice(
                            text = stringResource(choice),
                            selected = selectedChoiceIndex == index,
                            onClick = { selectedChoiceIndex = index }
                        )
                    }
                }
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                selectedChoiceIndex = OTHER_CHOICE_INDEX
                            }
                        },
                    state = otherTextState,
                    inputTransformation = InputTransformation.maxLength(250),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = WikipediaTheme.colors.primaryColor,
                        unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                        focusedContainerColor = WikipediaTheme.colors.backgroundColor,
                        unfocusedContainerColor = WikipediaTheme.colors.backgroundColor,
                        cursorColor = WikipediaTheme.colors.progressiveColor,
                        selectionColors = TextSelectionColors(
                            handleColor = WikipediaTheme.colors.progressiveColor,
                            backgroundColor = WikipediaTheme.colors.progressiveColor.copy(alpha = 0.4f)
                        ),
                        focusedIndicatorColor = WikipediaTheme.colors.progressiveColor,
                        unfocusedIndicatorColor = WikipediaTheme.colors.inactiveColor
                    )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AppTextButton(onClick = onCancelClick) {
                    Text(
                        text = stringResource(R.string.survey_dialog_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                    )
                }
                AppTextButton(onClick = { onSubmitClick(selectedChoiceIndex, otherTextState.text.toString()) }) {
                    Text(
                        text = stringResource(R.string.survey_dialog_submit),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SurveyChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = WikipediaTheme.colors.progressiveColor,
                unselectedColor = WikipediaTheme.colors.progressiveColor
            )
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )
    }
}

@Preview
@Composable
private fun ReadAloudSurveyContentPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadAloudSurveyContent()
    }
}
