package org.wikipedia.suggestededits

import android.app.Activity
import android.net.Uri
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil

object SuggestedEditsSurvey {

    private const val VALID_SUGGESTED_EDITS_COUNT_FOR_SURVEY = 3

    fun maybeRunSurvey(activity: Activity) {
        if (Prefs.showSuggestedEditsSurvey) {
            val snackbar = FeedbackUtil.makeSnackbar(activity,
                activity.getString(R.string.suggested_edits_snackbar_survey_text), FeedbackUtil.LENGTH_MEDIUM)
            val actionView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
            actionView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_open_in_new_black_24px, 0)
            actionView.compoundDrawablePadding = activity.resources.getDimensionPixelOffset(R.dimen.margin)
            TextViewCompat.setCompoundDrawableTintList(actionView, ResourceUtil.getThemedColorStateList(activity, R.attr.progressive_color))
            snackbar.setAction(activity.getString(R.string.suggested_edits_snackbar_survey_action_text)) { openSurveyInBrowser() }
            snackbar.show()
            Prefs.showSuggestedEditsSurvey = false
        }
    }

    fun onEditSuccess() {
        Prefs.suggestedEditsCountForSurvey = Prefs.suggestedEditsCountForSurvey + 1
        if (Prefs.suggestedEditsCountForSurvey == 1 ||
            Prefs.suggestedEditsCountForSurvey == VALID_SUGGESTED_EDITS_COUNT_FOR_SURVEY &&
            !Prefs.suggestedEditsSurveyClicked) {
            Prefs.showSuggestedEditsSurvey = true
        }
    }

    private fun openSurveyInBrowser() {
        Prefs.suggestedEditsSurveyClicked = true
        UriUtil.visitInExternalBrowser(
            WikipediaApp.instance,
            Uri.parse(WikipediaApp.instance.getString(R.string.suggested_edits_survey_url))
        )
    }
}
