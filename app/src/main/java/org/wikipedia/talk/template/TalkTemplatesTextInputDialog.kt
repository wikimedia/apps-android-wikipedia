package org.wikipedia.talk.template

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.databinding.DialogTalkTemplatesTextInputBinding

class TalkTemplatesTextInputDialog constructor(context: Context,
                                               positiveButtonText: Int = R.string.text_input_dialog_ok_button_text,
                                               negativeButtonText: Int = R.string.text_input_dialog_cancel_button_text) : MaterialAlertDialogBuilder(context) {
    interface Callback {
        fun onShow(dialog: TalkTemplatesTextInputDialog)
        fun onTextChanged(text: CharSequence, dialog: TalkTemplatesTextInputDialog)
        fun onSuccess(text: CharSequence, secondaryText: CharSequence, tertiaryText: CharSequence)
        fun onCancel()
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
    }

    override fun create(): AlertDialog {
        dialog = super.create()
        dialog?.setOnShowListener {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            callback?.onShow(this@TalkTemplatesTextInputDialog)
        }
        return dialog!!
    }

    fun setText(text: CharSequence?, select: Boolean) {
        binding.titleInput.setText(text)
        if (select) {
            binding.titleInput.selectAll()
        }
    }

    fun setSecondaryText(text: CharSequence?) {
        binding.subjectTextInput.setText(text)
    }

    fun setTertiaryText(text: CharSequence?) {
        binding.bodyTextInput.editText.setText(text)
    }

    fun showSecondaryText(show: Boolean): TalkTemplatesTextInputDialog {
        binding.subjectTextInputContainer.visibility = if (show) View.VISIBLE else View.GONE
        return this
    }

    fun showTertiaryText(show: Boolean): TalkTemplatesTextInputDialog {
        binding.bodyTextInput.visibility = if (show) View.VISIBLE else View.GONE
        return this
    }

    fun setHint(@StringRes id: Int) {
        binding.titleInputContainer.hint = context.resources.getString(id)
    }

    fun setSecondaryHint(@StringRes id: Int) {
        binding.subjectTextInputContainer.hint = context.resources.getString(id)
    }

    fun setTertiaryHint(@StringRes id: Int) {
        binding.bodyTextInput.textInputLayout.hint = context.resources.getString(id)
    }

    fun setError(text: CharSequence?) {
        binding.titleInput.error = text
    }

    fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }
}
