package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageTitle;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditActivity extends SingleFragmentActivity<DescriptionEditFragment>
        implements DescriptionEditFragment.Callback {
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_USER_ID = "userId";

    public static Intent newIntent(@NonNull Context context, @NonNull PageTitle title) {
        return new Intent(context, DescriptionEditActivity.class)
                .putExtra(EXTRA_TITLE, GsonMarshaller.marshal(title));
    }

    @Override
    public void onDescriptionEditSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public DescriptionEditFragment createFragment() {
        return DescriptionEditFragment.newInstance(
                GsonUnmarshaller.unmarshal(PageTitle.class, getIntent().getStringExtra(EXTRA_TITLE)),
                getIntent().getIntExtra(EXTRA_USER_ID, 0)
        );
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard(this);
        super.onBackPressed();
    }
}
