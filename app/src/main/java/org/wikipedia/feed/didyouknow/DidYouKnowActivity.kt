package org.wikipedia.feed.didyouknow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme

class DidYouKnowActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wikiSite = intent.parcelableExtra<WikiSite>(Constants.ARG_WIKISITE)!!
        val dykHtmlList = intent.getStringArrayListExtra(DID_YOU_KNOW_ITEMS)!!

        setContent {
            BaseTheme {
                DidYouKnowScreen(
                    wikiSite = wikiSite,
                    dykHtmlList = dykHtmlList,
                    onNavigateBack = { finish() },
                    onPageClick = {
                        val entry = HistoryEntry(it, HistoryEntry.SOURCE_ACTIVITY_DID_YOU_KNOW)
                        startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
                    }
                )
            }
        }
    }

    companion object {
        const val DID_YOU_KNOW_ITEMS = "did_you_know_items"
        fun newIntent(context: Context, wikiSite: WikiSite, items: List<DidYouKnowItem>): Intent {
            return Intent(context, DidYouKnowActivity::class.java)
                .putExtra(Constants.ARG_WIKISITE, wikiSite)
                .putStringArrayListExtra(DID_YOU_KNOW_ITEMS, ArrayList(items.map { it.html }))
        }
    }
}

@Composable
fun DidYouKnowScreen(
    wikiSite: WikiSite,
    dykHtmlList: List<String>,
    onNavigateBack: () -> Unit,
    onPageClick: (PageTitle) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            Column {
                WikiTopAppBar(
                    title = stringResource(R.string.home_feed_did_you_know_title),
                    onNavigationClick = {
                        BreadCrumbLogEvent.logClick(context, "navigationButton")
                        onNavigateBack()
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = paddingValues
        ) {
            dykHtmlList.forEach {
                item {
                    DidYouKnowListItem(
                        context = context,
                        wikiSite = wikiSite,
                        dykHtml = it,
                        onClick = onPageClick,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DidYouKnowScreenPreview() {
    val dykHtml = "...that <a href=\"https://en.wikipedia.org/wiki/Elephant\">elephants</a> have a very long memory?"
    BaseTheme(currentTheme = Theme.LIGHT) {
        DidYouKnowScreen(
            wikiSite = WikiSite.preview(),
            dykHtmlList = listOf(dykHtml, dykHtml, dykHtml),
            onNavigateBack = {},
            onPageClick = {},
        )
    }
}
