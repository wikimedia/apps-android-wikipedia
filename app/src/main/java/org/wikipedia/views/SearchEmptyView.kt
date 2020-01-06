package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_search_empty.view.*
import org.wikipedia.R

class SearchEmptyView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        View.inflate(context, R.layout.view_search_empty, this)
        orientation = VERTICAL
    }

    fun setEmptyText(@StringRes id: Int) = search_empty_text.setText(id)

    fun setEmptyText(text: CharSequence?) {
        search_empty_text.text = text
    }
}
