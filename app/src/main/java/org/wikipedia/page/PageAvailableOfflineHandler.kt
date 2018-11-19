package org.wikipedia.page

import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage

object PageAvailableOfflineHandler {
    interface Callback {
        fun onFinish(available: Boolean)
    }

    fun check(page: ReadingListPage, callback: Callback) {
        callback.onFinish(WikipediaApp.getInstance().isOnline || (page.offline() && !page.saving()))
    }

    @SuppressLint("CheckResult")
    fun check(pageTitle: PageTitle, callback: Callback) {
        if (WikipediaApp.getInstance().isOnline) {
            callback.onFinish(true)
            return
        }

        Observable.fromCallable { ReadingListDbHelper.instance().findPageInAnyList(pageTitle) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onFinish(it!!.offline() && !it.saving())
                }, {
                    callback.onFinish(false)
                })
    }
}