package org.wikipedia.settings.languages

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ItemWikipediaLanguageBinding
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.DeviceUtil.setContextClickAsLongClick
import org.wikipedia.util.ResourceUtil.getThemedAttributeId
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.views.ViewUtil.formatLangButton
import java.util.*

class WikipediaLanguagesItemView : LinearLayout {
    interface Callback {
        fun onCheckedChanged(position: Int)
        fun onLongPress(position: Int)
    }

    private var binding = ItemWikipediaLanguageBinding.inflate(LayoutInflater.from(context), this)
    private var position = 0
    var callback: Callback? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundColor(getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context,
                    getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        binding.wikiLanguageCheckbox.setOnCheckedChangeListener { _, _ ->
            callback?.onCheckedChanged(position)
            updateBackgroundColor()
        }
        setOnLongClickListener {
            callback?.onLongPress(position)
            true
        }
        setContextClickAsLongClick(this)
    }

    private fun updateBackgroundColor() {
        setBackgroundColor(if (binding.wikiLanguageCheckbox.isChecked) getThemedColor(context, R.attr.multi_select_background_color) else getThemedColor(context, R.attr.paper_color))
    }

    fun setContents(langCode: String, languageLocalizedName: String?, position: Int) {
        this.position = position
        binding.wikiLanguageOrder.text = (position + 1).toString()
        binding.wikiLanguageTitle.text = languageLocalizedName.orEmpty().capitalize(Locale.getDefault())
        binding.wikiLanguageCode.text = langCode
        binding.wikiLanguageCode.setTextColor(getThemedColor(context, R.attr.material_theme_de_emphasised_color))
        binding.wikiLanguageCode.background.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(getThemedColor(context, R.attr.material_theme_de_emphasised_color), BlendModeCompat.SRC_IN)
        formatLangButton(binding.wikiLanguageCode, langCode, SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
    }

    fun setCheckBoxEnabled(enabled: Boolean) {
        binding.wikiLanguageOrder.isGone = enabled
        binding.wikiLanguageCheckbox.isVisible = enabled
        if (!enabled) {
            binding.wikiLanguageCheckbox.isChecked = false
            setBackgroundColor(getThemedColor(context, R.attr.paper_color))
        }
    }

    fun setCheckBoxChecked(checked: Boolean) {
        binding.wikiLanguageCheckbox.isChecked = checked
        updateBackgroundColor()
    }

    fun setDragHandleEnabled(enabled: Boolean) {
        binding.wikiLanguageDragHandle.isVisible = enabled
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.wikiLanguageDragHandle.setOnTouchListener(listener)
    }
}
