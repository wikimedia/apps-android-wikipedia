package org.wikipedia.views

import com.google.android.material.textfield.TextInputLayout
import android.text.TextWatcher
import android.text.Editable
import android.widget.Button

/**
 * Triggers events when one or more EditTexts are empty or not
 */
class NonEmptyValidator(private val actionButton: Button, private vararg val textInputs: TextInputLayout) {

    private var lastIsValidValue = false

    init {
        val triggerWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun afterTextChanged(editable: Editable) {
                revalidate()
            }
        }
        textInputs.forEach {
            it.editText!!.addTextChangedListener(triggerWatcher)
        }
    }

    private fun revalidate() {
        var isValid = true
        textInputs.forEach {
            isValid = isValid && it.editText!!.text.isNotEmpty()
        }
        if (isValid != lastIsValidValue) {
            lastIsValidValue = isValid
            actionButton.isEnabled = lastIsValidValue
            actionButton.alpha = if (lastIsValidValue) 1.0f else 0.5f
        }
    }
}
