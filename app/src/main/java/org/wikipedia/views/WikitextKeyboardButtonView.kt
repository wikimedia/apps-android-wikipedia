package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ViewWikitextKeyboardButtonBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.ResourceUtil

class WikitextKeyboardButtonView constructor(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    val binding = ViewWikitextKeyboardButtonBinding.inflate(LayoutInflater.from(context), this)

    init {
        val size = DimenUtil.roundedDpToPx(48f)
        layoutParams = ViewGroup.LayoutParams(size, size)
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.WikitextKeyboardButtonView) {
                val drawableId = getResourceId(R.styleable.WikitextKeyboardButtonView_buttonImage, 0)
                val buttonText = getString(R.styleable.WikitextKeyboardButtonView_buttonText)
                val buttonHint = getString(R.styleable.WikitextKeyboardButtonView_buttonHint)
                val buttonTextColor = getColor(R.styleable.WikitextKeyboardButtonView_buttonTextColor,
                        ResourceUtil.getThemedColor(context, R.attr.primary_color))
                if (drawableId != 0) {
                    binding.wikitextButtonText.visibility = GONE
                    binding.wikitextButtonImage.visibility = VISIBLE
                    binding.wikitextButtonImage.setImageResource(drawableId)
                } else {
                    binding.wikitextButtonText.visibility = VISIBLE
                    binding.wikitextButtonImage.visibility = GONE
                    if (!buttonText.isNullOrEmpty()) {
                        binding.wikitextButtonText.text = buttonText
                    }
                    binding.wikitextButtonText.setTextColor(buttonTextColor)
                }
                if (!buttonHint.isNullOrEmpty()) {
                    contentDescription = buttonHint
                    setButtonLongPressToast(this@WikitextKeyboardButtonView)
                }
            }
        }
        isClickable = true
        isFocusable = true
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, android.R.attr.selectableItemBackground))
    }

    fun setExpandable(expandable: Boolean) {
        binding.expandNotch.isVisible = expandable
    }

    fun setExpanded(expanded: Boolean) {
        val color = ResourceUtil.getThemedColor(context, if (expanded) R.attr.progressive_color else R.attr.primary_color)
        binding.wikitextButtonImage.imageTintList = ColorStateList.valueOf(color)
        binding.expandNotch.setImageResource(if (expanded) R.drawable.ic_arrow_drop_up_24 else R.drawable.ic_arrow_drop_down_black_24dp)
        binding.expandNotch.imageTintList = ColorStateList.valueOf(color)
        binding.wikitextButtonText.setTextColor(color)
    }
}
