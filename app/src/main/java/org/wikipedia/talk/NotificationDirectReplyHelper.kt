package org.wikipedia.talk

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
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

    fun handleReply(context: Context, wiki: WikiSite, title: PageTitle, replyText: String, notificationId: Int) {
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
                    val body = TalkTopicActivity.addDefaultFormatting(replyText, topic.depth)
                    ServiceFactory.get(wiki).postEditSubmit(
                        title.prefixedText, topic.id.toString(), null,
                        "", if (AccountUtil.isLoggedIn) "user" else null, null, body,
                        pair.second.revision, pair.first, null, null
                    )
                }
            }
            .doOnTerminate {
                // updateNotification(context, notificationId)
            }
            .subscribe({
                if (it.edit?.editSucceeded == true) {
                    waitForUpdatedRevision(context, wiki, title, it.edit.newRevId)
                } else {
                    fallBackToTalkPage(context, title)
                }
            }, {
                L.e(it)
                fallBackToTalkPage(context, title)
            })
    }

    private fun waitForUpdatedRevision(context: Context, wiki: WikiSite, title: PageTitle, newRevision: Long) {
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

    private fun updateNotification(context: Context, notificationId: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            return
        }
        val notificationManager = context.getSystemService<NotificationManager>()!!
        val notifications = notificationManager.activeNotifications!!
        for (notification in notifications) {
            if (notification.id == notificationId) {
                notificationManager.notify(notificationId, notification.notification)
            }
        }
    }
}
