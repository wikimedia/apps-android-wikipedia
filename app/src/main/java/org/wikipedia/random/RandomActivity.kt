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
        return RandomFragment.newInstance(intent.getParcelableExtra(INTENT_EXTRA_WIKISITE)!!,
                intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource)
    }
}
