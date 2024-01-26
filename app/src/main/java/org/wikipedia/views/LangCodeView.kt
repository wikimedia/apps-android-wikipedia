package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewLangCodeBinding
import org.wikipedia.language.LanguageUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class LangCodeView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewLangCodeBinding.inflate(LayoutInflater.from(context), this)
    private val primaryColor = ResourceUtil.getThemedColor(context, R.attr.primary_color)
    init {
        val (textColor, backgroundTint, fillBackground) = context
            .obtainStyledAttributes(attrs, R.styleable.LangCodeView)
            .use {
                Triple(
                    it.getColor(R.styleable.LangCodeView_textColor, primaryColor),
                    it.getColor(R.styleable.LangCodeView_backgroundTint, primaryColor),
                    it.getBoolean(R.styleable.LangCodeView_fillBackground, false)
                )
            }

        layoutParams = ViewGroup.LayoutParams(DimenUtil.roundedDpToPx(48.0f), ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackgroundBorderless))
        setTextColor(textColor)
        setBackgroundTint(backgroundTint)
        fillBackground(fillBackground)
        isFocusable = true
    }

    fun setLangCode(langCode: String) {
        binding.langCodeText.text = LanguageUtil.formatLangCodeForButton(langCode.uppercase())
        val textSize = if (langCode.length > 2) 5f else 10f
        binding.langCodeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    }

    fun setTextColor(@ColorInt textColor: Int) {
        binding.langCodeText.setTextColor(textColor)
    }

    fun setTextColor(colors: ColorStateList) {
        binding.langCodeText.setTextColor(colors)
    }

    fun fillBackground(fillBackground: Boolean) {
        binding.langCodeText.setBackgroundResource(if (fillBackground) R.drawable.tab_counts_shape_border_filled else R.drawable.tab_counts_shape_border)
    }

    fun setBackgroundTint(@ColorInt tintColor: Int) {
        ViewCompat.setBackgroundTintList(binding.langCodeText, ColorStateList.valueOf(tintColor))
    }

    fun setBackgroundTint(colors: ColorStateList) {
        ViewCompat.setBackgroundTintList(binding.langCodeText, colors)
    }
}
