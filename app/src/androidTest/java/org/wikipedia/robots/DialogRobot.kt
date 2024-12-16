package org.wikipedia.robots

import android.util.Log
import org.wikipedia.base.BaseRobot

class DialogRobot : BaseRobot() {

    fun dismissContributionDialog() = apply {
        try {
            clickOnViewWithText(text = "No, thanks")
        } catch (e: Exception) {
            Log.d("DialogRobot: ", "No Contribution dialog shown.")
        }
    }

    fun dismissBigEnglishDialog() = apply {
        try {
            clickOnViewWithText(text = "Maybe later")
        } catch (e: Exception) {
            Log.d("DialogRobot: ", "No Big English dialog shown.")
        }
    }

    fun dismissShareReadingListDialog() = apply {
        try {
            clickOnViewWithText(text = "Got it")
        } catch (e: Exception) {
            Log.d("DialogRobot: ", "No share reading list dialog shown.")
        }
    }
}
