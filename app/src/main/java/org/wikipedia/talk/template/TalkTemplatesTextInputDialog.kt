package org.wikipedia.talk.template

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.databinding.DialogTalkTemplatesTextInputBinding

class TalkTemplatesTextInputDialog constructor(context: Context,
                                               positiveButtonText: Int = R.string.text_input_dialog_ok_button_text,
                                               negativeButtonText: Int = R.string.text_input_dialog_cancel_button_text) : MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Input) {
    interface Callback {
        fun onShow(dialog: TalkTemplatesTextInputDialog)
        fun onTextChanged(text: CharSequence, dialog: TalkTemplatesTextInputDialog)
        fun onSuccess(titleText: CharSequence, subjectText: CharSequence, bodyText: CharSequence)
        fun onCancel()
        fun onDismiss()
    }

    private var binding = DialogTalkTemplatesTextInputBinding.inflate(LayoutInflater.from(context))
    private var dialog: AlertDialog? = null
    var callback: Callback? = null

    init {
        setView(binding.root)
        binding.titleInputContainer.isErrorEnabled = true
        setPositiveButton(positiveButtonText) { _, _ ->
            callback?.onSuccess(binding.titleInput.text.toString(), binding.subjectTextInput.text.toString(), binding.bodyTextInput.editText.text.toString())
        }
        setNegativeButton(negativeButtonText) { _, _ ->
            callback?.onCancel()
        }
        binding.titleInput.doOnTextChanged { text, _, _, _ ->
            callback?.onTextChanged(text ?: "", this)
        }
        setOnDismissListener {
            callback?.onDismiss()
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
}
