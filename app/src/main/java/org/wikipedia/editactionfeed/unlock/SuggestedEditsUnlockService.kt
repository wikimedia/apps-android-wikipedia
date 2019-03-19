package org.wikipedia.editactionfeed.unlock

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.wikipedia.Constants
import org.wikipedia.util.log.L
import java.util.concurrent.TimeUnit


class SuggestedEditsUnlockService : Service() {

    private val disposables = CompositeDisposable()

    override fun onBind(p0: Intent?): IBinder? {
        throw UnsupportedOperationException("Suggested edits unlock countdown timer service failed")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        L.d("SuggestedEditsUnlockService started")

        val unlockType = intent.getSerializableExtra(INTENT_EXTRA_UNLOCK_TYPE) as Constants.InvokeSource

        disposables.add(Observable.interval(UNLOCK_CHECK_IN_HOURS, TimeUnit.HOURS)
                .map {
                    // TODO: check status from API
                    if (unlockType == Constants.InvokeSource.EDIT_FEED_TITLE_DESC) {
                        // TODO ..
                    } else {
                        // TODO ..
                    }
                    true
                }
                .subscribe({
                    if (it) {
                        // if app is open, and then show dialog; if not, show notification
                        SuggestedEditsUnlockNotifications.showUnlockAddDescriptionNotification(this)
                    }
                }, L::d))


        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        L.d("SuggestedEditsUnlockService canceled")
        disposables.clear()
    }

    companion object {
        private const val UNLOCK_CHECK_IN_HOURS: Long = 24
        const val INTENT_EXTRA_UNLOCK_TYPE = "intentExtraUnlockType"
    }
}
