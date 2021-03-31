package org.wikipedia.onboarding

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewOnboardingPageBinding
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
    private val binding = ViewOnboardingPageBinding.inflate(LayoutInflater.from(context), this)
    private var listDataType: String? = null
    private var viewHeightDetected: Boolean = false

    init {
        attrs?.let { attrSet ->
            context.withStyledAttributes(attrSet, R.styleable.OnboardingPageView) {
                val centeredImage = AppCompatResources.getDrawable(context,
                        getResourceId(R.styleable.OnboardingPageView_centeredImage, -1))
                val primaryText = getString(R.styleable.OnboardingPageView_primaryText)
                val secondaryText = getString(R.styleable.OnboardingPageView_secondaryText)
                val tertiaryText = getString(R.styleable.OnboardingPageView_tertiaryText)
                val switchText = getString(R.styleable.OnboardingPageView_switchText)
                listDataType = getString(R.styleable.OnboardingPageView_dataType)
                val showListView = getBoolean(R.styleable.OnboardingPageView_showListView, false)
                val background = getDrawable(R.styleable.OnboardingPageView_background)
                val imageSize = getDimension(R.styleable.OnboardingPageView_imageSize, 0f)
                background?.let { setBackground(it) }
                binding.imageViewCentered.setImageDrawable(centeredImage)
                if (imageSize > 0 && centeredImage != null && centeredImage.intrinsicHeight > 0) {
                    val aspect = centeredImage.intrinsicWidth.toFloat() / centeredImage.intrinsicHeight
                    binding.imageViewCentered.updateLayoutParams {
                        width = imageSize.toInt()
                        height = (imageSize / aspect).toInt()
                    }
                }
                binding.primaryTextView.visibility = if (primaryText.isNullOrEmpty()) GONE else VISIBLE
                binding.primaryTextView.text = primaryText
                binding.secondaryTextView.text = StringUtil.fromHtml(secondaryText)
                binding.tertiaryTextView.text = tertiaryText
                binding.switchContainer.visibility = if (TextUtils.isEmpty(switchText)) View.GONE else View.VISIBLE
                binding.switchView.text = switchText
                setUpLanguageListContainer(showListView, listDataType)
                binding.secondaryTextView.movementMethod = LinkMovementMethodExt { url: String ->
                    callback?.onLinkClick(this@OnboardingPageView, url)
                }
                binding.languageListContainer.addLangContainer.setOnClickListener {
                    callback?.onListActionButtonClicked(this@OnboardingPageView)
                }
                binding.switchView.setOnCheckedChangeListener { _, checked ->
                    callback?.onSwitchChange(this@OnboardingPageView, checked)
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!viewHeightDetected) {
            if (binding.scrollView != null && binding.scrollViewContainer != null &&
                    binding.scrollView.height <= binding.scrollViewContainer.height) {
                // Remove layout gravity of the text below on small screens to make centered image visible
                removeScrollViewContainerGravity()
            }
            viewHeightDetected = true
        }
    }

    private fun removeScrollViewContainerGravity() {
        val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.NO_GRAVITY
        binding.scrollViewContainer?.layoutParams = params
    }

    fun setSwitchChecked(checked: Boolean) {
        binding.switchView.isChecked = checked
    }

    private fun setUpLanguageListContainer(showListView: Boolean, dataType: String?) {
        if (!showListView) {
            return
        }
        binding.tertiaryTextView.visibility = View.GONE
        binding.languageListContainer.root.visibility = View.VISIBLE
        binding.languageListContainer.languagesList.layoutManager = LinearLayoutManager(context)
        binding.languageListContainer.languagesList.adapter = LanguageListAdapter(getListData(dataType))
    }

    private fun getListData(dataType: String?): List<String?> {
        val items = mutableListOf<String>()
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
            holder.optionLabelTextView.textDirection = if (ViewCompat.LAYOUT_DIRECTION_LTR == ViewCompat.getLayoutDirection(binding.primaryTextView)) View.TEXT_DIRECTION_LTR else View.TEXT_DIRECTION_RTL
            holder.optionLabelTextView.text = context.getString(R.string.onboarding_option_string, (position + 1).toString(), items[position])
        }

        override fun getItemCount() = items.size

        inner class OptionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var optionLabelTextView = itemView.findViewById<TextView>(R.id.option_label)!!
        }
    }

    fun refreshLanguageList() {
        binding.languageListContainer.languagesList.adapter = LanguageListAdapter(getListData(listDataType))
        binding.languageListContainer.languagesList.adapter?.notifyDataSetChanged()
    }
}
