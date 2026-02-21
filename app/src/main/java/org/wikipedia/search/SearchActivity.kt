package org.wikipedia.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.extensions.instrument
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L

class SearchActivity : SingleFragmentActivity<SearchFragment>() {

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            instrument?.submitInteraction("click", actionSource = "search", elementId = "search_back_button")

            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(EXTRA_SHOW_SNACKBAR_MESSAGE)) {
            val messageResId = intent.getIntExtra(EXTRA_SHOW_SNACKBAR_MESSAGE, 0)
            if (messageResId != 0) {
                FeedbackUtil.showMessage(this, messageResId)
                intent.removeExtra(EXTRA_SHOW_SNACKBAR_MESSAGE)
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        _instrument = TestKitchenAdapter.client.getInstrument("apps-search")
            .startFunnel("search")
            .setExperiment(TestKitchenAdapter.getExperiment(HybridSearchAbCTest()))
    }

    public override fun createFragment(): SearchFragment {
        var source = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource?
        if (source == null) {
            when {
                Intent.ACTION_SEND == intent.action -> { source = InvokeSource.INTENT_SHARE }
                Intent.ACTION_PROCESS_TEXT == intent.action -> { source = InvokeSource.INTENT_PROCESS_TEXT }
                else -> {
                    source = InvokeSource.INTENT_UNKNOWN
                    L.logRemoteErrorIfProd(RuntimeException("Unknown intent when launching SearchActivity: " + intent.action.orEmpty()))
                }
            }
        }
        return SearchFragment.newInstance(
            source = source,
            query = intent.getStringExtra(QUERY_EXTRA),
            returnLink = intent.getBooleanExtra(EXTRA_RETURN_LINK, false),
            title = intent.getStringExtra(EXTRA_TITLE),
            initiateHybridSearch = intent.getBooleanExtra(EXTRA_SHOW_HYBRID_SEARCH, false)
        )
    }

    companion object {
        const val QUERY_EXTRA = "query"
        const val EXTRA_TITLE = "articleTitle"
        const val EXTRA_RETURN_LINK = "returnLink"
        const val EXTRA_RETURN_LINK_TITLE = "returnLinkTitle"
        const val RESULT_LINK_SUCCESS = 97
        const val EXTRA_SHOW_SNACKBAR_MESSAGE = "showSnackbarMessage"
        const val EXTRA_SHOW_HYBRID_SEARCH = "showHybridSearch"

        fun newIntent(context: Context, source: InvokeSource, query: String?, returnLink: Boolean = false, title: String? = null, initiateHybridSearch: Boolean = false): Intent {
            if (HybridSearchAbCTest().shouldShowOnboarding(WikipediaApp.instance.languageState.appLanguageCode)) {
                Prefs.isHybridSearchOnboardingShown = true
                return HybridSearchOnboardingActivity.newIntent(context, source)
            }

            return Intent(context, SearchActivity::class.java)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, source)
                    .putExtra(QUERY_EXTRA, query)
                    .putExtra(EXTRA_RETURN_LINK, returnLink)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_SHOW_HYBRID_SEARCH, initiateHybridSearch)
        }
    }
}
