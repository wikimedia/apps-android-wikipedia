package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_image_detail_horizontal.view.*
import org.wikipedia.R
import org.wikipedia.util.StringUtil

class ImageDetailHorizontalView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.view_image_detail_horizontal, this)
        if (attrs != null) {
            val array = getContext().obtainStyledAttributes(attrs, R.styleable.ImageDetailHorizontalView, 0, 0)
            titleText!!.text = array.getString(R.styleable.ImageDetailHorizontalView_title)
            setDetailText(array.getString(R.styleable.ImageDetailHorizontalView_detail))
            array.recycle()
        }
        orientation = HORIZONTAL
    }

    fun setDetailText(detail: String?) {
        if (!detail.isNullOrEmpty()) {
            detailText!!.text = StringUtil.removeHTMLTags(detail)
        }
    }
}