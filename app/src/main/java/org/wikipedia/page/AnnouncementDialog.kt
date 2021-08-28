package org.wikipedia.page

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.widget.ScrollView
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.FeedFunnel
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.feed.announcement.AnnouncementCard
import org.wikipedia.feed.announcement.AnnouncementCardView
import org.wikipedia.feed.configure.ConfigureActivity
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.login.LoginActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.UriUtil

class AnnouncementDialog internal constructor(context: Context, val announcement: Announcement) : AlertDialog(context), AnnouncementCardView.Callback {

    // TODO: refactor this item when the new Modern Event Platform is finished.
    private val funnel: FeedFunnel = FeedFunnel(WikipediaApp.instance)

    init {
        val scrollView = ScrollView(context)
        val cardView = AnnouncementCardView(context)
        cardView.card = AnnouncementCard(announcement)
        cardView.localCallback = this
        scrollView.addView(cardView)
        scrollView.isVerticalScrollBarEnabled = true
        setView(scrollView)
    }

    override fun show() {
        funnel.cardShown(CardType.ARTICLE_ANNOUNCEMENT, WikipediaApp.instance.appOrSystemLanguageCode)
        super.show()
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
                context.startActivity(WikipediaLanguagesActivity.newIntent(context, Constants.InvokeSource.ANNOUNCEMENT))
            else -> UriUtil.handleExternalLink(context, uri)
        }
        funnel.cardClicked(CardType.ARTICLE_ANNOUNCEMENT, WikipediaApp.instance.appOrSystemLanguageCode)
        dismissDialog()
    }

    override fun onAnnouncementNegativeAction(card: Card) {
        funnel.dismissCard(CardType.ARTICLE_ANNOUNCEMENT, 0)
        dismissDialog()
    }

    private fun dismissDialog() {
        Prefs.setAnnouncementShownDialogs(setOf(announcement.id))
        dismiss()
    }
}
