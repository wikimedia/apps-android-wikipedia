package org.wikipedia.feed.readaloud

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.compose.components.AppTextButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
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
    fun maybeShow(context: Context, byDate: Boolean) {
        if (Prefs.readAloudLeadSectionSurveyShown) {
            return
        }
        val lastPlayedDate = if (Prefs.readAloudLeadSectionLastPlayedDate.isEmpty()) null else runCatching { LocalDate.parse(Prefs.readAloudLeadSectionLastPlayedDate) }.getOrNull()
        if (byDate) {
            if (lastPlayedDate == null || lastPlayedDate.isAfter(LocalDate.now().minusDays(7))) {
                return
            }
        }

        val composeView = ComposeView(context)
        val dialog = MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_AdjustResize)
            .setView(composeView)
            .setCancelable(false)
            .show()

        // The dialog hides the keyboard from its window when its custom view contains no text editor
        // at creation time, which is always the case for a ComposeView that has yet to compose.
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

        composeView.setContent {
            BaseTheme {
                ReadAloudSurveyContent(
                    onCancelClick = { dialog.dismiss() },
                    onSubmitClick = { choiceIndex, otherText ->
                        // TODO: send the survey response.
                        (context as? Activity)?.let {
                            FeedbackUtil.showMessage(it, R.string.survey_dialog_submitted_snackbar)
                        }
                        dialog.dismiss()
                    }
                )
            }
        }

        //Prefs.readAloudLeadSectionSurveyShown = true
    }
}

@Composable
private fun ReadAloudSurveyContent(
    onCancelClick: () -> Unit = {},
    onSubmitClick: (choiceIndex: Int?, otherText: String) -> Unit = { _, _ -> }
) {
    var selectedChoiceIndex by remember { mutableStateOf<Int?>(null) }
    var otherText by remember { mutableStateOf("") }
    var isOtherTextFocused by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // While typing, keep the text field and the buttons below it in view as the keyboard shrinks the dialog.
    LaunchedEffect(isOtherTextFocused) {
        if (isOtherTextFocused) {
            snapshotFlow { scrollState.maxValue }.collect { scrollState.animateScrollTo(it) }
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(bottom = 12.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp),
            text = stringResource(R.string.read_aloud_lead_section_survey_title),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            color = WikipediaTheme.colors.primaryColor
        )
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
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
                        isOtherTextFocused = it.isFocused
                        if (it.isFocused) {
                            selectedChoiceIndex = OTHER_CHOICE_INDEX
                        }
                    },
                value = otherText,
                onValueChange = { otherText = it },
                singleLine = true,
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
            AppTextButton(onClick = { onSubmitClick(selectedChoiceIndex, otherText) }) {
                Text(
                    text = stringResource(R.string.survey_dialog_submit),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                )
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
        Column(modifier = Modifier.background(WikipediaTheme.colors.paperColor)) {
            ReadAloudSurveyContent()
        }
    }
}
