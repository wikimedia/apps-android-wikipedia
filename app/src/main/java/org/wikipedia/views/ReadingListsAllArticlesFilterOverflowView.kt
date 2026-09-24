package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import org.wikipedia.databinding.ViewReadingListsAllArticlesFilterOverflowBinding
import org.wikipedia.readinglist.SavedArticleFilter

class ReadingListsAllArticlesFilterOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun onOptionSelected(option: SavedArticleFilter)
    }

    private val binding = ViewReadingListsAllArticlesFilterOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var popupWindowHost: PopupWindow? = null

    fun show(anchorView: View, selectedOption: SavedArticleFilter, callback: Callback) {
        updateSelectedOption(selectedOption)

        binding.filterAllArticles.setOnClickListener {
            selectOption(SavedArticleFilter.ALL_ARTICLES, callback)
        }
        binding.filterNotInCollection.setOnClickListener {
            selectOption(SavedArticleFilter.NOT_IN_COLLECTION, callback)
        }

        popupWindowHost = PopupWindow(
            this,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).also {
            it.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }
    }

    private fun selectOption(option: SavedArticleFilter, callback: Callback) {
        updateSelectedOption(option)
        callback.onOptionSelected(option)
        popupWindowHost?.dismiss()
        popupWindowHost = null
    }

    private fun updateSelectedOption(option: SavedArticleFilter) {
        binding.filterAllArticlesSelected.isVisible = option == SavedArticleFilter.ALL_ARTICLES
        binding.filterAllArticlesSelected.isSelected = option == SavedArticleFilter.ALL_ARTICLES

        binding.filterNotInCollectionSelected.isVisible = option == SavedArticleFilter.NOT_IN_COLLECTION
        binding.filterNotInCollectionSelected.isSelected = option == SavedArticleFilter.NOT_IN_COLLECTION
    }
}
