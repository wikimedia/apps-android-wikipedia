package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.wikipedia.databinding.ViewSearchEmptyBinding

class RecommendedContentView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    private val binding = ViewSearchEmptyBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }
}
