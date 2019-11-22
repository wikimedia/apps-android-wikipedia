package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.suggestededits.SuggestedEditsCardsFragment.Companion.newInstance
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class SuggestedEditsCardsActivity : SingleFragmentActivity<SuggestedEditsCardsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(getActionBarTitleRes(intent.getSerializableExtra(EXTRA_SOURCE) as InvokeSource))
        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.suggestions_background_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.suggestions_background_color))
    }

    override fun createFragment(): SuggestedEditsCardsFragment {
        return newInstance(intent.getSerializableExtra(EXTRA_SOURCE) as InvokeSource)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_suggested_edits, menu)
        ResourceUtil.setMenuItemTint(this, menu.findItem(R.id.menu_help), R.attr.colorAccent)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                FeedbackUtil.showAndroidAppEditingFAQ(baseContext)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getActionBarTitleRes(invokeSource: InvokeSource): Int {
        return when(invokeSource) {
            SUGGESTED_EDITS_TRANSLATE_DESC -> {
                R.string.suggested_edits_translate_descriptions
            }
            SUGGESTED_EDITS_ADD_CAPTION -> {
                R.string.suggested_edits_add_image_captions
            }
            SUGGESTED_EDITS_TRANSLATE_CAPTION -> {
                R.string.suggested_edits_translate_image_captions
            }
            else -> R.string.suggested_edits_add_descriptions
        }
    }

    companion object {
        const val EXTRA_SOURCE = "source"
        const val EXTRA_SOURCE_ADDED_CONTRIBUTION = "addedContribution"

        fun newIntent(context: Context, source: InvokeSource): Intent {
            return Intent(context, SuggestedEditsCardsActivity::class.java).putExtra(EXTRA_SOURCE, source)
        }
    }
}
