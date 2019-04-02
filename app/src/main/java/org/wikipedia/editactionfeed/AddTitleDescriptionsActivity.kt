package org.wikipedia.editactionfeed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.editactionfeed.AddTitleDescriptionsFragment.Companion.newInstance
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.views.DialogTitleWithImage

class AddTitleDescriptionsActivity : SingleFragmentActivity<AddTitleDescriptionsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.elevation = if (intent.getSerializableExtra(EXTRA_SOURCE) == EDIT_FEED_TITLE_DESC) 8f else 0f
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(if (intent.getSerializableExtra(EXTRA_SOURCE) == EDIT_FEED_TITLE_DESC)
            R.string.editactionfeed_add_title_descriptions else R.string.translation_task_title)
    }

    override fun createFragment(): AddTitleDescriptionsFragment {
        return newInstance(intent.getSerializableExtra(EXTRA_SOURCE) as InvokeSource)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_suggested_edits, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                FeedbackUtil.showAndroidAppEditingFAQ(baseContext)
                true
            }
            R.id.menu_my_contributions -> {
                startActivity(MyContributionsActivity.newIntent(this))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_SOURCE = "source"
        const val EXTRA_SOURCE_ADDED_DESCRIPTION = "addedDescription"

        fun newIntent(context: Context, source: InvokeSource): Intent {
            return Intent(context, AddTitleDescriptionsActivity::class.java).putExtra(EXTRA_SOURCE, source)
        }

        fun showEditUnlockDialog(context: Context) {
            AlertDialog.Builder(context)
                    .setCustomTitle(DialogTitleWithImage(context, R.string.suggested_edits_unlock_add_descriptions_dialog_title, R.drawable.ic_illustration_description_edit_trophy, true))
                    .setMessage(R.string.suggested_edits_unlock_add_descriptions_dialog_message)
                    .setPositiveButton(R.string.suggested_edits_unlock_dialog_yes) { _, _ ->
                        SuggestedEditsFunnel.get(ONBOARDING_DIALOG)
                        context.startActivity(EditTasksActivity.newIntent(context, EDIT_FEED_TITLE_DESC))
                    }
                    .setNegativeButton(R.string.suggested_edits_unlock_dialog_no, null)
                    .show()
        }

        fun showTranslateUnlockDialog(context: Context) {
            AlertDialog.Builder(context)
                    .setCustomTitle(DialogTitleWithImage(context, R.string.suggested_edits_unlock_translate_descriptions_dialog_title, R.drawable.ic_illustration_description_edit_trophy, true))
                    .setMessage(R.string.suggested_edits_unlock_translate_descriptions_dialog_message)
                    .setPositiveButton(R.string.suggested_edits_unlock_dialog_yes) { _, _ ->
                        SuggestedEditsFunnel.get(ONBOARDING_DIALOG)
                        context.startActivity(EditTasksActivity.newIntent(context, EDIT_FEED_TRANSLATE_TITLE_DESC))
                    }
                    .setNegativeButton(R.string.suggested_edits_unlock_dialog_no, null)
                    .show()
        }
    }
}
