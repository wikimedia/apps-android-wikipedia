package org.wikipedia.talk

import android.app.Activity
import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

object TalkPageSurvey {

    fun maybeShowSurvey(activity: Activity, editSubmitted: Boolean) {
        if (fallsWithinGeoRange() || Prefs.talkPageSurveyOverride) {
            queueSurveyDialog(activity, editSubmitted)
        }
    }

    private fun queueSurveyDialog(activity: Activity, editSubmitted: Boolean) {
        val attempts = Prefs.showTalkPageSurveyAttempts
        if (attempts > 1) {
            return
        }
        Prefs.showTalkPageSurveyAttempts = attempts + 1

        // If they just submitted an edit, then show the survey immediately.
        if (editSubmitted) {
            showSurveyDialog(activity, attempts)
            return
        }

        // Otherwise, fetch the user's edit count, and only show the survey if the count is nonzero.
        ServiceFactory.get(WikipediaApp.instance.wikiSite).userInfo
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if ((it.query?.userInfo?.editCount ?: 0) > 0) {
                        showSurveyDialog(activity, attempts)
                    }
                }, { L.e(it) })
    }

    fun showSurveyDialog(activity: Activity, attempts: Int) {
        if (activity.isDestroyed) {
            return
        }
        val dialog = AlertDialog.Builder(activity)
                .setMessage(StringUtil.fromHtml(activity.getString(R.string.talk_snackbar_survey_text) +
                        "<br/><br/><small><a href=\"https://foundation.m.wikimedia.org/wiki/Legal:Wikipedia_Android_App_Talk_Page_Survey_Privacy_Statement\">" +
                        activity.getString(R.string.privacy_policy_description) + "</a></small>"))
                .setPositiveButton(R.string.talk_snackbar_survey_action_text) { _, _ -> takeUserToSurvey(activity) }
                .setNegativeButton(if (attempts == 0) R.string.onboarding_maybe_later else android.R.string.cancel, null)
                .setCancelable(false)
                .create()
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethodExt { url ->
            CustomTabsUtil.openInCustomTab(activity, url)
        }
    }

    private fun fallsWithinGeoRange(): Boolean {
        val languages = WikipediaApp.instance.appLanguageState.appLanguageCodes
        val country = GeoUtil.geoIPCountry.orEmpty()
        return (languages.contains("hi") ||
                languages.contains("id") ||
                languages.contains("ja") ||
                ((languages.contains("ar") || languages.contains("fr")) && (country == "MA" || country == "EG" || country == "ML" || country == "CD")) ||
                (languages.contains("en") && (country == "IN" || country == "NG")))
    }

    private fun takeUserToSurvey(context: Context) {
        Prefs.showTalkPageSurveyAttempts = 10
        CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
    }

    private fun getLanguageSpecificUrl(): String {
        return when (WikipediaApp.instance.appLanguageState.appLanguageCode) {
            "ar" -> "https://docs.google.com/forms/d/e/1FAIpQLScEeo2yy3xrJTx2iRn4DjnzGlCftyaGVoPHbg7Vp4TBNM2C1g/viewform"
            "fr" -> "https://docs.google.com/forms/d/e/1FAIpQLSeKsILKIKzdgzrwZjHPmThSKLbf5ZlN2vPH-iCgCioete94eA/viewform"
            "hi" -> "https://docs.google.com/forms/d/e/1FAIpQLSeYSHaa5Gd3PPDi0g14suUC-6LiKsQnyIFB-v8mA_3aCW73mA/viewform"
            "id" -> "https://docs.google.com/forms/d/e/1FAIpQLSeL9LTtHMJDdUevJYXp3Z5NS1QF2j7SovTX6RaCHbcIFcOPfA/viewform"
            "ja" -> "https://docs.google.com/forms/d/e/1FAIpQLSf3HTuDp9IGClMAoh2YhcyhtRfNj8GCuRavGyoGi5OC46ElCw/viewform"
            else -> "https://docs.google.com/forms/d/e/1FAIpQLSeF-G-QNaDUSGGx8LTCD92xbALq2KvZTDpN9NavCVZtajiOew/viewform"
        }
    }
}
