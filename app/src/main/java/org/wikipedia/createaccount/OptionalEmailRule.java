package org.wikipedia.createaccount;

import android.text.TextUtils;

import com.mobsandgeeks.saripaar.AnnotationRule;

import commons.validator.routines.EmailValidator;

public class OptionalEmailRule extends AnnotationRule<OptionalEmail, String> {
    protected OptionalEmailRule(final OptionalEmail email) {
        super(email);
    }

    @Override
    public boolean isValid(final String email) {
        if (TextUtils.isEmpty(email)) {
            return true;
        } else {
            boolean allowLocal = mRuleAnnotation.allowLocal();
            return EmailValidator.getInstance(allowLocal).isValid(email);
        }
    }
}