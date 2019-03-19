package org.wikipedia.editactionfeed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.view.Menu
import android.view.MenuItem
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.Constants.InvokeSource.EDIT_FEED_TITLE_DESC
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.editactionfeed.AddTitleDescriptionsFragment.Companion.newInstance
import org.wikipedia.editactionfeed.unlock.SuggestedEditsUnlockDialogs
import org.wikipedia.util.FeedbackUtil

class AddTitleDescriptionsActivity : SingleFragmentActivity<AddTitleDescriptionsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.elevation = 0f
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(if (intent.getSerializableExtra(EXTRA_SOURCE) == EDIT_FEED_TITLE_DESC)
            R.string.editactionfeed_add_title_descriptions else R.string.editactionfeed_translate_descriptions)
    }

    override fun createFragment(): AddTitleDescriptionsFragment {
        return newInstance(intent.getSerializableExtra(EXTRA_SOURCE) as InvokeSource)
    }


    override fun onResume() {
        super.onResume()
        SuggestedEditsUnlockDialogs.showUnlockTranslateDescriptionDialog(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_action_feed, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }

            R.id.menu_help -> {
                FeedbackUtil.showAndroidAppEditingFAQ(baseContext)
                true
            }
            R.id.menu_my_contributions -> {
                // TODO: go to My contributions
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_SOURCE = "source"
        const val EXTRA_SOURCE_ADDED_DESCRIPTION = "addedDescription"

        fun newIntent(context: Context, source: InvokeSource): Intent {
            return Intent(context, AddTitleDescriptionsActivity::class.java)
                    .putExtra(EXTRA_SOURCE, source)
        }
    }
}
