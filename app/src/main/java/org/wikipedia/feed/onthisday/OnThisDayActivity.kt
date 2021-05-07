package org.wikipedia.feed.onthisday

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite

class OnThisDayActivity : SingleFragmentActivity<OnThisDayFragment>() {
    override fun createFragment(): OnThisDayFragment {
        return OnThisDayFragment.newInstance(
            intent.getIntExtra(AGE, 0), intent.getParcelableExtra(
                WIKISITE
            )
        )
    }

    companion object {
        const val AGE = "age"
        const val YEAR = "year"
        const val WIKISITE = "wikisite"
        fun newIntent(
            context: Context,
            age: Int,
            year: Int,
            wikiSite: WikiSite,
            invokeSource: InvokeSource
        ): Intent {
            return Intent(context, OnThisDayActivity::class.java)
                .putExtra(AGE, age)
                .putExtra(WIKISITE, wikiSite)
                .putExtra(YEAR, year)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
