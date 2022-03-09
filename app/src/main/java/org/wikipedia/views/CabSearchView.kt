package org.wikipedia.views

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import org.wikipedia.R
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

/** [SearchView] that exposes contextual action bar callbacks.  */
class CabSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = androidx.appcompat.R.attr.searchViewStyle) :
        SearchView(context, attrs, defStyleAttr) {

    private val searchCloseBtn: ImageView
    private val searchSrcTextView: SearchAutoComplete

    init {
        val themedIconColor = ResourceUtil.getThemedColor(getContext(), R.attr.toolbar_icon_color)
        searchSrcTextView = findViewById(R.id.search_src_text)
        searchSrcTextView.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.primary_text_color))
        searchSrcTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SEARCH_TEXT_SIZE.toFloat())
        searchSrcTextView.setHintTextColor(themedIconColor)
        val searchMagIcon = findViewById<ImageView>(R.id.search_mag_icon)
        searchMagIcon.setColorFilter(themedIconColor)
        searchCloseBtn = findViewById(R.id.search_close_btn)
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
        if (searchString.isNullOrEmpty()) {
            searchCloseBtn.visibility = GONE
            searchCloseBtn.setImageDrawable(null)
        } else {
            searchCloseBtn.visibility = VISIBLE
            searchCloseBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_close_themed_24dp))
        }
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
