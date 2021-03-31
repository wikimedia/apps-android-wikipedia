package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import org.wikipedia.R
import org.wikipedia.databinding.ViewImageDetailHorizontalBinding
import org.wikipedia.util.StringUtil

class ImageDetailHorizontalView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val binding = ViewImageDetailHorizontalBinding.inflate(LayoutInflater.from(context), this)

    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.ImageDetailHorizontalView) {
                binding.titleText.text = getString(R.styleable.ImageDetailHorizontalView_title)
                setDetailText(getString(R.styleable.ImageDetailHorizontalView_detail))
            }
        }
        orientation = HORIZONTAL
    }

    fun setTitleText(title: String) {
        binding.titleText.text = title
    }

    fun setDetailText(detail: String?) {
        if (!detail.isNullOrEmpty()) {
            binding.detailText.text = StringUtil.removeHTMLTags(detail)
        }
    }
}
