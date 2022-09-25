package org.wikipedia.page.linkpreview

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class LinkPreviewViewModel : ViewModel() {
    private val _viewState = MutableStateFlow<LinkPreviewViewState>(LinkPreviewViewState.Loading)
    val viewState = _viewState.asStateFlow()
    private val disposables = CompositeDisposable()

    fun loadContent(pageTitle: PageTitle) {
        disposables.add(
            ServiceFactory.getRest(pageTitle.wikiSite)
                .getSummaryResponse(pageTitle.prefixedText, null, null, null, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    _viewState.value = LinkPreviewViewState.Content(response)
                }) { caught ->
                    _viewState.value = LinkPreviewViewState.Error(caught)
                })

    }

    fun loadGallery(pageTitle: PageTitle, revision: Long) {
        if (Prefs.isImageDownloadEnabled) {
            disposables.add(ServiceFactory.getRest(pageTitle.wikiSite)
                .getMediaList(pageTitle.prefixedText, revision)
                .flatMap { mediaList ->
                    val maxImages = 10
                    val items = mediaList.getItems("image", "video").asReversed()
                    val titleList =
                        items.filter { it.showInGallery }.map { it.title }.take(maxImages)
                    if (titleList.isEmpty()) Observable.empty() else ServiceFactory.get(
                        pageTitle.wikiSite
                    ).getImageInfo(titleList.joinToString("|"), pageTitle.wikiSite.languageCode)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    _viewState.value = LinkPreviewViewState.Completed
                }
                .subscribe({ response ->
                    val pageList =
                        response.query?.pages?.filter { it.imageInfo() != null }.orEmpty()
                    _viewState.value = LinkPreviewViewState.Gallery(pageList)
                }) { caught ->
                    L.w("Failed to fetch gallery collection.", caught)
                })
        } else {
            _viewState.value = LinkPreviewViewState.Completed
        }
    }

    fun unBind() {
        disposables.clear()
    }
}