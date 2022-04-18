package org.wikipedia.talk

import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserTalkAliasData

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


    }

    fun cancel() {
        disposables.clear()
    }
}
