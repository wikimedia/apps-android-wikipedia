package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.WindowManager;

import org.wikipedia.activity.SingleFragmentActivity;

public class DescriptionEditTutorialActivity
        extends SingleFragmentActivity<DescriptionEditTutorialFragment>
        implements DescriptionEditTutorialFragment.Callback {

    @NonNull public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DescriptionEditTutorialActivity.class);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setPortraitOrientation();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override public void onStartEditingClick() {
        setResult(RESULT_OK);
        finish();
    }

    @Override protected DescriptionEditTutorialFragment createFragment() {
        return DescriptionEditTutorialFragment.newInstance();
    }

    private void setPortraitOrientation() {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        }

        setRequestedOrientation(orientation);
    }
}