package org.wikipedia.editactionfeed;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.main.MainActivity;
import org.wikipedia.util.log.L;

public class EditTasksActivity extends SingleFragmentActivity<EditTasksFragment> {

    @Override
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
