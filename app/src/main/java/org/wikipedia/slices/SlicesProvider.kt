package org.wikipedia.slices

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.Slice
import androidx.slice.SliceProvider
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.SliceAction
import androidx.slice.builders.list
import androidx.slice.builders.row
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData

class SlicesProvider : SliceProvider() {

    override fun onMapIntentToUri(intent: Intent?): Uri {
        var uriBuilder: Uri.Builder = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
        if (intent == null) return uriBuilder.build()
        val data = intent.data
        if (data != null && data.path != null) {
            val path = data.path?.replace("/", "")
            uriBuilder = uriBuilder.path(path)
        }
        val context = context
        if (context != null) {
            uriBuilder = uriBuilder.authority(context.packageName)
        }
        return uriBuilder.build()
    }

    override fun onCreateSliceProvider(): Boolean {
        return true
    }

    override fun onBindSlice(sliceUri: Uri?): Slice {
        val searchPattern = sliceUri?.path?.removePrefix("/")
        val pageTitle = getPageTitle(searchPattern)
        return list(context!!, sliceUri!!, ListBuilder.INFINITY) {
            row {
                primaryAction = createActivityAction(pageTitle)
                title = pageTitle.displayText
            }
        }
    }

    private fun getPageTitle(searchPattern: String?): PageTitle {

        if (searchPattern.isNullOrEmpty()) {
            return PageTitle(MainPageNameData.valueFor(WikipediaApp.getInstance().appOrSystemLanguageCode), WikipediaApp.getInstance().wikiSite)
        }

        return runBlocking {
            val pageSummary = withContext(Dispatchers.IO) {
                ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getSummary(null, searchPattern!!)
            }
            return@runBlocking pageSummary.blockingFirst().getPageTitle(WikipediaApp.getInstance().wikiSite)
        }
    }

    private fun createActivityAction(pageTitle: PageTitle): SliceAction {

        val intent = Intent(context, PageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(PageActivity.EXTRA_PAGETITLE, pageTitle)
        intent.putExtra(Constants.INTENT_FEATURED_ARTICLE_FROM_WIDGET, true)

        return SliceAction.create(
                PendingIntent.getActivity(context, 0, intent, 0),
                IconCompat.createWithResource(context, R.mipmap.launcher),
                ListBuilder.ICON_IMAGE,
                "Enter app"
        )
    }
}
