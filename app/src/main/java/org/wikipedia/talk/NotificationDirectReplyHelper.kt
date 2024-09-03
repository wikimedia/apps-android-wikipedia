package org.wikipedia.talk

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

object NotificationDirectReplyHelper {
    private const val DIRECT_REPLY_EDIT_COMMENT = "#directreply-1.0"

    // TODO: This is not being used anywhere. Please test before using it.
    // TODO: update this to use DiscussionTools API, and enable.
    fun handleReply(context: Context, wiki: WikiSite, title: PageTitle, replyText: String,
                    replyTo: String, notificationId: Int) {
        Toast.makeText(context, context.getString(R.string.notifications_direct_reply_progress, replyTo), Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            fallBackToTalkPage(context, title)
        }) {
            val token = async { CsrfTokenClient.getToken(wiki).blockingFirst() }
            val talkPageResponse = async { ServiceFactory.getRest(wiki).getTalkPage(title.prefixedText) }
            val topic = talkPageResponse.await().topics?.find {
                it.id > 0 && it.html?.trim().orEmpty() == StringUtil.removeUnderscores(title.fragment)
            }

            topic?.let {
                val submitResponse = ServiceFactory.get(wiki).postEditSubmitSuspend(
                    title.prefixedText, topic.id.toString(), null,
                    DIRECT_REPLY_EDIT_COMMENT, AccountUtil.assertUser, null, replyText,
                    talkPageResponse.await().revision, token.await(), null, null
                )
                if (submitResponse.edit?.editSucceeded == true) {
                    waitForUpdatedRevision(context, wiki, title, submitResponse.edit.newRevId, notificationId)
                } else {
                    fallBackToTalkPage(context, title)
                }
            }
        }
    }

    private suspend fun waitForUpdatedRevision(context: Context, wiki: WikiSite, title: PageTitle, newRevision: Long, notificationId: Int) {
        var tries = 0
        do {
            if (tries > 0) {
                delay(2000)
            }
            val talkPageResponse = ServiceFactory.getRest(wiki).getTalkPage(title.prefixedText)
            tries++
        } while (talkPageResponse.revision < newRevision && tries < 20)
        Toast.makeText(context, R.string.notifications_direct_reply_success, Toast.LENGTH_LONG).show()
        cancelNotification(context, notificationId)
    }

    private fun fallBackToTalkPage(context: Context, title: PageTitle) {
        Toast.makeText(context, R.string.notifications_direct_reply_error, Toast.LENGTH_LONG).show()
        context.startActivity(TalkTopicsActivity.newIntent(context, title, Constants.InvokeSource.NOTIFICATION))
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            return
        }
        context.getSystemService<NotificationManager>()?.activeNotifications?.find { it.id == notificationId }?.run {
            val n = NotificationCompat.Builder(context, this.notification)
                    .setRemoteInputHistory(null)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setVibrate(null)
                    .setTimeoutAfter(1)
                    .build()
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling ActivityCompat#requestPermissions
                return
            }
            NotificationManagerCompat.from(context).notify(notificationId, n)
        }
    }
}
