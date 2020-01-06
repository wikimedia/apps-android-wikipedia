package org.wikipedia.views

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputLayout

/**
 * Triggers events when one or more EditTexts are empty or not
 */
class NonEmptyValidator(private val validationChanged: ValidationChangedCallback, private vararg val textInputs: TextInputLayout) {

    interface ValidationChangedCallback {
        fun onValidationChanged(isValid: Boolean)
    }

    init {
        val triggerWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) = Unit
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) = Unit
            override fun afterTextChanged(editable: Editable) = revalidate()
        }

        textInputs.mapNotNull { it.editText }.forEach { it.addTextChangedListener(triggerWatcher) }
    }

    private var lastIsValidValue = false

    private fun revalidate() {
        var isValid = true

        textInputs.forEach { isValid = isValid && it.editText?.text?.isNotEmpty() ?: false }

        if (isValid != lastIsValidValue) {
            lastIsValidValue = isValid
            validationChanged.onValidationChanged(isValid)
        }
    }
}
