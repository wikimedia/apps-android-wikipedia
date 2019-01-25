package org.wikipedia.readinglist

import android.annotation.SuppressLint
import android.text.TextUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingList.SORT_BY_NAME_ASC
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.util.*

object ReadingListSearchUtil {

    interface Callback {
        fun onComplete(pairs: MutableList<Any>)
    }

    @SuppressLint("CheckResult")
    fun searchListsAndPages(searchQuery: String?, callback: Callback) {
        Observable.fromCallable { ReadingListDbHelper.instance().allLists }
                .map {
                    val list = applySearchQuery(searchQuery, it)
                    if (TextUtils.isEmpty(searchQuery)) {
                        ReadingList.sortGenericList(list, Prefs.getReadingListSortMode(SORT_BY_NAME_ASC))
                    }
                    list
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ callback.onComplete(it) }, { L.w(it) })
    }

    private fun applySearchQuery(searchQuery: String?, lists: List<ReadingList>): MutableList<Any> {
        val result = mutableListOf<Any>()

        if (TextUtils.isEmpty(searchQuery)) {
            result.addAll(lists)
            return result
        }

        val normalizedQuery = StringUtils.stripAccents(searchQuery)?.toLowerCase()
        var lastListItemIndex = 0
        for (list in lists) {
            if (StringUtils.stripAccents(list.title()).toLowerCase().contains(normalizedQuery!!)) {
                result.add(lastListItemIndex++, list)
            }
            for (page in list.pages()) {
                if (page.title().toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                    var noMatch = true
                    for (item in result) {
                        if (item is ReadingListPage && item.title() == page.title()) {
                            noMatch = false
                            break
                        }
                    }
                    if (noMatch) {
                        result.add(page)
                    }
                }
            }
        }
        return result
    }
}
