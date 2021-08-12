package org.wikipedia.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.util.StringUtil.listToJsonArrayString

class SettingsActivity : SingleFragmentActivity<SettingsFragment>() {
    private lateinit var initialLanguageList: String
    private lateinit var initialFeedCardsEnabled: List<Boolean>
    private lateinit var initialFeedCardsOrder: List<Int>
    private val app = WikipediaApp.getInstance()

    public override fun createFragment(): SettingsFragment {
        return SettingsFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialLanguageList = listToJsonArrayString(app.language().appLanguageCodes)
        initialFeedCardsEnabled = Prefs.getFeedCardsEnabled()
        initialFeedCardsOrder = Prefs.getFeedCardsOrder()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val finalLanguageList = listToJsonArrayString(app.language().appLanguageCodes)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE &&
                finalLanguageList != initialLanguageList) {
            setResult(ACTIVITY_RESULT_LANGUAGE_CHANGED)
        } else if (requestCode == Constants.ACTIVITY_REQUEST_FEED_CONFIGURE &&
                (Prefs.getFeedCardsEnabled() != initialFeedCardsEnabled || Prefs.getFeedCardsOrder() != initialFeedCardsOrder)) {
            setResult(ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED)
        }
    }

    companion object {
        const val ACTIVITY_RESULT_LANGUAGE_CHANGED = 1
        const val ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED = 2
        const val ACTIVITY_RESULT_LOG_OUT = 3
        @JvmStatic
        fun newIntent(ctx: Context): Intent {
            return Intent(ctx, SettingsActivity::class.java)
        }
    }
}
