package org.wikipedia.editactionfeed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.SuggestedEditsFunnel;
import org.wikipedia.main.MainActivity;

public class EditTasksActivity extends SingleFragmentActivity<EditTasksFragment> {
    public static Intent newIntent(@NonNull Context context, Constants.InvokeSource invokeSource) {
        return new Intent(context, EditTasksActivity.class)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SuggestedEditsFunnel.reset();
        if (getIntent().hasExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE)) {
            SuggestedEditsFunnel.get((Constants.InvokeSource) getIntent().getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE));
        } else {
            SuggestedEditsFunnel.get();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SuggestedEditsFunnel.get().pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SuggestedEditsFunnel.get().resume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SuggestedEditsFunnel.get().log();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!WikipediaApp.getInstance().haveMainActivity()) {
                    startActivity(MainActivity.newIntent(this)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(Constants.INTENT_RETURN_TO_MAIN, true));
                } else {
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, EditTasksActivity.class);
    }

    @Override
    protected EditTasksFragment createFragment() {
        return EditTasksFragment.newInstance();
    }
}
