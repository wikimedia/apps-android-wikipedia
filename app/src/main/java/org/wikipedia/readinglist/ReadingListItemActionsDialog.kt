package org.wikipedia.readinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.extensions.coroutineScope
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.ResourceUtil

class ReadingListItemActionsDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun onToggleItemOffline(pageId: Long)
        fun onShareItem(pageId: Long)
        fun onManageCollections(pageId: Long)
        fun onSelectItem(pageId: Long)
        fun onDeleteItem(pageId: Long)
    }

    private lateinit var actionsView: ReadingListItemActionsView
    private var readingListPage: ReadingListPage? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        actionsView = ReadingListItemActionsView(requireContext())
        actionsView.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        actionsView.callback = ItemActionsCallback()

        actionsView.coroutineScope().launch {
            AppDatabase.instance.readingListPageDao()
                .getPageById(requireArguments().getLong(ARG_READING_LIST_PAGE))?.let {
                readingListPage = it
                val customCollectionCount = requireArguments().getInt(ARG_CUSTOM_COLLECTION_COUNT)
                val collectionActionText = if (customCollectionCount == 0) {
                    getString(R.string.reading_list_add_to_collection)
                } else {
                    resources.getQuantityString(
                        R.plurals.reading_list_in_collections,
                        customCollectionCount,
                        customCollectionCount
                    )
                }
                val removeFromListText =
                    if (requireArguments().getInt(ARG_READING_LIST_SIZE) == 1) getString(
                        R.string.reading_list_remove_from_list,
                        requireArguments().getString(ARG_READING_LIST_NAME)
                    ) else getString(R.string.reading_list_remove_from_lists)
                actionsView.setState(
                    it.displayTitle,
                    removeFromListText,
                    collectionActionText,
                    it.offline,
                    requireArguments().getBoolean(ARG_READING_LIST_HAS_ACTION_MODE)
                )
            }
        }
        return actionsView
    }

    override fun onDestroyView() {
        actionsView.callback = null
        super.onDestroyView()
    }

    private inner class ItemActionsCallback : ReadingListItemActionsView.Callback {
        override fun onToggleOffline() {
            dismiss()
            readingListPage?.let {
                callback()?.onToggleItemOffline(it.id)
            }
        }

        override fun onShare() {
            dismiss()
            readingListPage?.let {
                callback()?.onShareItem(it.id)
            }
        }

        override fun onManageCollections() {
            dismiss()
            readingListPage?.let {
                callback()?.onManageCollections(it.id)
            }
        }

        override fun onSelect() {
            dismiss()
            readingListPage?.let {
                callback()?.onSelectItem(it.id)
            }
        }

        override fun onDelete() {
            dismiss()
            readingListPage?.let {
                callback()?.onDeleteItem(it.id)
            }
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        private const val ARG_READING_LIST_NAME = "readingListName"
        private const val ARG_READING_LIST_SIZE = "readingListSize"
        private const val ARG_READING_LIST_PAGE = "readingListPage"
        private const val ARG_READING_LIST_HAS_ACTION_MODE = "hasActionMode"
        private const val ARG_CUSTOM_COLLECTION_COUNT = "customCollectionCount"

        fun newInstance(
            readingListName: String,
            readingListSize: Int,
            pageID: Long,
            hasActionMode: Boolean,
            customCollectionCount: Int
        ): ReadingListItemActionsDialog {
            return ReadingListItemActionsDialog().apply {
                arguments = bundleOf(ARG_READING_LIST_NAME to readingListName,
                        ARG_READING_LIST_SIZE to readingListSize,
                        ARG_READING_LIST_PAGE to pageID,
                        ARG_READING_LIST_HAS_ACTION_MODE to hasActionMode,
                        ARG_CUSTOM_COLLECTION_COUNT to customCollectionCount)
            }
        }
    }
}
