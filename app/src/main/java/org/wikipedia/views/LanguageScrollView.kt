package org.wikipedia.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewLanguageScrollBinding
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.ResourceUtil

class LanguageScrollView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    interface Callback {
        fun onLanguageTabSelected(selectedLanguageCode: String)
        fun onLanguageButtonClicked()
    }

    private val binding = ViewLanguageScrollBinding.inflate(LayoutInflater.from(context), this)
    private var callback: Callback? = null
    private var languageCodes: List<String> = mutableListOf()
    val selectedPosition: Int
        get() = binding.horizontalScrollLanguages.selectedTabPosition

    init {
        binding.horizontalScrollLanguages.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabView(true, tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabView(false, tab)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                updateTabView(true, tab)
            }
        })
        binding.moreLanguages.setOnClickListener {
            callback?.onLanguageButtonClicked()
        }
    }

    private fun updateTabView(selected: Boolean, tab: TabLayout.Tab) {
        val view = tab.customView
        if (selected) {
            view?.let {
                @ColorInt val color = ResourceUtil.getThemedColor(context, R.attr.colorAccent)
                @ColorInt val paperColor = ResourceUtil.getThemedColor(context, R.attr.paper_color)
                val drawable = AppCompatResources.getDrawable(context, R.drawable.lang_button_shape)
                updateTabLanguageCode(it, null, paperColor, drawable, color)
                updateTabLanguageLabel(it, null, color)
            }
            callback?.onLanguageTabSelected(languageCodes[tab.position])
        } else {
            view?.let {
                @ColorInt val color = ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color)
                updateTabLanguageLabel(it, null, color)
                updateTabLanguageCode(it, null, color, AppCompatResources.getDrawable(context, R.drawable.lang_button_shape_border), color)
            }
        }
    }

    fun setUpLanguageScrollTabData(languageCodes: List<String>, position: Int, callback: Callback?) {
        this.callback = callback
        this.languageCodes = languageCodes
        if (binding.horizontalScrollLanguages.childCount > 0) {
            binding.horizontalScrollLanguages.removeAllTabs()
        }
        languageCodes.forEach {
            val tab = binding.horizontalScrollLanguages.newTab()
            tab.customView = createLanguageTab(it)
            binding.horizontalScrollLanguages.addTab(tab)
            updateTabView(false, tab)
        }
        binding.horizontalScrollLanguages.getTabAt(position)?.let {
            binding.horizontalScrollLanguages.post {
                if (!isAttachedToWindow) {
                    return@post
                }
                binding.horizontalScrollLanguages.getTabAt(position)!!.select()
            }
        }
    }

    private fun createLanguageTab(languageCode: String): View {
        val tab = LayoutInflater.from(context).inflate(R.layout.view_custom_language_tab, this, false)
        updateTabLanguageCode(tab, languageCode, null, null, null)
        updateTabLanguageLabel(tab, languageCode, null)
        return tab
    }

    private fun updateTabLanguageLabel(customView: View, languageCode: String?, @ColorInt textColor: Int?) {
        val languageLabelTextView = customView.findViewById<TextView>(R.id.language_label)
        if (!languageCode.isNullOrEmpty()) {
            languageLabelTextView.text = WikipediaApp.instance.appLanguageState.getAppLanguageLocalizedName(languageCode)
        }
        textColor?.let {
            languageLabelTextView.setTextColor(textColor)
        }
    }

    private fun updateTabLanguageCode(customView: View, languageCode: String?, @ColorInt textColor: Int?, background: Drawable?, @ColorInt backgroundColorTint: Int?) {
        val languageCodeTextView = customView.findViewById<TextView>(R.id.language_code)
        if (languageCode != null) {
            languageCodeTextView.text = languageCode
            ViewUtil.formatLangButton(languageCodeTextView, languageCode, SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        }
        textColor?.let { languageCodeTextView.setTextColor(it) }
        background?.let { languageCodeTextView.background = it }
        backgroundColorTint?.let {
            languageCodeTextView.background.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(it, BlendModeCompat.SRC_IN)
        }
    }
}
