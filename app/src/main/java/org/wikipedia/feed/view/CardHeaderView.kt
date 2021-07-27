package org.wikipedia.feed.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewCardHeaderBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.util.L10nUtil

class CardHeaderView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    interface Callback {
        fun onRequestDismissCard(card: Card): Boolean
        fun onRequestEditCardLanguages(card: Card)
        fun onRequestCustomize(card: Card)
    }

    private val binding = ViewCardHeaderBinding.inflate(LayoutInflater.from(context), this)

    private var card: Card? = null
    private var callback: Callback? = null
    var titleView = binding.viewCardHeaderTitle
        private set

    init {
        binding.viewListCardHeaderMenu.setOnClickListener { showOverflowMenu(it) }
    }

    private fun showOverflowMenu(anchorView: View) {
        card?.let {
            val menu = PopupMenu(anchorView.context, anchorView, Gravity.END)
            menu.menuInflater.inflate(R.menu.menu_feed_card_header, menu.menu)
            val editCardLangItem = menu.menu.findItem(R.id.menu_feed_card_edit_card_languages)
            editCardLangItem.isVisible = it.type().contentType()?.run { isPerLanguage } ?: false
            menu.setOnMenuItemClickListener(CardHeaderMenuClickListener())
            menu.show()
        }
    }

    fun setCard(card: Card): CardHeaderView {
        this.card = card
        return this
    }

    fun setCallback(callback: Callback?): CardHeaderView {
        this.callback = callback
        return this
    }

    fun setTitle(title: CharSequence?): CardHeaderView {
        binding.viewCardHeaderTitle.text = title
        return this
    }

    fun setTitle(@StringRes id: Int): CardHeaderView {
        binding.viewCardHeaderTitle.setText(id)
        return this
    }

    fun setLangCode(langCode: String?): CardHeaderView {
        if (langCode.isNullOrEmpty() || WikipediaApp.instance.appLanguageState.appLanguageCodes.size < 2) {
            binding.viewListCardHeaderLangBackground.visibility = View.GONE
            binding.viewListCardHeaderLangCode.visibility = View.GONE
            L10nUtil.setConditionalLayoutDirection(this, WikipediaApp.instance.appLanguageState.systemLanguageCode)
        } else {
            binding.viewListCardHeaderLangBackground.visibility = VISIBLE
            binding.viewListCardHeaderLangCode.visibility = VISIBLE
            binding.viewListCardHeaderLangCode.text = langCode
            L10nUtil.setConditionalLayoutDirection(this, langCode)
        }
        return this
    }

    private inner class CardHeaderMenuClickListener : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            return card?.let {
                when (item.itemId) {
                    R.id.menu_feed_card_dismiss -> {
                        callback?.onRequestDismissCard(it)
                        true
                    }
                    R.id.menu_feed_card_edit_card_languages -> {
                        callback?.onRequestEditCardLanguages(it)
                        true
                    }
                    R.id.menu_feed_card_customize -> {
                        callback?.onRequestCustomize(it)
                        true
                    }
                    else -> false
                }
            } ?: run { false }
        }
    }
}
