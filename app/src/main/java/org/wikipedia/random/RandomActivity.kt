package org.wikipedia.random

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.text.ListFormatter
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.extensions.instrument
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.RemoveFromReadingListsDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil

class RandomActivity : BaseActivity() {

    private val viewModel: RandomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setStatusBarColor(Color.TRANSPARENT)
        setNavigationBarColor(Color.BLACK)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        _instrument = TestKitchenAdapter.client.getInstrument("apps-randomizer")
            .startFunnel("randomizer")

        setContent {
            var shakePromptShown by remember { mutableStateOf(Prefs.randomizerShakePromptShown) }

            BaseTheme(currentTheme = Theme.BLACK) {
                RandomScreen(
                    viewModel = viewModel,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onArticleClick = ::openArticle,
                    onSaveClick = ::onSaveClick
                )

                if (!shakePromptShown) {
                    val dismissShakePrompt = {
                        shakePromptShown = true
                        Prefs.randomizerShakePromptShown = true
                    }
                    WikipediaAlertDialog(
                        title = stringResource(R.string.randomizer_shake_prompt_title),
                        titleModifier = Modifier.fillMaxWidth(),
                        message = stringResource(R.string.randomizer_shake_prompt_message),
                        image = {
                            Image(
                                painter = painterResource(R.drawable.shake_to_shuffle),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButtonText = stringResource(R.string.onboarding_got_it),
                        onDismissRequest = dismissShakePrompt,
                        onConfirmButtonClick = dismissShakePrompt
                    )
                }
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
        instrument?.submitInteraction("click", elementId = "article_open", pageData = TestKitchenAdapter.getPageData(pageTitle = title))
        startActivity(PageActivity.newIntentForNewTab(this, HistoryEntry(title, HistoryEntry.SOURCE_RANDOM), title))
    }

    private fun onSaveClick(title: PageTitle) {
        instrument?.submitInteraction("click", elementId = "article_save", pageData = TestKitchenAdapter.getPageData(pageTitle = title))
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
