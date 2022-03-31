package org.wikipedia.talk

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.log.L

class TalkTopicsProvider(title: PageTitle) {

    interface Callback {
        fun onUpdatePageTitle(title: PageTitle)
        fun onReceivedRevision(revision: MwQueryPage.Revision?)
        fun onSuccess(title: PageTitle, talkPage: TalkPage)
        fun onError(throwable: Throwable)
        fun onFinished()
    }

    private val pageTitle = title.copy()
    private var resolveTitleRequired = false
    private val disposables = CompositeDisposable()

    init {
        // Determine whether we need to resolve the PageTitle, since the calling activity might
        // have given us a non-Talk page, and we need to prepend the correct namespace.
        if (pageTitle.namespace.isEmpty()) {
            pageTitle.namespace = TalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.isUserPage) {
            pageTitle.namespace = UserTalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.namespace() != Namespace.TALK && pageTitle.namespace() != Namespace.USER_TALK) {
            // defer resolution of Talk page title for an API call.
            resolveTitleRequired = true
        }
    }

    fun load(callback: Callback) {
        cancel()
        disposables.add(if (resolveTitleRequired) {
                ServiceFactory.get(pageTitle.wikiSite).getPageNamespaceWithSiteInfo(pageTitle.prefixedText)
            } else {
                Observable.just(MwQueryResponse())
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { response ->
                resolveTitleRequired = false
                response.query?.namespaces?.let { namespaces ->
                    response.query?.firstPage()?.let { page ->
                        // In MediaWiki, namespaces that are even-numbered are "regular" pages,
                        // and namespaces that are odd-numbered are the "Talk" versions of the
                        // corresponding even-numbered namespace. For example, "User"=2, "User talk"=3.
                        // So then, if the namespace of our pageTitle is even (i.e. not a Talk page),
                        // then increment the namespace by 1, and update the pageTitle with it.
                        val newNs = namespaces.values.find { it.id == page.namespace().code() + 1 }
                        if (page.namespace().code() % 2 == 0 && newNs != null) {
                            pageTitle.namespace = newNs.name
                        }
                    }
                }
                callback.onUpdatePageTitle(pageTitle)
                Observable.zip(ServiceFactory.get(pageTitle.wikiSite).getLastModified(pageTitle.prefixedText),
                    ServiceFactory.getRest(pageTitle.wikiSite).getTalkPage(pageTitle.prefixedText)) {
                        lastModified, talkPage -> Pair(lastModified, talkPage)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                callback.onFinished()
            }
            .subscribe({ pair ->
                pair.first.query?.firstPage()?.revisions?.firstOrNull()?.let { revision ->
                    callback.onReceivedRevision(revision)
                }
                callback.onSuccess(pageTitle, pair.second)
            }, {
                L.e(it)
                callback.onError(it)
            }))
    }

    fun cancel() {
        disposables.clear()
    }
}
