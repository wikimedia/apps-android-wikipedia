package org.wikipedia.editactionfeed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.SuggestedEditsFunnel;

public class EditTasksActivity extends SingleFragmentActivity<EditTasksFragment> {
    public static Intent newIntent(@NonNull Context context, Constants.InvokeSource invokeSource) {
        return new Intent(context, EditTasksActivity.class)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().hasExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE)) {
            Constants.InvokeSource source = (Constants.InvokeSource) getIntent().getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE);
            SuggestedEditsFunnel.get(source);

            if (source == Constants.InvokeSource.EDIT_FEED_TITLE_DESC
                    || source == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
                startActivity(AddTitleDescriptionsActivity.Companion.newIntent(this, source));
            }

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
        SuggestedEditsFunnel.reset();
    }

    @Override
    protected EditTasksFragment createFragment() {
        return EditTasksFragment.newInstance();
    }
}
