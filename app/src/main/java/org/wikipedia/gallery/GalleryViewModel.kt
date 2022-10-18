package org.wikipedia.gallery

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

class GalleryViewModel(bundle: Bundle): ViewModel() {

    var pageTitle = bundle.getParcelable<PageTitle>(GalleryActivity.EXTRA_PAGETITLE)
    var fileName = bundle.getString(GalleryActivity.EXTRA_FILENAME)
    var revision = bundle.getLong(GalleryActivity.EXTRA_REVISION)
    var source = bundle.getInt(GalleryActivity.EXTRA_SOURCE)
    var wiki = bundle.getParcelable<WikiSite>(GalleryActivity.EXTRA_WIKI)


    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(bundle) as T
        }
    }
}