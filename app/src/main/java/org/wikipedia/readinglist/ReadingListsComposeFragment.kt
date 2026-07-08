package org.wikipedia.readinglist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.views.ReadingListsOverflowView

class ReadingListsComposeFragment : Fragment() {

    private var currentSearchQuery by mutableStateOf<String?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    ReadingListsComposeScreen(searchQuery = currentSearchQuery)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().invalidateOptionsMenu()
    }

    // Search action mode
    fun startSearchActionMode() {
        (requireActivity() as AppCompatActivity).startSupportActionMode(searchActionModeCallback)
    }

    // TODO migration: filter the list based on the search query
    private val searchActionModeCallback = object : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (isAdded) {
                (requireParentFragment() as MainFragment).setBottomNavVisible(false)
            }
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            currentSearchQuery = s.trim()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            currentSearchQuery = null
            if (isAdded) {
                (requireParentFragment() as MainFragment).setBottomNavVisible(true)
            }
        }

        override fun getSearchHintString(): String {
            return getString(R.string.filter_hint_filter_my_lists_and_articles)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    // Overflow menu
    fun showReadingListsOverflowMenu() {
        ReadingListsOverflowView(requireContext()).show(
            (requireActivity() as MainActivity).getToolbar()
                .findViewById(R.id.menu_overflow_button), overflowCallback
        )
    }

    // TODO migration: implement these against the Compose list + ViewModel once they exist.
    private val overflowCallback = object : ReadingListsOverflowView.Callback {
        override fun sortByClick() {}

        override fun createNewListClick() {}

        override fun importNewList() {}

        override fun selectListClick() {}

        override fun refreshClick() {}
    }

    companion object {
        fun newInstance(): ReadingListsComposeFragment {
            return ReadingListsComposeFragment()
        }
    }
}
