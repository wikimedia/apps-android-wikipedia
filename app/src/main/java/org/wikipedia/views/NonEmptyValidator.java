package org.wikipedia.views;

import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;

/**
 * Triggers events when one or more EditTexts are empty or not
 */
public class NonEmptyValidator {
    private final TextInputLayout[] textInputs;
    private final ValidationChangedCallback validationChanged;

    public interface ValidationChangedCallback {
        void onValidationChanged(boolean isValid);
    }

    public NonEmptyValidator(ValidationChangedCallback validationChanged, TextInputLayout... textInputs) {
        this.textInputs = textInputs;
        this.validationChanged = validationChanged;

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
            validationChanged.onValidationChanged(isValid);
        }
    }
}
