package org.wikipedia.edit

import android.content.Intent
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.wikipedia.Constants
import org.wikipedia.edit.insertmedia.InsertMediaActivity
import org.wikipedia.edit.templates.TemplatesSearchActivity
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
    val pageTitle: PageTitle,
    private val rootView: View,
    val editText: SyntaxHighlightableEditText,
    private val wikiTextKeyboardView: WikiTextKeyboardView,
    private val wikiTextKeyboardFormattingView: WikiTextKeyboardFormattingView,
    private val wikiTextKeyboardHeadingsView: WikiTextKeyboardHeadingsView,
    private val invokeSource: Constants.InvokeSource,
    private val requestInsertMedia: ActivityResultLauncher<Intent>,
    private val requestInsertTemplate: ActivityResultLauncher<Intent>,
    showUserMention: Boolean = false
) : WikiTextKeyboardView.Callback {

    init {
        wikiTextKeyboardView.editText = editText
        wikiTextKeyboardView.callback = this
        wikiTextKeyboardFormattingView.editText = editText
        wikiTextKeyboardFormattingView.callback = this
        wikiTextKeyboardHeadingsView.editText = editText
        wikiTextKeyboardHeadingsView.callback = this
        wikiTextKeyboardView.userMentionVisible = showUserMention
        hideAllSyntaxModals()

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
                wikiTextKeyboardView.insertLink(title, pageTitle.wikiSite.languageCode)
            }
        }
    }

    override fun onPreviewLink(title: String) {
        val dialog = LinkPreviewDialog.newInstance(HistoryEntry(PageTitle(title, pageTitle.wikiSite), HistoryEntry.SOURCE_INTERNAL_LINK))
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
        requestInsertMedia.launch(InsertMediaActivity.newIntent(activity, pageTitle.wikiSite,
            if (invokeSource == Constants.InvokeSource.EDIT_ACTIVITY) pageTitle.displayText else "",
            invokeSource))
    }

    override fun onRequestInsertTemplate() {
        requestInsertTemplate.launch(TemplatesSearchActivity.newIntent(activity, pageTitle.wikiSite, invokeSource))
    }

    override fun onRequestInsertLink() {
        requestLinkFromSearch.launch(SearchActivity.newIntent(activity, invokeSource, null, true))
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
