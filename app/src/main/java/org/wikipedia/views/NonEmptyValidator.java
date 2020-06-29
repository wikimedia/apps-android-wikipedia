package org.wikipedia.views;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;

/**
 * Triggers events when one or more EditTexts are empty or not
 */
public class NonEmptyValidator {
    private final TextInputLayout[] textInputs;
    private final Button actionButton;

    public NonEmptyValidator(@NonNull Button actionButton, TextInputLayout... textInputs) {
        this.textInputs = textInputs;
        this.actionButton = actionButton;

        TextWatcher triggerWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                revalidate();
            }
        };

        for (TextInputLayout t : textInputs) {
            t.getEditText().addTextChangedListener(triggerWatcher);
        }
    }

    private boolean lastIsValidValue = false;
    private void revalidate() {
        boolean isValid = true;
        for (TextInputLayout t : textInputs) {
            isValid = isValid && t.getEditText().getText().length() != 0;
        }

        if (isValid != lastIsValidValue) {
            lastIsValidValue = isValid;
            actionButton.setEnabled(lastIsValidValue);
            actionButton.setAlpha(lastIsValidValue ? 1.0f : 0.5f);
        }
    }
}
