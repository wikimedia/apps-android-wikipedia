package org.wikipedia.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.edit.EditorChoiceContent
import org.wikipedia.edit.EditorChoiceDialogConfig

class VisualEditorSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                VisualEditorSettingsScreen(
                    onBackArrowClicked = { finish() }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, VisualEditorSettingsActivity::class.java)
        }
    }
}

@Composable
fun VisualEditorSettingsScreen(
    onBackArrowClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        EditorChoiceContent(
            initialChoice = Prefs.editorModeChoice,
            dialogConfigData = EditorChoiceDialogConfig(
                dialogTitle = R.string.editor_select_title_settings_screen,
                confirmButtonText = R.string.editor_select_save_btn_settings_screen,
                isInSettingsScreen = true
            ),
            onBackArrowClicked = { onBackArrowClicked() },
            onCancel = {},
            onConfirm = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VisualEditorSettingsScreenPreview() {
    BaseTheme {
        VisualEditorSettingsScreen(
            onBackArrowClicked = {}
        )
    }
}

