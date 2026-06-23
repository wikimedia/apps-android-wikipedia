package org.wikipedia.feed.didyouknow

import android.content.Context
import android.content.Intent
import android.icu.text.ListFormatter
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.components.menu.PageOverflowMenu
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.RemoveFromReadingListsDialog
import org.wikipedia.theme.Theme
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import kotlin.collections.orEmpty
import kotlin.getValue

class DidYouKnowActivity : BaseActivity() {
    private val pageOverflowMenuViewModel: PageOverflowMenuViewModel by viewModels()

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
                    },
                    overflowMenuState = pageOverflowMenuViewModel.pageOverflowMenuState,
                    onPageOverflowDismiss = {
                        pageOverflowMenuViewModel.dismissPageOverflowMenu()
                    },
                    onPageOverflowClick = { pageSummary, source, menuKey ->
                        pageOverflowMenuViewModel.onPageOverflowClick(
                            context = this,
                            wikiSite = wikiSite,
                            pageSummary = pageSummary,
                            source = source,
                            menuKey = menuKey,
                            onOpenPage = { entry ->
                                startActivity(PageActivity.newIntentForCurrentTab(this, entry, entry.title))
                            },
                            onOpenInNewTab = { entry ->
                                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
                            },
                            onAddRequest = { entry, addToDefault ->
                                ReadingListBehaviorsUtil.addToDefaultList(this, entry.title, addToDefault, InvokeSource.DID_YOU_KNOW)
                            },
                            onMoveRequest = { id, entry ->
                                ReadingListBehaviorsUtil.moveToList(this, id, entry.title, InvokeSource.FEED)
                            },
                            onRemoveRequest = { entry, lists ->
                                RemoveFromReadingListsDialog(lists).deleteOrShowDialog(this) { readingLists, _ ->
                                    if (!this.isDestroyed) {
                                        val names = readingLists.map { it.title }.run {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                ListFormatter.getInstance().format(this)
                                            } else {
                                                joinToString(separator = ", ")
                                            }
                                        }
                                        FeedbackUtil.showMessage(this, getString(R.string.reading_list_item_deleted_from_list, entry.title.displayText, names))
                                    }
                                }
                            },
                            onShareRequest = { entry ->
                                ShareUtil.shareText(this, entry.title.displayText, entry.title.uri)
                            },
                            onLinkCopyRequest = { entry ->
                                ClipboardUtil.setPlainText(this, text = entry.title.uri)
                                FeedbackUtil.showMessage(this, R.string.address_copied)
                            }
                        )
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
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    onPageOverflowDismiss: () -> Unit = {},
    onPageOverflowClick: (pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _ -> },
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
            dykHtmlList.forEachIndexed { index, html ->
                item {
                    DidYouKnowListItem(
                        wikiSite = wikiSite,
                        dykHtml = html,
                        onClick = onPageClick,
                        pageOverflowContent = {
                            PageOverflowMenu(
                                menuKey = "dyk-$index",
                                overflowMenuState = overflowMenuState,
                                onDismiss = onPageOverflowDismiss,
                                items = overflowMenuState?.items.orEmpty()
                            )
                        },
                        onPageOverflowClick = { onPageOverflowClick(it, HistoryEntry.SOURCE_ACTIVITY_DID_YOU_KNOW, "dyk-$index") }
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
