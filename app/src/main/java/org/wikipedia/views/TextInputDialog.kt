package org.wikipedia.views

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.databinding.DialogTextInputBinding

class TextInputDialog constructor(context: Context) : AlertDialog(context) {
    interface Callback {
        fun onShow(dialog: TextInputDialog)
        fun onTextChanged(text: CharSequence, dialog: TextInputDialog)
        fun onSuccess(text: CharSequence, secondaryText: CharSequence)
        fun onCancel()
    }

    private var binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
    private val watcher = TextInputWatcher()
    var callback: Callback? = null

    init {
        setView(binding.root)
        binding.textInputContainer.isErrorEnabled = true
        setButton(BUTTON_POSITIVE, context.getString(R.string.text_input_dialog_ok_button_text)) { _: DialogInterface, _: Int ->
            callback?.onSuccess(binding.textInput.text.toString(), binding.secondaryTextInput.text.toString())
        }
        setButton(BUTTON_NEGATIVE, context.getString(R.string.text_input_dialog_cancel_button_text)) { _: DialogInterface, _: Int ->
            callback?.onCancel()
        }
        setOnShowListener {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            callback?.onShow(this@TextInputDialog)
        }
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

    fun showSecondaryText(show: Boolean): TextInputDialog {
        binding.secondaryTextInputContainer.visibility = if (show) View.VISIBLE else View.GONE
        return this
    }

    fun setHint(@StringRes id: Int) {
        binding.textInput.hint = context.resources.getString(id)
    }

    fun setSecondaryHint(@StringRes id: Int) {
        binding.secondaryTextInputContainer.hint = context.resources.getString(id)
    }

    fun setError(text: CharSequence?) {
        binding.textInputContainer.error = text
    }

    fun setPositiveButtonEnabled(enabled: Boolean) {
        getButton(BUTTON_POSITIVE).isEnabled = enabled
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding.textInput.addTextChangedListener(watcher)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.textInput.removeTextChangedListener(watcher)
    }

    private inner class TextInputWatcher : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            callback?.onTextChanged(charSequence, this@TextInputDialog)
        }

        override fun afterTextChanged(editable: Editable) {}
    }
}
