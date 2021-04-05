package org.wikipedia.views

import android.widget.Button
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout

/**
 * Triggers events when one or more EditTexts are empty or not
 */
class NonEmptyValidator(private val actionButton: Button, private vararg val textInputs: TextInputLayout) {

    private var lastIsValidValue = false

    init {
        textInputs.forEach { it.editText?.doAfterTextChanged { revalidate() } }
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
