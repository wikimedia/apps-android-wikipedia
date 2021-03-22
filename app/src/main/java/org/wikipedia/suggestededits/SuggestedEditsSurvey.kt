package org.wikipedia.suggestededits

import android.app.Activity
import android.net.Uri
import android.widget.TextView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.FeedbackUtil.makeSnackbar
import org.wikipedia.util.UriUtil

object SuggestedEditsSurvey {
    private const val VALID_SUGGESTED_EDITS_COUNT_FOR_SURVEY = 3
    fun maybeRunSurvey(activity: Activity) {
        if (Prefs.shouldShowSuggestedEditsSurvey()) {
            makeSnackbar(activity,
                    activity.getString(R.string.suggested_edits_snackbar_survey_text),
                    FeedbackUtil.LENGTH_MEDIUM).apply {
                view.findViewById<TextView>(R.id.snackbar_action).apply {
                    setCompoundDrawablesWithIntrinsicBounds(0, 0,
                            R.drawable.ic_open_in_new_accent_24, 0)
                    compoundDrawablePadding =
                            activity.resources.getDimensionPixelOffset(R.dimen.margin)
                }
                setAction(activity.getString(R.string.suggested_edits_snackbar_survey_action_text)) { openSurveyInBrowser() }
                show()
            }
            Prefs.setShouldShowSuggestedEditsSurvey(false)
        }
    }

    @JvmStatic
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
        UriUtil.visitInExternalBrowser(WikipediaApp.getInstance(),
                Uri.parse(WikipediaApp.getInstance().getString(R.string.suggested_edits_survey_url)))
    }
}
