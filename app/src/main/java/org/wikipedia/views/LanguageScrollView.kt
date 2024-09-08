package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewLanguageScrollBinding
import org.wikipedia.util.ResourceUtil

class LanguageScrollView(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    interface Callback {
        fun onLanguageTabSelected(selectedLanguageCode: String)
        fun onLanguageButtonClicked()
    }

    private val binding = ViewLanguageScrollBinding.inflate(LayoutInflater.from(context), this)
    private var callback: Callback? = null
    private var languageCodes: List<String> = mutableListOf()
    private var allowSelect = false
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
                @ColorInt val color = ResourceUtil.getThemedColor(context, R.attr.progressive_color)
                @ColorInt val paperColor = ResourceUtil.getThemedColor(context, R.attr.paper_color)
                updateTabLanguageCode(it, textColor = paperColor, backgroundColorTint = color, fillBackground = true)
                updateTabLanguageLabel(it, textColor = color)
            }
            if (allowSelect) {
                callback?.onLanguageTabSelected(languageCodes[tab.position])
            }
        } else {
            view?.let {
                @ColorInt val color = ResourceUtil.getThemedColor(context, R.attr.placeholder_color)
                updateTabLanguageLabel(it, textColor = color)
                updateTabLanguageCode(it, textColor = color, backgroundColorTint = color)
            }
        }
    }

    fun setUpLanguageScrollTabData(languageCodes: List<String>, position: Int, callback: Callback?) {
        this.callback = callback
        this.languageCodes = languageCodes
        allowSelect = false
        if (binding.horizontalScrollLanguages.childCount > 0) {
            binding.horizontalScrollLanguages.removeAllTabs()
        }
        languageCodes.forEach {
            val tab = binding.horizontalScrollLanguages.newTab()
            tab.customView = createLanguageTab(it)
            binding.horizontalScrollLanguages.addTab(tab)
            updateTabView(false, tab)
        }
        allowSelect = true
        binding.horizontalScrollLanguages.post {
            if (isAttachedToWindow) {
                binding.horizontalScrollLanguages.getTabAt(position)?.select()
            }
        }
    }

    private fun createLanguageTab(languageCode: String): View {
        val tab = LayoutInflater.from(context).inflate(R.layout.view_custom_language_tab, this, false)
        updateTabLanguageCode(tab, languageCode)
        updateTabLanguageLabel(tab, languageCode)
        return tab
    }

    private fun updateTabLanguageLabel(customView: View, languageCode: String? = null, @ColorInt textColor: Int? = null) {
        val languageLabelTextView = customView.findViewById<TextView>(R.id.language_label)
        if (!languageCode.isNullOrEmpty()) {
            languageLabelTextView.text = WikipediaApp.instance.languageState.getAppLanguageCanonicalName(languageCode)
        }
        textColor?.let {
            languageLabelTextView.setTextColor(textColor)
        }
    }

    private fun updateTabLanguageCode(customView: View, languageCode: String? = null, @ColorInt textColor: Int? = null, @ColorInt backgroundColorTint: Int? = null, fillBackground: Boolean = false) {
        val languageCodeTextView = customView.findViewById<LangCodeView>(R.id.language_code)
        if (languageCode != null) {
            languageCodeTextView.setLangCode(languageCode)
        }
        languageCodeTextView.fillBackground(fillBackground)
        textColor?.let { languageCodeTextView.setTextColor(it) }
        backgroundColorTint?.let { languageCodeTextView.setBackgroundTint(it) }
    }
}
