package org.wikipedia.editactionfeed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.wikipedia.Constants;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.SuggestedEditsFunnel;

import androidx.annotation.NonNull;

public class EditTasksActivity extends SingleFragmentActivity<EditTasksFragment> {
    private static final String EXTRA_START_IMMEDIATELY = "startImmediately";
    private boolean startImmediately;

    public static Intent newIntent(@NonNull Context context, Constants.InvokeSource invokeSource) {
        Intent intent = new Intent(context, EditTasksActivity.class)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
        if (invokeSource == Constants.InvokeSource.EDIT_FEED_TITLE_DESC
                || invokeSource == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            intent.putExtra(EXTRA_START_IMMEDIATELY, true);
        }
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startImmediately = savedInstanceState == null
                ? getIntent().getBooleanExtra(EXTRA_START_IMMEDIATELY, false)
                : savedInstanceState.getBoolean(EXTRA_START_IMMEDIATELY, false);

        if (getIntent().hasExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE)) {
            Constants.InvokeSource source = (Constants.InvokeSource) getIntent().getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE);
            SuggestedEditsFunnel.get(source);

            if (startImmediately
                    && (source == Constants.InvokeSource.EDIT_FEED_TITLE_DESC
                    || source == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC)) {
                startImmediately = false;
                startActivity(AddTitleDescriptionsActivity.Companion.newIntent(this, source));
            }

        } else {
            SuggestedEditsFunnel.get();
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_START_IMMEDIATELY, startImmediately);
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
