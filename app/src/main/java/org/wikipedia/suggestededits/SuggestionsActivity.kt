package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.Constants.INTENT_EXTRA_ACTION
import org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
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
        return newInstance(intent.getSerializableExtra(INTENT_EXTRA_ACTION) as Action,
                intent.getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource)
    }

    companion object {
        const val EXTRA_SOURCE_ADDED_CONTRIBUTION = "addedContribution"

        fun newIntent(context: Context, action: Action, source: Constants.InvokeSource): Intent {
            return Intent(context, SuggestionsActivity::class.java)
                    .putExtra(INTENT_EXTRA_ACTION, action)
                    .putExtra(INTENT_EXTRA_INVOKE_SOURCE, source)
        }
    }
}
