package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;

public class DescriptionEditTutorialActivity
        extends SingleFragmentActivity<DescriptionEditTutorialFragment>
        implements DescriptionEditTutorialFragment.Callback {

    @NonNull public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DescriptionEditTutorialActivity.class);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.dark_blue);
    }

    @Override public void onStartEditingClick() {
        setResult(RESULT_OK);
        finish();
    }

    @Override protected DescriptionEditTutorialFragment createFragment() {
        return DescriptionEditTutorialFragment.newInstance();
    }
}
