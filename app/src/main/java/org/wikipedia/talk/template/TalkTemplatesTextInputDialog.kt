package org.wikipedia.talk.template

import android.content.Intent
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.DialogTalkTemplatesTextInputBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.edit.SyntaxHighlightViewAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.views.UserMentionInputView

class TalkTemplatesTextInputDialog constructor(private val activity: AppCompatActivity,
                                               positiveButtonText: Int = R.string.text_input_dialog_ok_button_text,
                                               negativeButtonText: Int = R.string.text_input_dialog_cancel_button_text,
                                               requestInertMedia: ActivityResultLauncher<Intent>? = null)
    : MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_Input), LinkPreviewDialog.Callback, UserMentionInputView.Listener {
    interface Callback {
        fun onShow(dialog: TalkTemplatesTextInputDialog)
        fun onTextChanged(text: CharSequence, dialog: TalkTemplatesTextInputDialog)
        fun onSuccess(titleText: CharSequence, subjectText: CharSequence, bodyText: CharSequence)
        fun onCancel()
    }

    private var binding = DialogTalkTemplatesTextInputBinding.inflate(LayoutInflater.from(context))
    private var dialog: AlertDialog? = null
    var callback: Callback? = null
    val bodyTextInput get() = binding.bodyTextInput

    private var userMentionScrolled = false
    private val wikiSite = WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)

    init {
        setView(binding.root)
        binding.titleInputContainer.isErrorEnabled = true
        setPositiveButton(positiveButtonText) { _, _ ->
            callback?.onSuccess(binding.titleInput.text.toString(), binding.subjectTextInput.text.toString(), binding.bodyTextInput.editText.text.toString())
        }
        setNegativeButton(negativeButtonText) { _, _ ->
            callback?.onCancel()
        }

        binding.bodyTextInput.wikiSite = wikiSite
        binding.bodyTextInput.listener = this

        requestInertMedia?.let {
            SyntaxHighlightViewAdapter(activity, PageTitle("Main Page", wikiSite), binding.root, binding.bodyTextInput.editText,
                binding.editKeyboardOverlay, binding.editKeyboardOverlayFormatting, binding.editKeyboardOverlayHeadings,
                Constants.InvokeSource.TALK_REPLY_ACTIVITY, it, true)
        }

        binding.titleInput.doOnTextChanged { text, _, _, _ ->
            callback?.onTextChanged(text ?: "", this)
        }
    }

    override fun create(): AlertDialog {
        dialog = super.create()
        dialog?.setOnShowListener {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            callback?.onShow(this@TalkTemplatesTextInputDialog)
        }
        return dialog!!
    }

    fun setDialogMessage(text: String) {
        binding.dialogMessage.text = text
    }

    fun setTitleText(text: CharSequence?) {
        binding.titleInput.setText(text)
    }

    fun setSubjectText(text: CharSequence?) {
        binding.subjectTextInput.setText(text)
    }

    fun setBodyText(text: CharSequence?) {
        binding.bodyTextInput.editText.setText(text)
    }

    fun showDialogMessage(show: Boolean) {
        binding.dialogMessage.isVisible = show
    }

    fun showSubjectText(show: Boolean) {
        binding.subjectTextInputContainer.isVisible = show
    }

    fun showBodyText(show: Boolean) {
        binding.bodyTextInput.isVisible = show
    }

    fun setTitleHint(@StringRes id: Int) {
        binding.titleInputContainer.hint = context.resources.getString(id)
    }

    fun setSubjectHint(@StringRes id: Int) {
        binding.subjectTextInputContainer.hint = context.resources.getString(id)
    }

    fun setBodyHint(@StringRes id: Int) {
        binding.bodyTextInput.textInputLayout.hint = context.resources.getString(id)
    }

    fun setError(text: CharSequence?) {
        binding.titleInput.error = text
    }

    fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        activity.startActivity(if (inNewTab) PageActivity.newIntentForNewTab(activity, entry, title) else
            PageActivity.newIntentForCurrentTab(activity, entry, title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(activity, text = title.uri)
        FeedbackUtil.showMessage(activity, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        ExclusiveBottomSheetPresenter.show(activity.supportFragmentManager,
            AddToReadingListDialog.newInstance(title, Constants.InvokeSource.TALK_REPLY_ACTIVITY))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(activity, title)
    }

    override fun onUserMentionListUpdate() {
        binding.scrollContainer.post {
            if (dialog?.isShowing == true && !userMentionScrolled) {
                binding.scrollContainer.smoothScrollTo(0, binding.root.height * 4)
                userMentionScrolled = true
            }
        }
    }

    override fun onUserMentionComplete() {
        userMentionScrolled = false
    }
}
