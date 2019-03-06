package org.wikipedia.editactionfeed;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class EditTasksActivity extends SingleFragmentActivity<EditTasksFragment> {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, EditTasksActivity.class);
    }

    @Override
    protected EditTasksFragment createFragment() {
        return EditTasksFragment.newInstance();
    }
}
