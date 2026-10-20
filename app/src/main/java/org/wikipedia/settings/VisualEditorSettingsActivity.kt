package org.wikipedia.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.edit.EDITOR_CHOICE_SOURCE
import org.wikipedia.edit.EditorChoiceContent
import org.wikipedia.theme.Theme

class VisualEditorSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                VisualEditorSettingsScreen(
                    initialChoice = Prefs.editorModeChoice,
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
    initialChoice: Int,
    onBackArrowClicked: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.editor_select_title_settings_screen),
                onNavigationClick = onBackArrowClicked
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { innerPadding ->
        Box(
            Modifier.padding(innerPadding)
        ) {
            EditorChoiceContent(
                initialChoice = initialChoice,
                isInSettingsScreen = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VisualEditorSettingsScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        VisualEditorSettingsScreen(
            initialChoice = EDITOR_CHOICE_SOURCE,
            onBackArrowClicked = {}
        )
    }
}
