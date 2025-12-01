package org.wikipedia.robots

import BaseRobot
import android.content.Context
import android.util.Log
import androidx.test.espresso.NoMatchingViewException
import org.wikipedia.R

class DialogRobot : BaseRobot() {

    fun dismissSurveyDialog() = apply {
        click.ifDialogShown("No thanks", errorString = "No Survey Dialog dialog shown.")
    }

    fun dismissContributionDialog() = apply {
        click.ifDialogShown("No thanks", errorString = "No Contribution dialog shown.")
    }

    fun dismissBigEnglishDialog() = apply {
        try {
            click.onDisplayedViewWithIdAnContentDescription(R.id.closeButton, "Close")
        } catch (e: NoMatchingViewException) {
            Log.e("DialogRobot", "No Big English Dialog shown.")
        } catch (e: Exception) {
            Log.e("DialogRobot", "Unexpected Error: ${e.message}")
        }
    }

    fun clickLogOutUser() = apply {
        click.ifDialogShown("Log out", errorString = "Cannot click Log out.")
    }

    fun dismissPromptLogInToSyncDialog(context: Context) = apply {
        click.ifDialogShown(
            context.getString(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks),
            errorString = "Cannot click")
    }

    fun click(string: String) = apply {
        click.ifDialogShown(string, "Cannot click")
    }
}
