package org.wikipedia.editactionfeed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.SuggestedEditsFunnel;

public class EditTasksActivity extends SingleFragmentActivity<EditTasksFragment> {
    private static SuggestedEditsFunnel FUNNEL;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, EditTasksActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFunnel();
    }

    @Override
    public void onPause() {
        super.onPause();
        getFunnel().pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getFunnel().resume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getFunnel().log();
    }

    @Override
    protected EditTasksFragment createFragment() {
        return EditTasksFragment.newInstance();
    }

    public static SuggestedEditsFunnel getFunnel() {
        if (FUNNEL == null) {
            FUNNEL = new SuggestedEditsFunnel(WikipediaApp.getInstance());
        }
        return FUNNEL;
    }
}
