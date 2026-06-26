package org.wikipedia.random

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.theme.Theme

class RandomActivity : BaseActivity() {

    private val viewModel: RandomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setStatusBarColor(Color.TRANSPARENT)
        setNavigationBarColor(Color.TRANSPARENT)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            BaseTheme(currentTheme = Theme.BLACK) {
                RandomScreen(
                    viewModel = viewModel,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onArticleClick = ::openArticle,
                    onSaveClick = ::onSaveClick
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                FlowEventBus.events.collectLatest { event ->
                    if (event is ArticleSavedOrDeletedEvent) {
                        viewModel.refreshSaveState()
                    }
                }
            }
        }
    }

    private fun openArticle(title: PageTitle) {
        startActivity(
            PageActivity.newIntentForNewTab(
                this,
                HistoryEntry(title, HistoryEntry.SOURCE_RANDOM),
                title
            )
        )
    }

    private fun onSaveClick(title: PageTitle) {
        if (viewModel.saveButtonState) {
            LongPressMenu(window.decorView, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(this@RandomActivity, title, addToDefault, viewModel.invokeSource) {
                        viewModel.updateSaveState(title)
                    }
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        ReadingListBehaviorsUtil.moveToList(this@RandomActivity, it.listId, title, viewModel.invokeSource) {
                            viewModel.updateSaveState(title)
                        }
                    }
                }
            }).show(HistoryEntry(title, HistoryEntry.SOURCE_RANDOM))
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(this, title, true, viewModel.invokeSource) {
                viewModel.updateSaveState(title)
            }
        }
    }

    companion object {
        fun newIntent(context: Context, wikiSite: WikiSite, invokeSource: InvokeSource?): Intent {
            return Intent(context, RandomActivity::class.java).apply {
                putExtra(Constants.ARG_WIKISITE, wikiSite)
                putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            }
        }
    }
}
