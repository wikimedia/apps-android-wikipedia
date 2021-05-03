package org.wikipedia.analytics

import android.text.TextUtils
import org.apache.commons.lang3.StringUtils
import org.json.JSONObject
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.PageTitle
import java.util.*

open class EditFunnel(app: WikipediaApp, private val title: PageTitle) :
        Funnel(app, SCHEMA_NAME, REV_ID, title.wikiSite) {

    open fun logStart() {
        log(
                "action", "start"
        )
    }

    fun logPreview() {
        log(
                "action", "preview"
        )
    }

    fun logSaved(revID: Long, source: String?) {
        log(
                "action", "saved",
                "revID", revID,
                "source", source
        )
    }

    open fun logSaved(revID: Long) {
        log(
                "action", "saved",
                "revID", revID
        )
    }

    fun logLoginAttempt() {
        log(
                "action", "loginAttempt"
        )
    }

    fun logLoginSuccess() {
        log(
                "action", "loginSuccess"
        )
    }

    fun logLoginFailure() {
        log(
                "action", "loginFailure"
        )
    }

    fun logCaptchaShown() {
        log(
                "action", "captchaShown"
        )
    }

    fun logCaptchaFailure() {
        log(
                "action", "captchaFailure"
        )
    }

    open fun logAbuseFilterWarning(code: String?) {
        log(
                "action", "abuseFilterWarning",
                "abuseFilterName", code
        )
    }

    fun logAbuseFilterWarningIgnore(code: String?) {
        log(
                "action", "abuseFilterWarningIgnore",
                "abuseFilterName", code
        )
    }

    fun logAbuseFilterWarningBack(code: String?) {
        log(
                "action", "abuseFilterWarningBack",
                "abuseFilterName", code
        )
    }

    fun logAbuseFilterError(code: String?) {
        log(
                "action", "abuseFilterError",
                "abuseFilterName", code
        )
    }

    open fun logError(code: String?) {
        log(
                "action", "error",
                "errorText", code
        )
    }

    fun logEditSummaryTap(summaryTagStringID: Int) {
        val summaryTag: String = when (summaryTagStringID) {
            R.string.edit_summary_tag_typo -> "typo"
            R.string.edit_summary_tag_grammar -> "grammar"
            R.string.edit_summary_tag_links -> "links"
            R.string.edit_summary_tag_other -> "other"
            else -> throw RuntimeException("Need to add new summary tags to EditFunnel")
        }
        log(
                "action", "editSummaryTap",
                "editSummaryTapped", summaryTag
        )
    }

    open fun logSaveAttempt() {
        log(
                "action", "saveAttempt"
        )
    }

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "anon", !AccountUtil.isLoggedIn)
        preprocessData(eventData, "pageNS", if (!TextUtils.isEmpty(title.namespace))
            StringUtils.capitalize(title.namespace!!.toLowerCase(Locale.getDefault())) else title.namespace)
        return super.preprocessData(eventData)!!
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppEdit"
        private const val REV_ID = 20710930
    }
}
