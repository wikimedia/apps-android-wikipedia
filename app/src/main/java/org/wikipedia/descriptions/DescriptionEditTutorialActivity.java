package org.wikipedia.descriptions;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class DescriptionEditTutorialActivity
        extends SingleFragmentActivity<DescriptionEditTutorialFragment>
        implements DescriptionEditTutorialFragment.Callback {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DescriptionEditTutorialActivity.class);
    }

    @TargetApi(18) @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setting this in the manifest will not suffice since any manifest screenOrientation
        // setting is overridden by the call to ActivityUtil.requestFullUserOrientation() in the
        // base ActionBarActivity's onCreate().
        setRequestedOrientation(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override protected DescriptionEditTutorialFragment createFragment() {
        return DescriptionEditTutorialFragment.newInstance();
    }

    @Override public void onStartEditingClick() {
        setResult(RESULT_OK);
        finish();
    }
}
