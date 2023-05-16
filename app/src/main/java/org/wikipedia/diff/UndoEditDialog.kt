package org.wikipedia.diff

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.EditHistoryInteractionEvent
import org.wikipedia.databinding.DialogUndoEditBinding

class UndoEditDialog constructor(
    private val editHistoryInteractionEvent: EditHistoryInteractionEvent?,
    context: Context,
    callback: Callback
) : MaterialAlertDialogBuilder(context) {

    fun interface Callback {
        fun onSuccess(text: CharSequence)
    }

    private var binding = DialogUndoEditBinding.inflate(LayoutInflater.from(context))
    private var dialog: AlertDialog? = null

    init {
        setView(binding.root)
        binding.textInputContainer.isErrorEnabled = true

        setPositiveButton(R.string.edit_undo) { _, _ ->
            callback.onSuccess(binding.textInput.text.toString())
        }

        setNegativeButton(R.string.text_input_dialog_cancel_button_text) { _, _ ->
            editHistoryInteractionEvent?.logUndoCancel()
        }

        binding.textInput.doOnTextChanged { text, _, _, _ ->
            setPositiveButtonEnabled(!text.isNullOrBlank())
        }

        setPositiveButtonEnabled(false)
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        setPositiveButtonEnabled(false)
        return dialog!!
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }
}
