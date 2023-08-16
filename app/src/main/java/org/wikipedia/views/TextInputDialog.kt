package org.wikipedia.views

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.databinding.DialogTextInputBinding

class TextInputDialog constructor(context: Context,
                                  positiveButtonText: Int = R.string.text_input_dialog_ok_button_text,
                                  negativeButtonText: Int = R.string.text_input_dialog_cancel_button_text) : MaterialAlertDialogBuilder(context) {
    interface Callback {
        fun onShow(dialog: TextInputDialog)
        fun onTextChanged(text: CharSequence, dialog: TextInputDialog)
        fun onSuccess(text: CharSequence, secondaryText: CharSequence, tertiaryText: CharSequence)
        fun onCancel()
    }

    private var binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
    private var dialog: AlertDialog? = null
    var callback: Callback? = null

    init {
        setView(binding.root)
        binding.textInputContainer.isErrorEnabled = true
        setPositiveButton(positiveButtonText) { _, _ ->
            callback?.onSuccess(binding.textInput.text.toString(), binding.secondaryTextInput.text.toString(), , binding.tertiaryTextInput.text.toString())
        }
        setNegativeButton(negativeButtonText) { _, _ ->
            callback?.onCancel()
        }
        binding.textInput.doOnTextChanged { text, _, _, _ ->
            callback?.onTextChanged(text ?: "", this)
        }
    }

    override fun create(): AlertDialog {
        dialog = super.create()
        dialog?.setOnShowListener {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            callback?.onShow(this@TextInputDialog)
        }
        return dialog!!
    }

    fun setText(text: CharSequence?, select: Boolean) {
        binding.textInput.setText(text)
        if (select) {
            binding.textInput.selectAll()
        }
    }

    fun setSecondaryText(text: CharSequence?) {
        binding.secondaryTextInput.setText(text)
    }

    fun setTertiaryText(text: CharSequence?) {
        binding.tertiaryTextInput.setText(text)
    }

    fun showSecondaryText(show: Boolean): TextInputDialog {
        binding.secondaryTextInputContainer.visibility = if (show) View.VISIBLE else View.GONE
        return this
    }

    fun showTertiaryText(show: Boolean): TextInputDialog {
        binding.tertiaryTextInputContainer.visibility = if (show) View.VISIBLE else View.GONE
        return this
    }

    fun setHint(@StringRes id: Int) {
        binding.textInputContainer.hint = context.resources.getString(id)
    }

    fun setSecondaryHint(@StringRes id: Int) {
        binding.secondaryTextInputContainer.hint = context.resources.getString(id)
    }

    fun setTertiaryHint(@StringRes id: Int) {
        binding.tertiaryTextInputContainer.hint = context.resources.getString(id)
    }

    fun setError(text: CharSequence?) {
        binding.textInputContainer.error = text
    }

    fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }
}
