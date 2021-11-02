package org.wikipedia.views

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
import org.wikipedia.R
import org.wikipedia.databinding.ViewReadingListsOverflowBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil.getLastSyncDateString
import java.text.ParseException

class ReadingListsOverflowView(context: Context) : FrameLayout(context) {
    interface Callback {
        fun sortByClick()
        fun createNewListClick()
        fun refreshClick()
    }

    private var binding = ViewReadingListsOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null
    private var popupWindowHost: PopupWindow? = null

    init {
        binding.readingListsOverflowSortBy.setOnClickListener {
            dismissPopupWindowHost()
            callback?.sortByClick()
        }
        binding.readingListsOverflowCreateNewList.setOnClickListener {
            dismissPopupWindowHost()
            callback?.createNewListClick()
        }
        binding.readingListsOverflowRefresh.setOnClickListener {
            dismissPopupWindowHost()
            callback?.refreshClick()
        }
    }

    private fun dismissPopupWindowHost() {
        popupWindowHost?.dismiss()
        popupWindowHost = null
    }

    fun show(anchorView: View, callback: Callback) {
        this.callback = callback

        PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true).let {
            popupWindowHost = it
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }

        Prefs.readingListsLastSyncTime.let {
            binding.readingListsOverflowLastSync.visibility = if (it.isNullOrEmpty()) GONE else VISIBLE
            if (!it.isNullOrEmpty()) {
                try {
                    binding.readingListsOverflowLastSync.text = context.getString(R.string.reading_list_menu_last_sync,
                            getLastSyncDateString(it))
                } catch (e: ParseException) {
                    // ignore
                }
            }
        }
    }
}
