package org.wikipedia.language.addlanguages

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.util.DeviceUtil

class AddLanguagesListActivity : BaseActivity() {
    private var isLanguageSearched: Boolean = false
    private val viewModel: AddLanguagesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                val uiState = viewModel.uiState.collectAsState().value

                LanguagesListScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    languageListUiState = uiState,
                    onBackButtonClick = {
                        finish()
                    },
                    onSearchQueryChange = { query ->
                        viewModel.updateSearchTerm(query)
                    },
                    onListItemClick = { languageCode ->
                        val app = WikipediaApp.instance
                        if (languageCode != app.appOrSystemLanguageCode) {
                            app.languageState.addAppLanguageCode(languageCode)
                        }
                        val returnIntent = Intent()
                        returnIntent.putExtra(WikipediaLanguagesFragment.ADD_LANGUAGE_INTERACTIONS, 1)
                        returnIntent.putExtra(LANGUAGE_SEARCHED, isLanguageSearched)
                        setResult(RESULT_OK, returnIntent)
                        finish()
                    },
                    onLanguageSearched = {
                        isLanguageSearched = it
                    },
                    wikiErrorClickEvents = WikiErrorClickEvents(
                        backClickListener = { onBackPressed() },
                        retryClickListener = { viewModel.fetchAllData() }
                    )
                )
            }
        }
    }

    override fun onBackPressed() {
        DeviceUtil.hideSoftKeyboard(this)
        val returnIntent = Intent()
        returnIntent.putExtra(LANGUAGE_SEARCHED, isLanguageSearched)
        setResult(RESULT_OK, returnIntent)
        super.onBackPressed()
    }

    companion object {
        const val LANGUAGE_SEARCHED = "language_searched"
    }
}
