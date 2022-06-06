package org.wikipedia.feed.onthisday

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite

class OnThisDayActivity : SingleFragmentActivity<OnThisDayFragment>() {

    override fun createFragment(): OnThisDayFragment {
        return OnThisDayFragment.newInstance(intent.getIntExtra(EXTRA_AGE, 0),
            intent.getParcelableExtra(EXTRA_WIKISITE)!!,
            intent.getIntExtra(EXTRA_YEAR, -1),
            intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource)
    }

    companion object {
        const val EXTRA_AGE = "age"
        const val EXTRA_YEAR = "year"
        const val EXTRA_WIKISITE = "wikiSite"

        fun newIntent(context: Context, age: Int, year: Int,
                      wikiSite: WikiSite, invokeSource: InvokeSource): Intent {
            return Intent(context, OnThisDayActivity::class.java)
                .putExtras(bundleOf(EXTRA_AGE to age, EXTRA_WIKISITE to wikiSite,
                    EXTRA_YEAR to year, Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource))
        }
    }
}
