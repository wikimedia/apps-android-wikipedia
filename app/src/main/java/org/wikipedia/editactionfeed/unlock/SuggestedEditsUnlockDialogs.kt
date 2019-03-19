package org.wikipedia.editactionfeed.unlock

import android.content.Context
import android.support.v7.app.AlertDialog
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.editactionfeed.AddTitleDescriptionsActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.views.DialogTitleWithImage


object SuggestedEditsUnlockDialogs {

    fun showUnlockAddDescriptionDialog(context: Context) {
        // TODO: migrate this logic to NotificationReceiver, and account for reverts.
        if (Prefs.isActionEditDescriptionsUnlocked() || Prefs.getTotalUserDescriptionsEdited() < Constants.ACTION_DESCRIPTION_EDIT_UNLOCK_THRESHOLD
                || !ReleaseUtil.isPreBetaRelease()) {
            return
        }
        Prefs.setActionEditDescriptionsUnlocked(true)
        Prefs.setShowActionFeedIndicator(true)
        Prefs.setShowEditMenuOptionIndicator(true)
        AlertDialog.Builder(context)
                .setCustomTitle(DialogTitleWithImage(context, R.string.suggested_edits_unlock_add_descriptions_dialog_title, R.drawable.ic_illustration_description_edit_trophy, true))
                .setMessage(R.string.suggested_edits_unlock_add_descriptions_dialog_message)
                .setPositiveButton(R.string.suggested_edits_unlock_dialog_yes) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, Constants.InvokeSource.EDIT_FEED_TITLE_DESC)) }
                .setNegativeButton(R.string.suggested_edits_unlock_dialog_no, null)
                .show()
    }

    fun showUnlockTranslateDescriptionDialog(context: Context) {
        // TODO: migrate this logic to NotificationReceiver, and account for reverts.
        if (WikipediaApp.getInstance().language().appLanguageCodes.size < Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION || Prefs.getTotalUserDescriptionsEdited() <= Constants.ACTION_DESCRIPTION_EDIT_UNLOCK_THRESHOLD || !Prefs.showEditActionTranslateDescriptionsUnlockedDialog()) {
            return
        }
        Prefs.setActionEditDescriptionsUnlocked(true)
        Prefs.setShowActionFeedIndicator(true)
        Prefs.setShowEditMenuOptionIndicator(true)
        AlertDialog.Builder(context)
                .setCustomTitle(DialogTitleWithImage(context, R.string.suggested_edits_unlock_translate_descriptions_dialog_title, R.drawable.ic_illustration_description_edit_trophy, true))
                .setMessage(R.string.suggested_edits_unlock_translate_descriptions_dialog_message)
                .setPositiveButton(R.string.suggested_edits_unlock_dialog_yes) { _, _ -> context.startActivity(AddTitleDescriptionsActivity.newIntent(context, Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC)) }
                .setNegativeButton(R.string.suggested_edits_unlock_dialog_no, null)
                .show()
    }
}
