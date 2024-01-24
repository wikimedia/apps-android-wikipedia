package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import org.wikipedia.databinding.ViewLangCodeBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class LangCodeView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewLangCodeBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(DimenUtil.roundedDpToPx(48.0f), ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackgroundBorderless))
        isFocusable = true
    }

    fun setLangCode(langCode: String) {
        binding.langCodeText.text = langCode.uppercase()
        val textSize = if (langCode.length > 2) 5f else 10f
        if (langCode.contains("-") && langCode.length > 3) {
            val newLangCode = langCode.uppercase().replace("-", "-\n")
            binding.langCodeText.text = newLangCode
        }
        binding.langCodeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    }
}
