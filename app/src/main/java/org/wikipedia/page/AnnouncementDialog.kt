package org.wikipedia.page

import android.content.Context
import android.net.Uri
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.feed.announcement.AnnouncementCard
import org.wikipedia.feed.announcement.AnnouncementCardView
import org.wikipedia.feed.configure.ConfigureActivity
import org.wikipedia.feed.model.Card
import org.wikipedia.login.LoginActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.UriUtil

class AnnouncementDialog internal constructor(
    context: Context,
    val announcement: Announcement
) : AlertDialog.Builder(context), AnnouncementCardView.Callback {

    private var dialog: AlertDialog? = null

    init {
        val scrollView = ScrollView(context)
        val cardView = AnnouncementCardView(context)
        cardView.card = AnnouncementCard(announcement)
        cardView.localCallback = this
        scrollView.addView(cardView)
        scrollView.isVerticalScrollBarEnabled = true
        setView(scrollView)
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    override fun onAnnouncementPositiveAction(card: Card, uri: Uri) {
        when {
            uri.toString() == UriUtil.LOCAL_URL_LOGIN ->
                context.startActivity(LoginActivity.newIntent(context, LoginActivity.SOURCE_NAV))
            uri.toString() == UriUtil.LOCAL_URL_SETTINGS ->
                context.startActivity(SettingsActivity.newIntent(context))
            uri.toString() == UriUtil.LOCAL_URL_CUSTOMIZE_FEED ->
                context.startActivity(ConfigureActivity.newIntent(context, card.type().code()))
            uri.toString() == UriUtil.LOCAL_URL_LANGUAGES ->
                context.startActivity(WikipediaLanguagesActivity.newIntent(context, Constants.InvokeSource.ANNOUNCEMENT))
            else -> UriUtil.handleExternalLink(context, uri)
        }
        dismissDialog()
    }

    override fun onAnnouncementNegativeAction(card: Card) {
        dismissDialog()
    }

    private fun dismissDialog() {
        Prefs.announcementShownDialogs = setOf(announcement.id)
        dialog?.dismiss()
    }
}
