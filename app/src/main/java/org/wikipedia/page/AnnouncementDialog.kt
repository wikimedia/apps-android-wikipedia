package org.wikipedia.page

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.feed.announcement.AnnouncementCard
import org.wikipedia.feed.announcement.AnnouncementCardView
import org.wikipedia.feed.configure.ConfigureActivity
import org.wikipedia.feed.model.Card
import org.wikipedia.language.LanguageSettingsInvokeSource
import org.wikipedia.login.LoginActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.UriUtil
import java.util.*

class AnnouncementDialog internal constructor(context: Context, card: AnnouncementCard) : AlertDialog(context), AnnouncementCardView.Callback {
    init {
        val rootView = AnnouncementCardView(context)
        rootView.setCard(card)
        rootView.setCallback(this)
        setView(rootView)
    }

    override fun onAnnouncementPositiveAction(card: Card, uri: Uri) {
        when {
            uri.toString() == UriUtil.LOCAL_URL_LOGIN ->
                context.startActivity(LoginActivity.newIntent(context, LoginFunnel.SOURCE_NAV))
            uri.toString() == UriUtil.LOCAL_URL_SETTINGS ->
                context.startActivity(SettingsActivity.newIntent(context))
            uri.toString() == UriUtil.LOCAL_URL_CUSTOMIZE_FEED ->
                context.startActivity(ConfigureActivity.newIntent(context, card.type().code()))
            uri.toString() == UriUtil.LOCAL_URL_LANGUAGES ->
                context.startActivity(WikipediaLanguagesActivity.newIntent(context, LanguageSettingsInvokeSource.ANNOUNCEMENT.text()))
            else -> UriUtil.handleExternalLink(context, uri)
        }
        dismissDialog()
    }

    override fun onAnnouncementNegativeAction(card: Card) {
        dismissDialog()
    }

    private fun dismissDialog() {
        Prefs.setFundraisingDialogShownInYear(Calendar.getInstance().get(Calendar.YEAR))
        dismiss()
    }
}
