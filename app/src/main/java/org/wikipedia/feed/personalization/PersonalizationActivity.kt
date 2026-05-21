package org.wikipedia.feed.personalization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import org.wikipedia.Constants
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
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

        _instrument = TestKitchenAdapter.client.getInstrument("apps-home-feed")
            .setDefaultActionSource("feed_customize")
            .startFunnel("feed_customize")

        val showInterestsOnly = intent?.getBooleanExtra(EXTRA_SHOW_INTERESTS_ONLY, false) ?: false

        val pages = if (showInterestsOnly) {
            listOf(
                PersonalizationPage.INTERESTS
            )
        } else {
            listOf(
                PersonalizationPage.CURIOSITY,
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
                        if (showInterestsOnly) {
                            onFinish()
                            return@PersonalizationScreen
                        }

                        startActivity(ExploreFeedBuildingActivity.newIntent(this))
                        setResult(RESULT_OK)
                        finish()
                    },
                    onBackButtonClick = if (showInterestsOnly) ({
                        onFinish()
                    }) else null
                )
            }
        }
    }

    private fun onFinish() {
        setResult(if (viewModel.interestsUpdated) RESULT_INTERESTS_UPDATED else RESULT_OK)
        finish()
    }

    companion object {
        private const val EXTRA_SHOW_INTERESTS_ONLY = "show_interests_only"
        const val RESULT_INTERESTS_UPDATED = 100

        fun newIntent(context: Context, showInterestsOnly: Boolean = false): Intent {
            return Intent(context, PersonalizationActivity::class.java)
                .putExtra(EXTRA_SHOW_INTERESTS_ONLY, showInterestsOnly)
        }
    }
}

enum class PersonalizationPage {
    CURIOSITY,
    INTERESTS,
    HOME_PREFERENCE
}
