package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewDescriptionEditSuccessBinding

class DescriptionEditSuccessView : FrameLayout {
    interface Callback {
        fun onDismissClick()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val binding = ViewDescriptionEditSuccessBinding.inflate(LayoutInflater.from(context), this, true)
    var callback: Callback? = null

    init {
        val editHint = resources.getString(R.string.description_edit_success_article_edit_hint)
        binding.viewDescriptionEditSuccessHintText.setTextWithDrawables(editHint, R.drawable.ic_mode_edit_white_24dp)
        binding.viewDescriptionEditSuccessDoneButton.setOnClickListener {
            callback?.onDismissClick()
        }
    }
}
