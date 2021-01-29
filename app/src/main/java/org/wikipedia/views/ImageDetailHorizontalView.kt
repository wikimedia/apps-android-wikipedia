package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewImageDetailHorizontalBinding
import org.wikipedia.util.StringUtil

class ImageDetailHorizontalView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val binding = ViewImageDetailHorizontalBinding.inflate(LayoutInflater.from(context), this)

    init {
        if (attrs != null) {
            val array = getContext().obtainStyledAttributes(attrs, R.styleable.ImageDetailHorizontalView, 0, 0)
            binding.titleText.text = array.getString(R.styleable.ImageDetailHorizontalView_title)
            setDetailText(array.getString(R.styleable.ImageDetailHorizontalView_detail))
            array.recycle()
        }
        orientation = HORIZONTAL
    }

    fun setDetailText(detail: String?) {
        if (!detail.isNullOrEmpty()) {
            binding.detailText.text = StringUtil.removeHTMLTags(detail)
        }
    }
}
