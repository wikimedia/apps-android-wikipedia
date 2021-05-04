package org.wikipedia.readinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.ResourceUtil

class ReadingListItemActionsDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun onToggleItemOffline(pageId: Long)
        fun onShareItem(pageId: Long)
        fun onAddItemToOther(pageId: Long)
        fun onMoveItemToOther(pageId: Long)
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

        ReadingListDbHelper.instance().getPageById(requireArguments().getLong(ARG_READING_LIST_PAGE))?.let {
            readingListPage = it
            val removeFromListText = if (requireArguments().getInt(ARG_READING_LIST_SIZE) == 1) getString(R.string.reading_list_remove_from_list, requireArguments().getString(ARG_READING_LIST_NAME)) else getString(R.string.reading_list_remove_from_lists)
            actionsView.setState(it.displayTitle, removeFromListText, it.offline, requireArguments().getBoolean(ARG_READING_LIST_HAS_ACTION_MODE))
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

        override fun onAddToOther() {
            dismiss()
            readingListPage?.let {
                callback()?.onAddItemToOther(it.id)
            }
        }

        override fun onMoveToOther() {
            dismiss()
            readingListPage?.let {
                callback()?.onMoveItemToOther(it.id)
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

        fun newInstance(lists: List<ReadingList>, pageID: Long, hasActionMode: Boolean): ReadingListItemActionsDialog {
            return ReadingListItemActionsDialog().apply {
                arguments = bundleOf(ARG_READING_LIST_NAME to lists[0].title,
                        ARG_READING_LIST_SIZE to lists.size,
                        ARG_READING_LIST_PAGE to pageID,
                        ARG_READING_LIST_HAS_ACTION_MODE to hasActionMode)
            }
        }
    }
}
