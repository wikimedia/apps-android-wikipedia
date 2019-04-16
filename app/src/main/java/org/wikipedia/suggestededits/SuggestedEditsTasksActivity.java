package org.wikipedia.suggestededits;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.SuggestedEditsFunnel;

import androidx.annotation.NonNull;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.Constants.InvokeSource.EDIT_FEED_TITLE_DESC;
import static org.wikipedia.Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC;

public class SuggestedEditsTasksActivity extends SingleFragmentActivity<SuggestedEditsTasksFragment> {
    private static final String EXTRA_START_IMMEDIATELY = "startImmediately";
    private boolean startImmediately;

    public static Intent newIntent(@NonNull Context context, InvokeSource invokeSource) {
        Intent intent = new Intent(context, SuggestedEditsTasksActivity.class)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
        if (invokeSource == EDIT_FEED_TITLE_DESC
                || invokeSource == EDIT_FEED_TRANSLATE_TITLE_DESC) {
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

        if (getIntent().hasExtra(INTENT_EXTRA_INVOKE_SOURCE)) {
            InvokeSource source = (InvokeSource) getIntent().getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE);
            SuggestedEditsFunnel.get(source);

            if (startImmediately && (source == EDIT_FEED_TITLE_DESC || source == EDIT_FEED_TRANSLATE_TITLE_DESC)) {
                startImmediately = false;
                startActivity(SuggestedEditsAddDescriptionsActivity.Companion.newIntent(this, source));
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
    protected SuggestedEditsTasksFragment createFragment() {
        return SuggestedEditsTasksFragment.newInstance();
    }
}
