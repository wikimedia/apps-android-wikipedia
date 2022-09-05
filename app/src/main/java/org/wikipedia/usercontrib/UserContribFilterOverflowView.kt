package org.wikipedia.usercontrib

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.core.widget.PopupWindowCompat
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewUserContribFilterOverflowBinding
import org.wikipedia.page.Namespace
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData

class UserContribFilterOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun onItemClicked()
    }

    private var binding = ViewUserContribFilterOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null
    private var popupWindowHost: PopupWindow? = null

    init {
        setButtonsListener()
    }

    fun show(anchorView: View, callback: Callback?) {
        this.callback = callback

        binding.nsUserText.text = UserAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode)
        binding.nsTalkText.text = TalkAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode)
        binding.nsUserTalkText.text = UserTalkAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode)
        updateSelectedItem()

        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }
        popupWindowHost?.setOnDismissListener {
            popupWindowHost = null
        }
    }

    private fun updateSelectedItem() {
        binding.nsNoneCheckIcon.visibility = View.INVISIBLE
        binding.nsArticleCheckIcon.visibility = View.INVISIBLE
        binding.nsTalkCheckIcon.visibility = View.INVISIBLE
        binding.nsUserCheckIcon.visibility = View.INVISIBLE
        binding.nsUserTalkCheckIcon.visibility = View.INVISIBLE

        when (Prefs.userContribFilterNs) {
            Namespace.MAIN.code() -> { binding.nsArticleCheckIcon.visibility = View.VISIBLE }
            Namespace.TALK.code() -> { binding.nsTalkCheckIcon.visibility = View.VISIBLE }
            Namespace.USER.code() -> { binding.nsUserCheckIcon.visibility = View.VISIBLE }
            Namespace.USER_TALK.code() -> { binding.nsUserTalkCheckIcon.visibility = View.VISIBLE }
            else -> { binding.nsNoneCheckIcon.visibility = View.VISIBLE }
        }
    }

    private fun setButtonsListener() {
        binding.nsNoneButton.setOnClickListener {
            Prefs.userContribFilterNs = -1
            onSelected()
        }
        binding.nsArticleButton.setOnClickListener {
            Prefs.userContribFilterNs = Namespace.MAIN.code()
            onSelected()
        }
        binding.nsTalkButton.setOnClickListener {
            Prefs.userContribFilterNs = Namespace.TALK.code()
            onSelected()
        }
        binding.nsUserButton.setOnClickListener {
            Prefs.userContribFilterNs = Namespace.USER.code()
            onSelected()
        }
        binding.nsUserTalkButton.setOnClickListener {
            Prefs.userContribFilterNs = Namespace.USER_TALK.code()
            onSelected()
        }
    }

    private fun onSelected() {
        callback?.onItemClicked()
        popupWindowHost?.dismiss()
    }
}
