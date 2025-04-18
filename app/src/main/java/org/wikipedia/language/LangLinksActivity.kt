package org.wikipedia.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil

class LangLinksActivity : BaseActivity() {
    private val viewModel: LangLinksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                val uiState by viewModel.uiState.collectAsState()
                ComposeLangLinksScreen(
                    uiState = uiState,
                    onLanguageSelected = { item ->
                        val pageTitle = item.pageTitle ?: return@ComposeLangLinksScreen
                        WikipediaApp.instance.languageState
                            .addMruLanguageCode(item.languageCode)
                        val historyEntry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_LANGUAGE_LINK).apply {
                            prevId = viewModel.historyEntryId
                        }
                        val intent = PageActivity.newIntentForCurrentTab(this, historyEntry, pageTitle, false)
                        setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent)
                        DeviceUtil.hideSoftKeyboard(this)
                        finish()
                    },
                    onBackButtonClick = {
                        finish()
                    },
                    onSearchQueryChange = {
                        viewModel.onSearchQueryChange(it)
                    },
                    wikiErrorClickEvents = WikiErrorClickEvents(
                        backClickListener = {
                            onBackPressed()
                        },
                        retryClickListener = {
                            viewModel.fetchAllData()
                        }
                    )
                )
            }
        }
    }

    companion object {
        const val ACTIVITY_RESULT_LANGLINK_SELECT = 1
        fun newIntent(context: Context, title: PageTitle, historyEntryId: Long = -1): Intent {
            return Intent(context, LangLinksActivity::class.java)
                .putExtra(Constants.ARG_TITLE, title)
                .putExtra(Constants.ARG_NUMBER, historyEntryId)
        }
    }
}
