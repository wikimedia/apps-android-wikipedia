package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_image_detail.view.*
import org.wikipedia.R

class ImageDetailView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_image_detail, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setDetail(detail: String) {
        detailTextView.text = detail
    }
}
