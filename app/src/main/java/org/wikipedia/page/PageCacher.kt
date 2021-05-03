package org.wikipedia.page

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.log.L

object PageCacher {
    @JvmStatic
    fun loadIntoCache(title: PageTitle) {
        L.d("Loading page into cache: " + title.prefixedText)
        ServiceFactory.getRest(title.wikiSite)
                .getSummaryResponse(title.prefixedText, null, null, null, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ summaryRsp ->
                    WikipediaApp.getInstance().tabList.asReversed().find { it.backStackPositionTitle == title }?.backStackPositionTitle?.apply {
                        thumbUrl = summaryRsp.body()!!.thumbnailUrl
                    }
                }) { caught -> L.e(caught) }
    }
}
