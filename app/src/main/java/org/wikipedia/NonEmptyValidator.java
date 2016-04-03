package org.wikipedia;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Triggers events when one or more EdtiTexts are empty or not
 */
public class NonEmptyValidator {
    private final EditText[] editTexts;
    private final ValidationChangedCallback validationChanged;

    public interface ValidationChangedCallback {
        void onValidationChanged(boolean isValid);
    }

    public NonEmptyValidator(ValidationChangedCallback validationChanged, EditText... editTexts) {
        this.editTexts = editTexts;
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

        for (EditText editText : editTexts) {
            editText.addTextChangedListener(triggerWatcher);
        }
    }

    private boolean lastIsValidValue = false;
    private void revalidate() {
        boolean isValid = true;
        for (EditText editText : editTexts) {
            isValid = isValid && editText.getText().length() != 0;
        }

        if (isValid != lastIsValidValue) {
            lastIsValidValue = isValid;
            validationChanged.onValidationChanged(isValid);
        }
    }
}
