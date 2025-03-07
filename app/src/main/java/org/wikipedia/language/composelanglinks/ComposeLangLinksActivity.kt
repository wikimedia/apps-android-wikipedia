package org.wikipedia.language.composelanglinks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.LangLinksActivity.Companion.ACTIVITY_RESULT_LANGLINK_SELECT
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil

class ComposeLangLinksActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                ComposeLangLinksParentScreen(
                    onLanguageSelected = { item ->
                        val pageTitle = item.pageTitle ?: return@ComposeLangLinksParentScreen
                        WikipediaApp.instance.languageState
                            .addMruLanguageCode(item.languageCode)
                        val intent = PageActivity.newIntentForCurrentTab(this, HistoryEntry(pageTitle, HistoryEntry.SOURCE_LANGUAGE_LINK), pageTitle, false)
                        setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent)
                        DeviceUtil.hideSoftKeyboard(this)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context, title: PageTitle): Intent {
            return Intent(context, ComposeLangLinksActivity::class.java)
                .putExtra(Constants.ARG_TITLE, title)
        }
    }
}
