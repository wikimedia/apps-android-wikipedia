package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class DescriptionEditSuccessActivity extends SingleFragmentActivity<DescriptionEditSuccessFragment>
        implements DescriptionEditSuccessFragment.Callback {

    static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DescriptionEditSuccessActivity.class);
    }

    @Override protected DescriptionEditSuccessFragment createFragment() {
        return DescriptionEditSuccessFragment.newInstance();
    }

    @Override
    public void onDismissClick() {
        setResult(RESULT_OK);
        finish();
    }
}
