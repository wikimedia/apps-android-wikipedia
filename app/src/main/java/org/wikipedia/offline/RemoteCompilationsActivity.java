package org.wikipedia.offline;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;

public class RemoteCompilationsActivity extends SingleFragmentActivity<RemoteCompilationsFragment> {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.green30);
    }

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, RemoteCompilationsActivity.class);
    }

    @Override
    public RemoteCompilationsFragment createFragment() {
        return RemoteCompilationsFragment.newInstance();
    }
}
