package org.wikipedia.robots

import org.wikipedia.base.BaseRobot

class DialogRobot : BaseRobot() {

    fun dismissContributionDialog() = apply {
        clickIfDialogShown("No thanks", errorString = "No Contribution dialog shown.")
    }

    fun dismissBigEnglishDialog() = apply {
        clickIfDialogShown("Maybe later", errorString = "No Big English dialog shown.")
    }

    fun clickLogOutUser() = apply {
        clickIfDialogShown("Log out", errorString = "Cannot click Log out.")
    }
}
