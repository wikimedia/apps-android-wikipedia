package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class SuggestedEditsTasksActivity : SingleFragmentActivity<SuggestedEditsTasksFragment>() {
    private var startImmediately: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startImmediately = savedInstanceState?.getBoolean(EXTRA_START_IMMEDIATELY, false)
                ?: intent.getBooleanExtra(EXTRA_START_IMMEDIATELY, false)

        if (intent.hasExtra(INTENT_EXTRA_INVOKE_SOURCE)) {
            val source = intent.getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
            SuggestedEditsFunnel.get(source)
            if (startImmediately && (source == SUGGESTED_EDITS_ADD_DESC || source == SUGGESTED_EDITS_TRANSLATE_DESC
                            || source == SUGGESTED_EDITS_ADD_CAPTION || source == SUGGESTED_EDITS_TRANSLATE_CAPTION)) {
                startImmediately = false
                startActivity(SuggestedEditsCardsActivity.newIntent(this, source))
            }
        } else {
            SuggestedEditsFunnel.get(NAV_MENU)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_START_IMMEDIATELY, startImmediately)
    }

    public override fun onPause() {
        super.onPause()
        SuggestedEditsFunnel.get().pause()
    }

    public override fun onResume() {
        super.onResume()
        SuggestedEditsFunnel.get().resume()
    }

    public override fun onDestroy() {
        super.onDestroy()
        SuggestedEditsFunnel.get().log()
        SuggestedEditsFunnel.reset()
    }

    override fun createFragment(): SuggestedEditsTasksFragment {
        return SuggestedEditsTasksFragment.newInstance()
    }

    companion object {
        private const val EXTRA_START_IMMEDIATELY = "startImmediately"

        @JvmStatic
        fun newIntent(context: Context, invokeSource: InvokeSource): Intent {
            val intent = Intent(context, SuggestedEditsTasksActivity::class.java)
                    .putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            if (invokeSource == SUGGESTED_EDITS_ADD_DESC || invokeSource == SUGGESTED_EDITS_TRANSLATE_DESC
                    || invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION) {
                intent.putExtra(EXTRA_START_IMMEDIATELY, true)
            }
            return intent
        }
    }
}
