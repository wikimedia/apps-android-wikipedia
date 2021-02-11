package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import org.wikipedia.databinding.ViewSearchEmptyBinding

class SearchEmptyView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    private val binding = ViewSearchEmptyBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }

    fun setEmptyText(@StringRes id: Int) {
        binding.searchEmptyText.setText(id)
    }

    fun setEmptyText(text: CharSequence?) {
        binding.searchEmptyText.text = text
    }
}
