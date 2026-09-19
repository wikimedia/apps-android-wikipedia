package org.wikipedia.search.semantic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import org.wikipedia.Constants
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class SemanticSearchResultsDialog : ExtendedBottomSheetDialogFragment() {

    private val viewModel: SemanticSearchResultsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    SemanticSearchResultsScreen(
                        viewModel = viewModel,
                        onItemClick = { result, title, inNewTab, fromSnippetLink, position, location ->
                            // TODO: start PageActivity
                        },
                        onCloseClick = {
                            dismiss()
                        },
                        onRatingClick = { rate, searchResult ->
                        },
                        onLoading = { }
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_SEARCH_QUERY = "searchQuery"
        const val ARG_LANGUAGE_CODE = "languageCode"

        fun newInstance(query: String, languageCode: String, invokeSource: Constants.InvokeSource): SemanticSearchResultsDialog {
            val dialog = SemanticSearchResultsDialog()
            dialog.arguments = Bundle().apply {
                putString(ARG_SEARCH_QUERY, query)
                putString(ARG_LANGUAGE_CODE, languageCode)
                putSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            }
            return dialog
        }
    }
}
