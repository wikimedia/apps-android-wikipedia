package org.wikipedia.views

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import org.wikipedia.R
import org.wikipedia.util.DeviceUtil


class TextInputDialog private constructor(context: Context) : AlertDialog(context) {

    interface Callback {
        fun onShow(dialog: TextInputDialog)
        fun onTextChanged(text: CharSequence, dialog: TextInputDialog)
        fun onSuccess(text: CharSequence, secondaryText: CharSequence)
        fun onCancel()
    }

    init {
        setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(R.string.text_input_dialog_ok_button_text)) { _, _ ->
            //DeviceUtil.hideSoftKeyboard();
            callback?.onSuccess(editText.text, secondaryText.text)
        }

        setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getString(R.string.text_input_dialog_cancel_button_text)) { _, _ ->
            //DeviceUtil.hideSoftKeyboard(editText);
            callback?.onCancel()
        }

        setOnShowListener {
            editText.requestFocus()
            DeviceUtil.showSoftKeyboard(editText)
            callback?.onShow(this@TextInputDialog)
        }

        setOnDismissListener { }
    }

    private var callback: Callback? = null
    private lateinit var editText: EditText
    private lateinit var secondaryText: EditText
    private lateinit var editTextContainer: TextInputLayout
    private lateinit var secondaryTextContainer: TextInputLayout
    private val watcher = TextInputWatcher()

    fun setCallback(callback: Callback?): TextInputDialog {
        this.callback = callback
        return this
    }

    fun setView(@LayoutRes id: Int): TextInputDialog {
        val rootView = LayoutInflater.from(context).inflate(id, null)
        editText = rootView.findViewById(R.id.text_input)
        editTextContainer = rootView.findViewById(R.id.text_input_container)
        secondaryText = rootView.findViewById(R.id.secondary_text_input)
        secondaryTextContainer = rootView.findViewById(R.id.secondary_text_input_container)
        super.setView(rootView)

        editTextContainer.isErrorEnabled = true
        return this
    }

    fun setText(text: CharSequence?) = editText.setText(text)

    fun setSecondaryText(text: CharSequence?) = secondaryText.setText(text)

    private fun showSecondaryText(show: Boolean): TextInputDialog {
        secondaryTextContainer.visibility = if (show) View.VISIBLE else View.GONE
        return this
    }

    fun setHint(@StringRes id: Int) {
        editTextContainer.hint = context.resources.getString(id)
    }

    fun setSecondaryHint(@StringRes id: Int) {
        secondaryTextContainer.hint = context.resources.getString(id)
    }

    fun selectAll() = editText.selectAll()

    fun setError(text: CharSequence?) {
        editTextContainer.error = text
    }

    fun setPositiveButtonEnabled(enabled: Boolean) {
        getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = enabled
    }

    override fun onAttachedToWindow() = editText.addTextChangedListener(watcher)

    override fun onDetachedFromWindow() = editText.removeTextChangedListener(watcher)

    class DefaultCallback : Callback {
        override fun onShow(dialog: TextInputDialog) = Unit
        override fun onTextChanged(text: CharSequence, dialog: TextInputDialog) = Unit
        override fun onSuccess(text: CharSequence, secondaryText: CharSequence) = Unit
        override fun onCancel() = Unit
    }

    private inner class TextInputWatcher : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) = Unit
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            callback?.onTextChanged(charSequence, this@TextInputDialog)
        }

        override fun afterTextChanged(editable: Editable) = Unit
    }

    companion object {
        @JvmStatic
        fun newInstance(context: Context, showSecondaryText: Boolean, callback: Callback?): TextInputDialog =
                TextInputDialog(context)
                        .setView(R.layout.dialog_text_input)
                        .showSecondaryText(showSecondaryText)
                        .setCallback(callback)
    }
}
