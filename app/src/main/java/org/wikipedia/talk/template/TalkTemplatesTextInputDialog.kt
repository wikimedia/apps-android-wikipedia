package org.wikipedia.talk.template

import android.app.Activity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.DialogTalkTemplatesTextInputBinding
import org.wikipedia.talk.TalkReplyActivity

class TalkTemplatesTextInputDialog constructor(private val activity: Activity,
                                               positiveButtonText: Int = R.string.text_input_dialog_ok_button_text,
                                               negativeButtonText: Int = R.string.text_input_dialog_cancel_button_text) : MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_Input) {
    interface Callback {
        fun onSuccess()
        fun onCancel()
        fun onDismiss()
    }

    private var binding = DialogTalkTemplatesTextInputBinding.inflate(LayoutInflater.from(context))
    private var dialog: AlertDialog? = null
    var callback: Callback? = null
    val isSaveAsNewChecked get() = binding.dialogSaveAsNewRadio.isChecked
    val isSaveExistingChecked get() = binding.dialogSaveExistingRadio.isChecked

    init {
        setView(binding.root)

        setPositiveButton(positiveButtonText) { _, _ ->
            callback?.onSuccess()
        }
        setNegativeButton(negativeButtonText) { _, _ ->
            callback?.onCancel()
        }
        setOnDismissListener {
            callback?.onDismiss()
        }
    }

    override fun create(): AlertDialog {
        dialog = super.create()
        dialog?.setOnShowListener {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            binding.dialogSaveAsNewRadio.isChecked = true
        }
        return dialog!!
    }

    fun showTemplateCheckboxes() {
        binding.dialogSaveAsNewRadio.isVisible = true
        binding.dialogSaveExistingRadio.isVisible = true
    }

    private fun sendPatrollerExperienceEvent(action: String) {
        PatrollerExperienceEvent.logAction(
            action, if (activity is TalkReplyActivity) "pt_warning_messages" else "pt_templates"
        )
    }
}
