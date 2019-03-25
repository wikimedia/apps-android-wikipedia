package org.wikipedia.editactionfeed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import org.wikipedia.Constants.*
import org.wikipedia.Constants.InvokeSource.EDIT_FEED_TITLE_DESC
import org.wikipedia.Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.editactionfeed.AddTitleDescriptionsFragment.Companion.newInstance
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.views.DialogTitleWithImage

class AddTitleDescriptionsActivity : SingleFragmentActivity<AddTitleDescriptionsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.elevation = 0f
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(if (intent.getSerializableExtra(EXTRA_SOURCE) == EDIT_FEED_TITLE_DESC)
            R.string.editactionfeed_add_title_descriptions else R.string.translation_task_title)
    }

    override fun createFragment(): AddTitleDescriptionsFragment {
        return newInstance(intent.getSerializableExtra(EXTRA_SOURCE) as InvokeSource)
    }


    override fun onResume() {
        super.onResume()
        maybeShowTranslationEdit(this)
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

        fun maybeShowEditUnlockDialog(context: Context) {
            // TODO: migrate this logic to NotificationReceiver, and account for reverts.
            if (Prefs.isActionEditDescriptionsUnlocked() || Prefs.getTotalUserDescriptionsEdited() < ACTION_DESCRIPTION_EDIT_UNLOCK_THRESHOLD
                    || !ReleaseUtil.isPreBetaRelease()) {
                return
            }
            Prefs.setActionEditDescriptionsUnlocked(true)
            Prefs.setShowActionFeedIndicator(true)
            Prefs.setShowEditMenuOptionIndicator(true)
            AlertDialog.Builder(context)
                    .setCustomTitle(DialogTitleWithImage(context, R.string.description_edit_task_unlock_title, R.drawable.ic_illustration_description_edit_trophy, true))
                    .setMessage(R.string.description_edit_task_unlock_body)
                    .setPositiveButton(R.string.onboarding_get_started) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, EDIT_FEED_TITLE_DESC)) }
                    .setNegativeButton(R.string.onboarding_maybe_later, null)
                    .show()
        }

        fun maybeShowTranslationEdit(context: Context) {
            if (WikipediaApp.getInstance().language().appLanguageCodes.size < MIN_LANGUAGES_TO_UNLOCK_TRANSLATION || Prefs.getTotalUserDescriptionsEdited() <= ACTION_DESCRIPTION_EDIT_UNLOCK_THRESHOLD || !Prefs.showEditActionTranslateDescriptionsUnlockedDialog()) {
                return
            }
            Prefs.setShowEditActionTranslateDescriptionsUnlockedDialog(false)
            Prefs.setEditActionTranslateDescriptionsUnlocked(true)
            AlertDialog.Builder(context)
                    .setCustomTitle(DialogTitleWithImage(context, R.string.translation_description_edit_task_unlock_title, R.drawable.ic_illustration_description_edit_trophy, true))
                    .setMessage(R.string.translation_description_edit_task_unlock_body)
                    .setPositiveButton(R.string.onboarding_get_started) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, EDIT_FEED_TRANSLATE_TITLE_DESC)) }
                    .setNegativeButton(R.string.onboarding_maybe_later, null)
                    .show()
        }
    }
}
