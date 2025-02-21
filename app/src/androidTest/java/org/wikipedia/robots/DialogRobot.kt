package org.wikipedia.robots

import org.wikipedia.base.base.BaseRobot

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
}
