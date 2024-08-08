package org.wikipedia.recommendedcontent

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import org.wikipedia.databinding.ViewRecommendedContentBinding

class RecommendedContentView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding = ViewRecommendedContentBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        orientation = VERTICAL
    }
}
