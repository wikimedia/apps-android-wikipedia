package org.wikipedia.random

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.text.ListFormatter
import android.os.Build
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
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.RemoveFromReadingListsDialog
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil

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
            lifecycleScope.launch {
                val lists = AppDatabase.instance.readingListDao().getListsFromPageOccurrences(AppDatabase.instance.readingListPageDao().getAllPageOccurrences(title))
                if (lists.isEmpty()) {
                    return@launch
                }
                RemoveFromReadingListsDialog(lists).deleteOrShowDialog(this@RandomActivity) { readingLists, _ ->
                    if (!this@RandomActivity.isDestroyed) {
                        val names = readingLists.map { it.title }.run {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ListFormatter.getInstance().format(this)
                            } else {
                                joinToString(separator = ", ")
                            }
                        }
                        FeedbackUtil.showMessage(this@RandomActivity, getString(R.string.reading_list_item_deleted_from_list, title.displayText, names))
                    }
                }
            }
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(this, title, true, InvokeSource.RANDOM_ACTIVITY) {
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
