package org.wikipedia.talk

import android.app.Activity
import android.widget.TextView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil

object TalkPageSurveyHelper {

    fun shouldShowSurvey(): Boolean {
        val languages = WikipediaApp.getInstance().language().appLanguageCodes
        val country = GeoUtil.geoIPCountry.orEmpty()

        return Prefs.showTalkPageSurvey &&
                (languages.contains("hi") ||
                        languages.contains("id") ||
                        ((languages.contains("ar") || languages.contains("fr")) && (country == "MA" || country == "EG" || country == "ML" || country == "CD")) ||
                        (languages.contains("en") && (country == "IN" || country == "NG")))
    }

    fun showSurvey(activity: Activity) {
        val snackbar = FeedbackUtil.makeSnackbar(activity,
                activity.getString(R.string.talk_snackbar_survey_text), FeedbackUtil.LENGTH_LONG)
        val actionView = snackbar.view.findViewById<TextView>(R.id.snackbar_action)
        actionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_open_in_new_accent_24, 0)
        actionView.compoundDrawablePadding = activity.resources.getDimensionPixelOffset(R.dimen.margin)
        snackbar.setAction(activity.getString(R.string.talk_snackbar_survey_action_text)) { openSurveyInBrowser(activity) }
        snackbar.show()
        Prefs.showTalkPageSurvey = false
    }

    private fun openSurveyInBrowser(activity: Activity) {
        val lang = if (arrayOf("ar", "fr", "hi", "id", "ja").contains(WikipediaApp.getInstance().appOrSystemLanguageCode))
            ("/" + WikipediaApp.getInstance().appOrSystemLanguageCode) else ""
        CustomTabsUtil.openInCustomTab(activity, activity.getString(R.string.talk_pages_survey_url) + lang)
    }
}
