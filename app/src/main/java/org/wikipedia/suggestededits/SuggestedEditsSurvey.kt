package org.wikipedia.suggestededits

import android.app.Activity
import android.net.Uri
import android.widget.TextView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil

object SuggestedEditsSurvey {

    private const val VALID_SUGGESTED_EDITS_COUNT_FOR_SURVEY = 3

    fun maybeRunSurvey(activity: Activity) {
        if (Prefs.shouldShowSuggestedEditsSurvey()) {
            val snackbar = FeedbackUtil.makeSnackbar(activity,
                activity.getString(R.string.suggested_edits_snackbar_survey_text), FeedbackUtil.LENGTH_MEDIUM)
            val actionView = snackbar.view.findViewById<TextView>(R.id.snackbar_action)
            actionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_open_in_new_accent_24, 0)
            actionView.compoundDrawablePadding = activity.resources.getDimensionPixelOffset(R.dimen.margin)
            snackbar.setAction(activity.getString(R.string.suggested_edits_snackbar_survey_action_text)) { openSurveyInBrowser() }
            snackbar.show()
            Prefs.setShouldShowSuggestedEditsSurvey(false)
        }
    }

    fun onEditSuccess() {
        Prefs.setSuggestedEditsCountForSurvey(Prefs.getSuggestedEditsCountForSurvey() + 1)
        if (Prefs.getSuggestedEditsCountForSurvey() == 1 ||
            Prefs.getSuggestedEditsCountForSurvey() == VALID_SUGGESTED_EDITS_COUNT_FOR_SURVEY &&
            !Prefs.wasSuggestedEditsSurveyClicked()) {
            Prefs.setShouldShowSuggestedEditsSurvey(true)
        }
    }

    private fun openSurveyInBrowser() {
        Prefs.setSuggestedEditsSurveyClicked(true)
        UriUtil.visitInExternalBrowser(
            WikipediaApp.getInstance(),
            Uri.parse(WikipediaApp.getInstance().getString(R.string.suggested_edits_survey_url))
        )
    }
}
