package org.wikipedia.edit

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.edit.insertmedia.InsertMediaActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.search.SearchActivity
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil

class SyntaxHighlightViewAdapter(
    val activity: AppCompatActivity,
    val wikiSite: WikiSite,
    private val rootView: View,
    val editText: SyntaxHighlightableEditText,
    private val wikiTextKeyboardView: WikiTextKeyboardView,
    private val wikiTextKeyboardFormattingView: WikiTextKeyboardFormattingView,
    private val wikiTextKeyboardHeadingsView: WikiTextKeyboardHeadingsView
) : WikiTextKeyboardView.Callback {

    init {
        wikiTextKeyboardView.editText = editText
        wikiTextKeyboardView.callback = this
        wikiTextKeyboardFormattingView.editText = editText
        wikiTextKeyboardFormattingView.callback = this
        wikiTextKeyboardHeadingsView.editText = editText
        wikiTextKeyboardHeadingsView.callback = this

        activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            activity.window.decorView.post {
                if (!activity.isDestroyed) {
                    showOrHideSyntax(editText.hasFocus())
                }
            }
        }

        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            showOrHideSyntax(hasFocus)
        }
    }

    private val requestLinkFromSearch = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SearchActivity.RESULT_LINK_SUCCESS) {
            it.data?.parcelableExtra<PageTitle>(SearchActivity.EXTRA_RETURN_LINK_TITLE)?.let { title ->
                wikiTextKeyboardView.insertLink(title, wikiSite.languageCode)
            }
        }
    }

    private val requestInsertMedia = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == InsertMediaActivity.RESULT_INSERT_MEDIA_SUCCESS) {
            it.data?.let { intent ->
                editText.inputConnection?.commitText("${intent.getStringExtra(
                    InsertMediaActivity.RESULT_WIKITEXT)}", 1)
            }
        }
    }

    override fun onPreviewLink(title: String) {
        val dialog = LinkPreviewDialog.newInstance(HistoryEntry(PageTitle(title, wikiSite), HistoryEntry.SOURCE_INTERNAL_LINK), null)
        ExclusiveBottomSheetPresenter.show(activity.supportFragmentManager, dialog)
        editText.post {
            dialog.dialog?.setOnDismissListener {
                if (!activity.isDestroyed) {
                    editText.postDelayed({
                        DeviceUtil.showSoftKeyboard(editText)
                    }, 200)
                }
            }
        }
    }

    override fun onRequestInsertMedia() {
        requestInsertMedia.launch(InsertMediaActivity.newIntent(activity, wikiSite, ""))
    }

    override fun onRequestInsertLink() {
        requestLinkFromSearch.launch(SearchActivity.newIntent(activity, Constants.InvokeSource.TALK_REPLY_ACTIVITY, null, true))
    }

    override fun onRequestHeading() {
        if (wikiTextKeyboardHeadingsView.isVisible) {
            hideAllSyntaxModals()
            return
        }
        hideAllSyntaxModals()
        wikiTextKeyboardHeadingsView.isVisible = true
        wikiTextKeyboardView.onAfterHeadingsShown()
    }

    override fun onRequestFormatting() {
        if (wikiTextKeyboardFormattingView.isVisible) {
            hideAllSyntaxModals()
            return
        }
        hideAllSyntaxModals()
        wikiTextKeyboardFormattingView.isVisible = true
        wikiTextKeyboardView.onAfterFormattingShown()
    }

    override fun onSyntaxOverlayCollapse() {
        hideAllSyntaxModals()
    }

    private fun hideAllSyntaxModals() {
        wikiTextKeyboardHeadingsView.isVisible = false
        wikiTextKeyboardFormattingView.isVisible = false
        wikiTextKeyboardView.onAfterOverlaysHidden()
    }

    private fun showOrHideSyntax(hasFocus: Boolean) {
        val hasMinHeight = DeviceUtil.isHardKeyboardAttached(activity.resources) ||
                activity.window.decorView.height - rootView.height > DimenUtil.roundedDpToPx(150f)
        if (hasFocus && hasMinHeight) {
            wikiTextKeyboardView.isVisible = true
        } else {
            hideAllSyntaxModals()
            wikiTextKeyboardView.isVisible = false
        }
    }
}
