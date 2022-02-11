package org.wikipedia.diff

import android.content.Context
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import org.wikipedia.R
import org.wikipedia.databinding.DialogUndoEditBinding

class UndoEditDialog constructor(context: Context) : AlertDialog(context) {
    interface Callback {
        fun onSuccess(text: CharSequence)
    }

    private var binding = DialogUndoEditBinding.inflate(LayoutInflater.from(context))
    private lateinit var watcher: TextWatcher
    var callback: Callback? = null

    init {
        setView(binding.root)
        binding.textInputContainer.isErrorEnabled = true
        setButton(BUTTON_POSITIVE, context.getString(R.string.edit_undo)) { _, _ ->
            callback?.onSuccess(binding.textInput.text.toString())
        }
        setButton(BUTTON_NEGATIVE, context.getString(R.string.text_input_dialog_cancel_button_text)) { _, _ -> }
        create()
        setPositiveButtonEnabled(false)
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        getButton(BUTTON_POSITIVE).isEnabled = enabled
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        watcher = binding.textInput.doOnTextChanged { text, _, _, _ ->
            setPositiveButtonEnabled(!text.isNullOrBlank())
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.textInput.removeTextChangedListener(watcher)
    }
}
