package org.wikipedia.language.composelanglinks

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
                    isLoading = uiState.isLoading,
                    isSiteInfoLoaded = uiState.isSiteInfoLoaded,
                    langLinksItem = uiState.langLinksItems,
                    error = uiState.error,
                    onLanguageSelected = { item ->
                        val pageTitle = item.pageTitle ?: return@ComposeLangLinksScreen
                        WikipediaApp.instance.languageState
                            .addMruLanguageCode(item.languageCode)
                        val intent = PageActivity.newIntentForCurrentTab(this, HistoryEntry(pageTitle, HistoryEntry.SOURCE_LANGUAGE_LINK), pageTitle, false)
                        setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent)
                        DeviceUtil.hideSoftKeyboard(this)
                        finish()
                    },
                    onBackButtonClick = {
                        finish()
                    },
                    onFetchLanguageVariant = { langCode, prefixedText, pageTitle ->
                        if (viewModel.canFetchLanguageLinksVariant(pageTitle)) {
                            viewModel.fetchLangVariantLinks(langCode, prefixedText)
                        }
                    },
                    onSearchQueryChange = {
                        viewModel.onSearchQueryChange(it)
                    },
                    wikiErrorClickEvents = WikiErrorClickEvents(
                        backClickListener = {
                            onBackPressed()
                        },
                        retryClickListener = {
                            viewModel.fetchLangLinks()
                        }
                    )
                )
            }
        }
    }

    companion object {
        const val ACTIVITY_RESULT_LANGLINK_SELECT = 1
        fun newIntent(context: Context, title: PageTitle): Intent {
            return Intent(context, LangLinksActivity::class.java)
                .putExtra(Constants.ARG_TITLE, title)
        }
    }
}
