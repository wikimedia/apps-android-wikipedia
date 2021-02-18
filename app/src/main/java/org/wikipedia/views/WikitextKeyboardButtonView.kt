package org.wikipedia.views

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import butterknife.ButterKnife
import org.wikipedia.R
import org.wikipedia.databinding.ViewWikitextKeyboardButtonBinding
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.ResourceUtil.getThemedAttributeId
import org.wikipedia.util.ResourceUtil.getThemedColor

class WikitextKeyboardButtonView constructor(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    init {
        val binding = ViewWikitextKeyboardButtonBinding.inflate(LayoutInflater.from(context), this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ButterKnife.bind(this)
        attrs?.let {
            val array = context.obtainStyledAttributes(attrs,
                    R.styleable.WikitextKeyboardButtonView, 0, 0)
            val drawableId = array.getResourceId(R.styleable.WikitextKeyboardButtonView_buttonImage, 0)
            val buttonText = array.getString(R.styleable.WikitextKeyboardButtonView_buttonText)
            val buttonHint = array.getString(R.styleable.WikitextKeyboardButtonView_buttonHint)
            val buttonTextColor = array.getColor(R.styleable.WikitextKeyboardButtonView_buttonTextColor,
                    getThemedColor(context, R.attr.material_theme_secondary_color))
            if (drawableId != 0) {
                binding.wikitextButtonText.visibility = GONE
                binding.wikitextButtonHint.visibility = GONE
                binding.wikitextButtonImage.visibility = VISIBLE
                binding.wikitextButtonImage.setImageResource(drawableId)
            } else {
                binding.wikitextButtonText.visibility = VISIBLE
                binding.wikitextButtonHint.visibility = VISIBLE
                binding.wikitextButtonImage.visibility = GONE
                if (!TextUtils.isEmpty(buttonText)) {
                    binding.wikitextButtonText.text = buttonText
                }
                binding.wikitextButtonText.setTextColor(buttonTextColor)
                if (!buttonHint.isNullOrEmpty()) {
                    binding.wikitextButtonHint.text = buttonHint
                }
            }
            if (!buttonHint.isNullOrEmpty()) {
                contentDescription = buttonHint
                setButtonLongPressToast(this)
            }
            array.recycle()
        }
        isClickable = true
        isFocusable = true
        background = AppCompatResources.getDrawable(context,
                getThemedAttributeId(context, android.R.attr.selectableItemBackground))
    }
}
