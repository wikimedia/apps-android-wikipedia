package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants.INTENT_EXTRA_ACTION
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.extensions.serializableExtra
import org.wikipedia.suggestededits.SuggestedEditsCardsFragment.Companion.newInstance

class SuggestionsActivity : SingleFragmentActivity<SuggestedEditsCardsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setImageZoomHelper()
    }

    override fun onBackPressed() {
        if (fragment.topBaseChild()?.onBackPressed() == false) {
            return
        }
        super.onBackPressed()
    }

    override fun createFragment(): SuggestedEditsCardsFragment {
        return newInstance(intent.serializableExtra(INTENT_EXTRA_ACTION)!!)
    }

    companion object {
        const val EXTRA_SOURCE_ADDED_CONTRIBUTION = "addedContribution"

        fun newIntent(context: Context, action: Action): Intent {
            return Intent(context, SuggestionsActivity::class.java)
                    .putExtra(INTENT_EXTRA_ACTION, action)
        }
    }
}
