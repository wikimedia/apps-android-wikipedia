package org.wikipedia.feed.personalization

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
import org.wikipedia.feed.onboarding.ExploreFeedBuildingActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchActivity

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
        val showIntroPage = intent?.getBooleanExtra(EXTRA_SHOW_INTRO_PAGE, true) ?: true
        val pages = if (showIntroPage) {
            listOf(
                PersonalizationPage.CURIOSITY,
                PersonalizationPage.INTERESTS,
                PersonalizationPage.HOME_PREFERENCE
            )
        } else {
            listOf(
                PersonalizationPage.INTERESTS,
                PersonalizationPage.HOME_PREFERENCE
            )
        }

        setContent {
            BaseTheme {
                PersonalizationScreen(
                    viewModel = viewModel,
                    screens = pages,
                    onSkipClick = { finish() },
                    onSearchClick = {
                        val intent = SearchActivity.newIntent(this, Constants.InvokeSource.FEED_INTEREST_SELECTION, null, returnLink = true)
                        searchLauncher.launch(intent)
                    },
                    onCompleteOnboardingClick = {
                        startActivity(ExploreFeedBuildingActivity.newIntent(this))
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SHOW_INTRO_PAGE = "show_intro_page"

        fun newIntent(context: Context, showIntroPage: Boolean = true): Intent {
            return Intent(context, PersonalizationActivity::class.java)
                .putExtra(EXTRA_SHOW_INTRO_PAGE, showIntroPage)
        }
    }
}

enum class PersonalizationPage {
    CURIOSITY,
    INTERESTS,
    HOME_PREFERENCE
}
