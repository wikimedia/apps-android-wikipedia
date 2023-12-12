package org.wikipedia.diff

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.EditHistoryInteractionEvent
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.DialogUndoEditBinding
import org.wikipedia.util.ResourceUtil

class UndoEditDialog constructor(
    private val editHistoryInteractionEvent: EditHistoryInteractionEvent?,
    context: Context,
    source: Constants.InvokeSource?,
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
            if (source == Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS) {
                PatrollerExperienceEvent.logAction("undo_confirm", "pt_edit",
                    PatrollerExperienceEvent.getActionDataString(summaryText = binding.textInput.text.toString()))
            }
            callback.onSuccess(binding.textInput.text.toString())
        }

        setNegativeButton(R.string.text_input_dialog_cancel_button_text) { _, _ ->
            if (source == Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS) {
                PatrollerExperienceEvent.logAction("undo_cancel", "pt_edit")
            }
            editHistoryInteractionEvent?.logUndoCancel()
        }

        binding.textInput.doOnTextChanged { text, _, _, _ ->
            setPositiveButtonEnabled(!text.isNullOrBlank())
        }

        setPositiveButtonEnabled(false)
        if (source == Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS) {
            PatrollerExperienceEvent.logAction("undo_summary_impression", "pt_edit")
        }
    }

    override fun show(): AlertDialog {
        dialog = super.show()

        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ResourceUtil.getThemedColor(context, R.attr.destructive_color))

        return dialog!!
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }
}
