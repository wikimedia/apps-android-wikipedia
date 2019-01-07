package org.wikipedia.editactionfeed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.descriptions.DescriptionEditHelpActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.views.DialogTitleWithImage

class AddTitleDescriptionsActivity : SingleFragmentActivity<AddTitleDescriptionsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.elevation = 0f
    }

    override fun createFragment(): AddTitleDescriptionsFragment {
        return AddTitleDescriptionsFragment.newInstance(intent.getIntExtra(EXTRA_SOURCE, SOURCE_ADD_DESCRIPTIONS))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_action_feed, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                startActivity(DescriptionEditHelpActivity.newIntent(this))
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
        const val SOURCE_ADD_DESCRIPTIONS = 0
        const val SOURCE_TRANSLATE_DESCRIPTIONS = 1

        fun newIntent(context: Context, source: Int): Intent {
            return Intent(context, AddTitleDescriptionsActivity::class.java)
                    .putExtra(EXTRA_SOURCE, source)
        }

        fun maybeShowEditUnlockDialog(context: Context) {
            // TODO: migrate this logic to NotificationReceiver, and account for reverts.
            if (Prefs.isActionEditDescriptionsUnlocked() || Prefs.getTotalUserDescriptionsEdited() < Constants.ACTION_DESCRIPTION_EDIT_UNLOCK_THRESHOLD
                    || !ReleaseUtil.isPreBetaRelease()) {
                return
            }
            Prefs.setActionEditDescriptionsUnlocked(true)
            Prefs.setShowActionFeedIndicator(true)
            Prefs.setShowEditMenuOptionIndicator(true)
            AlertDialog.Builder(context)
                    .setCustomTitle(DialogTitleWithImage(context, R.string.description_edit_task_unlock_title, R.drawable.ic_illustration_description_edit_trophy, true))
                    .setMessage(R.string.description_edit_task_unlock_body)
                    .setPositiveButton(R.string.onboarding_get_started) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, SOURCE_ADD_DESCRIPTIONS)) }
                    .setNegativeButton(R.string.onboarding_maybe_later, null)
                    .show()
        }
    }
}
