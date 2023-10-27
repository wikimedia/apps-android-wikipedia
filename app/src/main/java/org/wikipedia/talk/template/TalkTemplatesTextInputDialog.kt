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
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
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
    val isSaveAsNewChecked get() = binding.dialogSaveAsNewCheckbox.isChecked
    val isSaveExistingChecked get() = binding.dialogSaveExistingCheckbox.isChecked

    init {
        setView(binding.root)
        binding.titleInputContainer.isErrorEnabled = true

        setPositiveButton(positiveButtonText) { _, _ ->
            callback?.onSuccess(binding.titleInput.text.toString().trim(), binding.subjectTextInput.text.toString().trim(), binding.bodyTextInput.editText.text.toString().trim())
        }
        setNegativeButton(negativeButtonText) { _, _ ->
            callback?.onCancel()
        }
        setOnDismissListener {
            callback?.onDismiss()
        }
        binding.titleInput.doOnTextChanged { text, _, _, _ ->
            if (binding.dialogSaveAsNewCheckbox.isVisible && !binding.dialogSaveAsNewCheckbox.isChecked) {
                binding.dialogSaveAsNewCheckbox.isChecked = true
                binding.dialogSaveExistingCheckbox.isChecked = false
            }
            callback?.onTextChanged(text ?: "", this)
        }
        binding.dialogSaveAsNewCheckbox.setOnClickListener {
            if (binding.dialogSaveExistingCheckbox.isChecked) {
                binding.dialogSaveAsNewCheckbox.isChecked = true
                binding.dialogSaveExistingCheckbox.isChecked = false
            }
            setPositiveButtonEnabled(binding.titleInput.text?.isNotBlank() ?: false)
        }
        binding.dialogSaveExistingCheckbox.setOnClickListener {
            if (binding.dialogSaveAsNewCheckbox.isChecked) {
                binding.dialogSaveAsNewCheckbox.isChecked = false
                binding.dialogSaveExistingCheckbox.isChecked = true
            }
            setError(null)
            setPositiveButtonEnabled(true)
        }
        binding.dialogSaveAsNewCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                setError(null)
                binding.root.requestFocus()
            }
        }
    }

    override fun create(): AlertDialog {
        dialog = super.create()
        dialog?.setOnShowListener {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            callback?.onShow(this@TalkTemplatesTextInputDialog)
            addTextWatcher()
        }
        return dialog!!
    }

    private fun addTextWatcher() {
        val textWatcher = binding.titleInput.doOnTextChanged { _, _, _, _ ->
            binding.titleInputContainer.error = null
            binding.subjectTextInputContainer.error = null
            binding.bodyTextInput.textInputLayout.error = null
            val title = binding.titleInput.text.toString().trim()
            val subject = binding.subjectTextInput.text.toString().trim()
            val body = binding.bodyTextInput.editText.text.toString().trim()
            if (title.isEmpty()) {
                PatrollerExperienceEvent.logAction("publish_error_title", "pt_warning_messages")
                binding.titleInputContainer.error = context.getString(R.string.talk_templates_message_title_empty)
            }
            if (subject.isEmpty()) {
                binding.subjectTextInputContainer.error = context.getString(R.string.talk_subject_empty)
            }
            if (body.isEmpty()) {
                binding.bodyTextInput.textInputLayout.error = context.getString(R.string.talk_message_empty)
            }
            if (binding.subjectTextInputContainer.isVisible && binding.bodyTextInput.isVisible) {
                setPositiveButtonEnabled(title.isNotBlank() && subject.isNotBlank() && body.isNotBlank())
            } else {
                setPositiveButtonEnabled(title.isNotBlank())
            }
        }
        binding.subjectTextInput.addTextChangedListener(textWatcher)
        binding.bodyTextInput.editText.addTextChangedListener(textWatcher)
    }

    fun requestFocus() {
        binding.titleInput.requestFocus()
    }

    fun showTemplateCheckboxes(hasTemplate: Boolean) {
        binding.dialogSaveAsNewCheckbox.isVisible = true
        if (!hasTemplate) {
            binding.dialogSaveAsNewCheckbox.text = context.getString(R.string.talk_warn_save_dialog_message)
        } else {
            binding.dialogSaveAsNewCheckbox.text = context.getString(R.string.talk_warn_save_dialog_existing_new_message)
            binding.dialogSaveExistingCheckbox.isVisible = true
        }
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
        binding.titleInputContainer.isErrorEnabled = !text.isNullOrEmpty()
    }

    fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }
}
