package org.wikipedia.diff

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.EditHistoryInteractionEvent
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.DialogUndoEditBinding
import org.wikipedia.util.ResourceUtil

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

        setPositiveButton(R.string.edit_undo) { _, _ ->
            PatrollerExperienceEvent.logAction("undo_confirm", "pt_edit")
            callback.onSuccess(binding.textInput.text.toString())
        }

        setNegativeButton(R.string.text_input_dialog_cancel_button_text) { _, _ ->
            PatrollerExperienceEvent.logAction("undo_cancel", "pt_edit")
            editHistoryInteractionEvent?.logUndoCancel()
        }

        binding.textInput.doOnTextChanged { text, _, _, _ ->
            setPositiveButtonEnabled(!text.isNullOrBlank())
        }

        setPositiveButtonEnabled(false)
        PatrollerExperienceEvent.logAction("undo_summary_impression", "pt_edit")
    }

    override fun show(): AlertDialog {
        dialog = super.show()

        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ResourceUtil.getThemedColor(context, R.attr.destructive_color))

        setPositiveButtonEnabled(false)
        return dialog!!
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }
}
