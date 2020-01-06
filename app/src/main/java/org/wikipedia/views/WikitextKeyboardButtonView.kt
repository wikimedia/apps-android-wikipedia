package org.wikipedia.views

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import kotlinx.android.synthetic.main.view_wikitext_keyboard_button.view.*
import org.wikipedia.R
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class WikitextKeyboardButtonView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_wikitext_keyboard_button, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs,
                    R.styleable.WikitextKeyboardButtonView, defStyleAttr, 0)
            val drawableId = array.getResourceId(R.styleable.WikitextKeyboardButtonView_buttonImage, 0)
            val buttonText = array.getString(R.styleable.WikitextKeyboardButtonView_buttonText)
            val buttonHint = array.getString(R.styleable.WikitextKeyboardButtonView_buttonHint)
            val buttonTextColor = array.getColor(R.styleable.WikitextKeyboardButtonView_buttonTextColor,
                    ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color))
            if (drawableId != 0) {
                wikitext_button_text.visibility = View.GONE
                wikitext_button_hint.visibility = View.GONE
                wikitext_button_image.visibility = View.VISIBLE
                wikitext_button_image.setImageResource(drawableId)
            } else {
                wikitext_button_text.visibility = View.VISIBLE
                wikitext_button_hint.visibility = View.VISIBLE
                wikitext_button_image.visibility = View.GONE
                if (!TextUtils.isEmpty(buttonText)) {
                    wikitext_button_text.text = buttonText
                }
                wikitext_button_text.setTextColor(buttonTextColor)
                if (!TextUtils.isEmpty(buttonHint)) {
                    wikitext_button_hint.text = buttonHint
                }
            }
            if (!TextUtils.isEmpty(buttonHint)) {
                contentDescription = buttonHint
                FeedbackUtil.setToolbarButtonLongPressToast(this)
            }
            array.recycle()
        }

        isClickable = true
        isFocusable = true
        background = AppCompatResources.getDrawable(context,
                ResourceUtil.getThemedAttributeId(context, android.R.attr.selectableItemBackground))
    }
}
