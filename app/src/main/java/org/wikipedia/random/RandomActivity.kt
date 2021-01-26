package org.wikipedia.random

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite

class RandomActivity : SingleFragmentActivity<RandomFragment>() {

    companion object {
        const val INTENT_EXTRA_WIKISITE = "wikiSite"

        @JvmStatic
        fun newIntent(context: Context, wikiSite: WikiSite, invokeSource: InvokeSource?): Intent {
            return Intent(context, RandomActivity::class.java).apply {
                putExtra(INTENT_EXTRA_WIKISITE, wikiSite)
                putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.elevation = 0f
    }

    public override fun createFragment(): RandomFragment {
        val wikiSite = intent.getParcelableExtra<WikiSite>(INTENT_EXTRA_WIKISITE)
        val invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as? InvokeSource

        if (wikiSite != null && invokeSource != null) {
            return RandomFragment.newInstance(wikiSite, invokeSource)
        } else {
            throw IllegalStateException("wikiSite or invokeSource is null.")
        }
    }
}