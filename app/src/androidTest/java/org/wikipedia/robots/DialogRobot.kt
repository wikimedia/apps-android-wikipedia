package org.wikipedia.robots

import BaseRobot
import android.content.Context
import org.wikipedia.R

class DialogRobot : BaseRobot() {

    fun dismissContributionDialog() = apply {
        click.ifDialogShown("No thanks", errorString = "No Contribution dialog shown.")
    }

    fun dismissBigEnglishDialog() = apply {
        click.ifDialogShown("Maybe later", errorString = "No Big English dialog shown.")
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
