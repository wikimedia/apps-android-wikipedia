package org.wikipedia.onboarding.personalization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import org.wikipedia.Constants
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchActivity
import org.wikipedia.util.FeedbackUtil

class PersonalizationActivity : BaseActivity() {

    private val viewModel: PersonalizationViewModel by viewModels { PersonalizationViewModel.Factory }

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SearchActivity.RESULT_LINK_SUCCESS) {
            val pageTitle = it.data?.parcelableExtra<PageTitle>(SearchActivity.EXTRA_RETURN_LINK_TITLE) ?: return@registerForActivityResult
            viewModel.addArticleFromSearch(pageTitle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                PersonalizationScreen(
                    viewModel = viewModel,
                    screens = listOf(
                        PersonalizationPage.CURIOSITY,
                        PersonalizationPage.INTERESTS,
                        PersonalizationPage.FEED_PREFERENCE
                    ),
                    onSkipClick = { finish() },
                    onSearchClick = {
                        val intent = SearchActivity.newIntent(this, Constants.InvokeSource.FEED_INTEREST_SELECTION, null, returnLink = true)
                        searchLauncher.launch(intent)
                    },
                    showError = { message ->
                        FeedbackUtil.showError(this, message)
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, PersonalizationActivity::class.java)
        }
    }
}

enum class PersonalizationPage {
    CURIOSITY,
    INTERESTS,
    FEED_PREFERENCE
}
