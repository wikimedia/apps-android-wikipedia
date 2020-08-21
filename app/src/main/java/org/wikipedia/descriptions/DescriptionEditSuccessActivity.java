package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.activity.SingleFragmentActivityTransparent;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class DescriptionEditSuccessActivity
        extends SingleFragmentActivityTransparent<DescriptionEditSuccessFragment>
        implements DescriptionEditSuccessFragment.Callback {

    public static final int RESULT_OK_FROM_EDIT_SUCCESS = 1;

    static Intent newIntent(@NonNull Context context, @NonNull Constants.InvokeSource invokeSource) {
        return new Intent(context, DescriptionEditSuccessActivity.class)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
    }

    @Override protected DescriptionEditSuccessFragment createFragment() {
        return DescriptionEditSuccessFragment.newInstance();
    }

    @Override
    public void onDismissClick() {
        setResult(RESULT_OK_FROM_EDIT_SUCCESS, getIntent());
        finish();
    }
}
