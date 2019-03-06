package org.wikipedia.editactionfeed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NavUtils
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TITLE_DESC_SOURCE
import org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE
import org.wikipedia.descriptions.DescriptionEditHelpActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.views.DialogTitleWithImage

class AddTitleDescriptionsActivity : SingleFragmentActivity<AddTitleDescriptionsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.elevation = 0f
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(if (intent.getIntExtra(EXTRA_SOURCE, EDIT_TASKS_TITLE_DESC_SOURCE) == EDIT_TASKS_TITLE_DESC_SOURCE)
            R.string.editactionfeed_add_title_descriptions else R.string.editactionfeed_translate_descriptions)
    }

    override fun createFragment(): AddTitleDescriptionsFragment {
        return AddTitleDescriptionsFragment.newInstance(intent.getIntExtra(EXTRA_SOURCE, EDIT_TASKS_TITLE_DESC_SOURCE))
    }


    override fun onResume() {
        super.onResume()
        maybeShowTranslationEdit()
    }

    private fun maybeShowTranslationEdit() {
        if (WikipediaApp.getInstance().language().appLanguageCodes.size > 1 && Prefs.getTotalUserDescriptionsEdited()>=2&& Prefs.showEditActionTranslateDescriptionsUnlockedDialog()) {
            Prefs.setShowEditActionTranslateDescriptionsUnlockedDialog(false)
            Prefs.setEditActionTranslateDescriptionsUnlocked(true);
            AlertDialog.Builder(this)
                    .setCustomTitle(DialogTitleWithImage(this, R.string.translation_description_edit_task_unlock_title, R.drawable.ic_illustration_description_edit_trophy, true))
                    .setMessage(R.string.translation_description_edit_task_unlock_body)
                    .setPositiveButton(R.string.translate_description_get_started) { _, _ ->startActivity(AddTitleDescriptionsActivity.newIntent(this, EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE)) }
                    .setNegativeButton(R.string.onboarding_maybe_later, null)
                    .show()
        }
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
                    .setPositiveButton(R.string.title_description_get_started) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, EDIT_TASKS_TITLE_DESC_SOURCE)) }
                    .setNegativeButton(R.string.onboarding_maybe_later, null)
                    .show()
        }
    }
}
