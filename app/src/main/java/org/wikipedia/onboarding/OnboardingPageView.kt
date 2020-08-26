package org.wikipedia.onboarding

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.view_onboarding_language_list.view.*
import kotlinx.android.synthetic.main.view_onboarding_page.view.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.onboarding.OnboardingPageView.LanguageListAdapter.OptionsViewHolder
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.StringUtil
import java.util.*

class OnboardingPageView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onSwitchChange(view: OnboardingPageView, checked: Boolean)
        fun onLinkClick(view: OnboardingPageView, url: String)
        fun onListActionButtonClicked(view: OnboardingPageView)
    }

    class DefaultCallback : Callback {
        override fun onSwitchChange(view: OnboardingPageView, checked: Boolean) {}
        override fun onLinkClick(view: OnboardingPageView, url: String) {}
        override fun onListActionButtonClicked(view: OnboardingPageView) {}
    }

    var callback: Callback? = null
    private var listDataType: String? = null

    init {
        View.inflate(context, R.layout.view_onboarding_page, this)
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.OnboardingPageView)
            val centeredImage = AppCompatResources.getDrawable(context,
                    array.getResourceId(R.styleable.OnboardingPageView_centeredImage, -1))
            val primaryText = array.getString(R.styleable.OnboardingPageView_primaryText)
            val secondaryText = array.getString(R.styleable.OnboardingPageView_secondaryText)
            val tertiaryText = array.getString(R.styleable.OnboardingPageView_tertiaryText)
            val switchText = array.getString(R.styleable.OnboardingPageView_switchText)
            listDataType = array.getString(R.styleable.OnboardingPageView_dataType)
            val showListView = array.getBoolean(R.styleable.OnboardingPageView_showListView, false)
            val background = array.getDrawable(R.styleable.OnboardingPageView_background)
            val imageSize = array.getDimension(R.styleable.OnboardingPageView_imageSize, 0f)
            background?.let { setBackground(it) }
            imageViewCentered.setImageDrawable(centeredImage)
            if (imageSize > 0 && centeredImage != null && centeredImage.intrinsicHeight > 0) {
                val aspect = centeredImage.intrinsicWidth.toFloat() / centeredImage.intrinsicHeight
                val params = imageViewCentered.layoutParams
                params.width = imageSize.toInt()
                params.height = (imageSize / aspect).toInt()
                imageViewCentered.layoutParams = params
            }
            primaryTextView.text = primaryText
            secondaryTextView.text = StringUtil.fromHtml(secondaryText)
            tertiaryTextView.text = tertiaryText
            switchContainer.visibility = if (TextUtils.isEmpty(switchText)) View.GONE else View.VISIBLE
            switchView.text = switchText
            setUpLanguageListContainer(showListView, listDataType)
            secondaryTextView.movementMethod = LinkMovementMethodExt { url: String ->
                if (callback != null) {
                    callback!!.onLinkClick(this@OnboardingPageView, url)
                }
            }
            addLangContainer.setOnClickListener {
                if (callback != null) {
                    callback!!.onListActionButtonClicked(this)
                }
            }
            switchView.setOnCheckedChangeListener { _, checked ->
                if (callback != null) {
                    callback!!.onSwitchChange(this, checked)
                }
            }
            array.recycle()
        }
    }

    fun setSwitchChecked(checked: Boolean) {
        switchView.isChecked = checked
    }

    private fun setUpLanguageListContainer(showListView: Boolean, dataType: String?) {
        if (!showListView) {
            return
        }
        tertiaryTextView.visibility = View.GONE
        languageListContainer.visibility = View.VISIBLE
        languagesList.layoutManager = LinearLayoutManager(context)
        languagesList.adapter = LanguageListAdapter(getListData(dataType))
    }

    private fun getListData(dataType: String?): List<String?> {
        val items: MutableList<String?> = ArrayList()
        if (dataType != null && dataType == context.getString(R.string.language_data)) {
            for (code in WikipediaApp.getInstance().language().appLanguageCodes) {
                items.add(StringUtils.capitalize(WikipediaApp.getInstance().language().getAppLanguageLocalizedName(code)))
            }
        }
        return items
    }

    inner class LanguageListAdapter internal constructor(private val items: List<String?>) : RecyclerView.Adapter<OptionsViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionsViewHolder {
            return OptionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_options_recycler, parent, false))
        }

        override fun onBindViewHolder(holder: OptionsViewHolder, position: Int) {
            holder.optionLabelTextView.textDirection = if (ViewCompat.LAYOUT_DIRECTION_LTR == ViewCompat.getLayoutDirection(primaryTextView)) View.TEXT_DIRECTION_LTR else View.TEXT_DIRECTION_RTL
            holder.optionLabelTextView.text = context.getString(R.string.onboarding_option_string, (position + 1).toString(), items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class OptionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var optionLabelTextView: TextView = itemView.findViewById(R.id.option_label)
        }
    }

    fun refreshLanguageList() {
        if (languagesList.adapter != null) {
            languagesList.adapter = null
            languagesList.adapter = LanguageListAdapter(getListData(listDataType))
            languagesList.adapter!!.notifyDataSetChanged()
        }
    }
}