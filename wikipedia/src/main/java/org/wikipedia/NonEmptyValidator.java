package org.wikipedia;

import android.text.*;
import android.widget.*;
import com.sun.corba.se.spi.activation.*;

import java.util.*;

/**
 * Triggers events when one or more EdtiTexts are empty or not
 */
public class NonEmptyValidator {
    private final EditText editTexts[];
    private final ValidationChangedCallback validationChanged;

    public interface ValidationChangedCallback {
        void onValidationChanged(boolean isValid);
    }

    public NonEmptyValidator(ValidationChangedCallback validationChanged, EditText... editTexts) {
        this.editTexts = editTexts;
        this.validationChanged = validationChanged;

        TextWatcher triggerWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                revalidate();
            }
        };

        for (int i = 0; i < editTexts.length; i++) {
            editTexts[i].addTextChangedListener(triggerWatcher);
        }
    }

    private boolean lastIsValidValue = false;
    private void revalidate() {
        boolean isValid = true;
        for (int i = 0; i < editTexts.length; i++) {
            isValid = isValid && editTexts[i].getText().length() != 0;
        }

        if (isValid != lastIsValidValue) {
            validationChanged.onValidationChanged(isValid);
            lastIsValidValue = isValid;
        }
    }

    public boolean isValid() {
        return lastIsValidValue;
    }
}
