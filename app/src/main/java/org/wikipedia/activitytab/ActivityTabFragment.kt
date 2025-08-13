package org.wikipedia.activitytab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.categories.CategoryActivity
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.util.UiState

class ActivityTabFragment : Fragment() {

    interface Callback {
        fun onNavigateToReadingLists()
    }

    private val viewModel: ActivityTabViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Prefs.activityTabRedDotShown = true

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    ActivityTabScreen(
                        uiState = viewModel.uiState.collectAsState().value,
                        categoriesUiState = viewModel.categoriesUiState.collectAsState().value,
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            retryClickListener = {
                                viewModel.load()
                            }
                        )
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        viewModel.loadCategories()
    }

    @Composable
    fun ActivityTabScreen(
        uiState: UiState<Unit>,
        categoriesUiState: UiState<List<Category>>,
        wikiErrorClickEvents: WikiErrorClickEvents? = null
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            TopCategoriesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                uiState = categoriesUiState,
                onDiscoverBtnCLick = {
                    callback()?.onNavigateToReadingLists()
                },
                onClick = {
                    val pageTitle = viewModel.createPageTitleForCategory(it)
                     startActivity(CategoryActivity.newIntent(requireActivity(), pageTitle))
                },
                onFormatString = { viewModel.formateString(it) },
                wikiErrorClickEvents = WikiErrorClickEvents(
                    retryClickListener = {
                        viewModel.loadCategories()
                    }
                )
            )
        }
    }

    companion object {
        fun newInstance(): ActivityTabFragment {
            return ActivityTabFragment().apply {
                arguments = Bundle().apply {
                    // TODO
                }
            }
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }
}
