package org.wikipedia.random

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.extensions.serializableExtra

class RandomActivity : SingleFragmentActivity<RandomFragment>() {

    companion object {
        fun newIntent(context: Context, wikiSite: WikiSite, invokeSource: InvokeSource?): Intent {
            return Intent(context, RandomActivity::class.java).apply {
                putExtra(Constants.ARG_WIKISITE, wikiSite)
                putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.elevation = 0f
    }

    public override fun createFragment(): RandomFragment {
        return RandomFragment.newInstance(intent.parcelableExtra(Constants.ARG_WIKISITE)!!,
                intent.serializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!)
    }
}
