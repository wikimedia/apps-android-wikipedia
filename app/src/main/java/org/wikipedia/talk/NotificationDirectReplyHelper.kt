package org.wikipedia.talk

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.edit.Edit
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.util.concurrent.TimeUnit

object NotificationDirectReplyHelper {
    const val DIRECT_REPLY_EDIT_COMMENT = "#directreply-1.0"

    fun handleReply(context: Context, wiki: WikiSite, title: PageTitle, replyText: String,
                    replyTo: String, notificationId: Int) {
        Toast.makeText(context, context.getString(R.string.notifications_direct_reply_progress, replyTo), Toast.LENGTH_SHORT).show()

        Observable.zip(CsrfTokenClient(wiki).token.subscribeOn(Schedulers.io()),
            ServiceFactory.getRest(wiki).getTalkPage(title.prefixedText).subscribeOn(Schedulers.io()), {
                token, response -> Pair(token, response)
            }).subscribeOn(Schedulers.io())
            .flatMap { pair ->
                val topic = pair.second.topics!!.find {
                    it.id > 0 && it.html?.trim().orEmpty() == StringUtil.removeUnderscores(title.fragment)
                }
                if (topic == null || title.fragment.isNullOrEmpty()) {
                    Observable.just(Edit())
                } else {
                    var topicDepth = 0
                    topic.replies?.lastOrNull()?.let {
                        topicDepth = it.depth
                    }
                    val body = TalkTopicActivity.addDefaultFormatting(replyText, topicDepth)
                    ServiceFactory.get(wiki).postEditSubmit(
                        title.prefixedText, topic.id.toString(), null,
                        DIRECT_REPLY_EDIT_COMMENT, if (AccountUtil.isLoggedIn) "user" else null, null, body,
                        pair.second.revision, pair.first, null, null
                    )
                }
            }
            .subscribe({
                if (it.edit?.editSucceeded == true) {
                    waitForUpdatedRevision(context, wiki, title, it.edit.newRevId, notificationId)
                } else {
                    fallBackToTalkPage(context, title)
                }
            }, {
                L.e(it)
                fallBackToTalkPage(context, title)
            })
    }

    private fun waitForUpdatedRevision(context: Context, wiki: WikiSite, title: PageTitle,
                                       newRevision: Long, notificationId: Int) {
        ServiceFactory.getRest(wiki).getTalkPage(title.prefixedText)
            .delay(2, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .map { response ->
                if (response.revision < newRevision) {
                    throw IllegalStateException()
                }
                response.revision
            }
            .retry(20) { t ->
                (t is IllegalStateException) || (t is HttpStatusException && t.code == 404)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnTerminate {
                cancelNotification(context, notificationId)
            }
            .subscribe({
                // revisionForUndo = it
                Toast.makeText(context, R.string.notifications_direct_reply_success, Toast.LENGTH_LONG).show()
            }, { t ->
                L.e(t)
                fallBackToTalkPage(context, title)
            })
    }

    private fun fallBackToTalkPage(context: Context, title: PageTitle) {
        Toast.makeText(context, R.string.notifications_direct_reply_error, Toast.LENGTH_LONG).show()
        context.startActivity(TalkTopicsActivity.newIntent(context, title, Constants.InvokeSource.NOTIFICATION))
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            return
        }
        context.getSystemService<NotificationManager>()?.activeNotifications?.find {
            it.id == notificationId
        }?.run {
            val n = NotificationCompat.Builder(context, this.notification)
                    .setRemoteInputHistory(null)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setVibrate(null)
                    .setTimeoutAfter(1)
                    .build()
            NotificationManagerCompat.from(context).notify(notificationId, n)
        }
    }
}
