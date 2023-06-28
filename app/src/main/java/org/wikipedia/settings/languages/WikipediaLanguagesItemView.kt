package org.wikipedia.settings.languages

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ItemWikipediaLanguageBinding
import org.wikipedia.language.LanguageUtil
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
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
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context,
                ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackground))
        }
        binding.wikiLanguageCheckbox.setOnCheckedChangeListener { _, _ ->
            callback?.onCheckedChanged(position)
            updateBackgroundColor()
        }
        setOnLongClickListener {
            callback?.onLongPress(position)
            true
        }
        DeviceUtil.setContextClickAsLongClick(this)
    }

    private fun updateBackgroundColor() {
        setBackgroundColor(if (binding.wikiLanguageCheckbox.isChecked) ResourceUtil.getThemedColor(context, R.attr.background_color)
        else ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }

    fun setContents(langCode: String, languageLocalizedName: String?, position: Int) {
        this.position = position
        binding.wikiLanguageOrder.text = (position + 1).toString()
        binding.wikiLanguageTitle.text = StringUtil.capitalize(languageLocalizedName.orEmpty())
        binding.wikiLanguageCode.text = LanguageUtil.formatLangCodeForButton(langCode)
        val color = ResourceUtil.getThemedColorStateList(context, R.attr.secondary_color)
        binding.wikiLanguageCode.setTextColor(color)
        ViewCompat.setBackgroundTintList(binding.wikiLanguageCode, color)
        ViewUtil.formatLangButton(binding.wikiLanguageCode, langCode, SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
    }

    fun setCheckBoxEnabled(enabled: Boolean) {
        binding.wikiLanguageOrder.visibility = if (enabled) GONE else VISIBLE
        binding.wikiLanguageCheckbox.visibility = if (enabled) VISIBLE else GONE
        if (!enabled) {
            binding.wikiLanguageCheckbox.isChecked = false
            setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        }
    }

    fun setCheckBoxChecked(checked: Boolean) {
        binding.wikiLanguageCheckbox.isChecked = checked
        updateBackgroundColor()
    }

    fun setDragHandleEnabled(enabled: Boolean) {
        binding.wikiLanguageDragHandle.visibility = if (enabled) VISIBLE else GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.wikiLanguageDragHandle.setOnTouchListener(listener)
    }
}
