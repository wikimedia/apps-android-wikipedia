package org.wikipedia.offline;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class CompilationDetailActivity extends SingleFragmentActivity<CompilationDetailFragment> {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, CompilationDetailActivity.class);
    }

    @Override
    protected CompilationDetailFragment createFragment() {
        return CompilationDetailFragment.newInstance();
    }
}
