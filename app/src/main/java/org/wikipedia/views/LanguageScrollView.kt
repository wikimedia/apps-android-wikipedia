package org.wikipedia.views

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.android.synthetic.main.view_language_scroll.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.ResourceUtil

class LanguageScrollView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    interface Callback {
        fun onLanguageTabSelected(selectedLanguageCode: String?)
        fun onLanguageButtonClicked()
    }

    private var callback: Callback? = null
    private val languageCodes: MutableList<String> = mutableListOf()

    init {
        inflate(context, R.layout.view_language_scroll, this)

        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        horizontal_scroll_languages.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = updateTabView(true, tab)
            override fun onTabUnselected(tab: TabLayout.Tab) = updateTabView(false, tab)
            override fun onTabReselected(tab: TabLayout.Tab) = updateTabView(true, tab)
        })

        more_languages.setOnClickListener { callback?.onLanguageButtonClicked() }
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

    fun setUpLanguageScrollTabData(languageCodes: List<String>, callback: Callback?, position: Int) {
        if (this.callback != null) this.callback = null

        if (horizontal_scroll_languages.childCount > 0) horizontal_scroll_languages.removeAllTabs()

        this.callback = callback
        this.languageCodes.clear()
        this.languageCodes.addAll(languageCodes)

        languageCodes.forEach {
            val tab = horizontal_scroll_languages.newTab()
            tab.customView = createLanguageTab(it)
            horizontal_scroll_languages.addTab(tab)
            updateTabView(false, tab)
        }

        selectLanguageTab(position)
    }

    private fun createLanguageTab(languageCode: String): View {
        val tab = LayoutInflater.from(context).inflate(R.layout.view_custom_language_tab, null) as LinearLayout
        updateTabLanguageCode(tab, languageCode, null, null, null)
        updateTabLanguageLabel(tab, languageCode, null)
        return tab
    }

    private fun updateTabLanguageLabel(customView: View, languageCode: String?, @ColorInt textColor: Int?) {
        val languageLabelTextView = customView.findViewById<TextView>(R.id.language_label)

        languageCode
                ?.let { WikipediaApp.getInstance().language().getAppLanguageLocalizedName(it) }
                ?.let { languageLabelTextView.text = it }

        textColor?.let { languageLabelTextView.setTextColor(it) }
    }

    private fun updateTabLanguageCode(customView: View, languageCode: String?, @ColorInt textColor: Int?, background: Drawable?, @ColorInt backgroundColorTint: Int?) {
        val languageCodeTextView = customView.findViewById<TextView>(R.id.language_code)

        languageCode?.let {
            languageCodeTextView.text = it
            ViewUtil.formatLangButton(languageCodeTextView, it, SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        }
        textColor?.let { languageCodeTextView.setTextColor(it) }
        background?.let { languageCodeTextView.background = it }
        backgroundColorTint?.let { languageCodeTextView.background.setColorFilter(it, PorterDuff.Mode.SRC_IN) }
    }

    fun selectLanguageTab(position: Int) = horizontal_scroll_languages?.getTabAt(position)?.select()
}
