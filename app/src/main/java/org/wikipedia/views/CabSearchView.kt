package org.wikipedia.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import org.wikipedia.R
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class CabSearchView(
    context: Context,
    attrs: AttributeSet? = null
) : SearchView(context, attrs, androidx.appcompat.R.attr.searchViewStyle) {

    private val searchCloseBtn: ImageView
    @SuppressLint("RestrictedApi")
    private val searchSrcTextView: SearchAutoComplete

    init {
        val themedIconColor = ResourceUtil.getThemedColor(getContext(), R.attr.placeholder_color)
        searchSrcTextView = findViewById(androidx.appcompat.R.id.search_src_text)
        searchSrcTextView.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.primary_color))
        searchSrcTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SEARCH_TEXT_SIZE.toFloat())
        searchSrcTextView.setHintTextColor(themedIconColor)
        searchCloseBtn = findViewById(androidx.appcompat.R.id.search_close_btn)
        searchCloseBtn.visibility = GONE
        searchCloseBtn.setColorFilter(themedIconColor)
        FeedbackUtil.setButtonLongPressToast(searchCloseBtn)
        searchSrcTextView.filters += PlainTextInputFilter()
    }

    fun selectAllQueryTexts() {
        searchSrcTextView.selectAll()
    }

    fun setSearchHintTextColor(color: Int) {
        searchSrcTextView.setHintTextColor(color)
    }

    fun setCloseButtonVisibility(searchString: String?) {
        val isEmpty = searchString.isNullOrEmpty()
        searchCloseBtn.isGone = isEmpty
        searchCloseBtn.setImageResource(if (isEmpty) 0 else R.drawable.ic_close_black_24dp)
        searchCloseBtn.imageTintList = ResourceUtil.getThemedColorStateList(context, R.attr.placeholder_color)
    }

    private class PlainTextInputFilter : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned,
                            dstart: Int, dend: Int): CharSequence {
            return RichTextUtil.stripRichText(source, start, end).subSequence(start, end)
        }
    }

    companion object {
        private const val SEARCH_TEXT_SIZE = 16
    }
}
