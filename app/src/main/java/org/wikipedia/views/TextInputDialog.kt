package org.wikipedia.views

import android.content.Context
import android.content.DialogInterface
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import org.wikipedia.R
import org.wikipedia.databinding.DialogTextInputBinding

class TextInputDialog constructor(context: Context) : AlertDialog(context) {
    interface Callback {
        fun onShow(dialog: TextInputDialog)
        fun onTextChanged(text: CharSequence, dialog: TextInputDialog)
        fun onSuccess(text: CharSequence, secondaryText: CharSequence)
        fun onCancel()
    }

    private lateinit var watcher: TextWatcher
    private var binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
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
        binding.secondaryTextInputContainer.isVisible = show
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
        watcher = binding.textInput.doOnTextChanged { text, _, _, _ ->
            callback?.onTextChanged(text ?: "", this@TextInputDialog)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.textInput.removeTextChangedListener(watcher)
    }
}
