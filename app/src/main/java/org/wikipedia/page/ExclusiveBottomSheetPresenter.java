package org.wikipedia.page;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class ExclusiveBottomSheetPresenter {
    private static final String BOTTOM_SHEET_FRAGMENT_TAG = "bottom_sheet_fragment";
    private FragmentActivity activity;

    public ExclusiveBottomSheetPresenter(FragmentActivity activity) {
        this.activity = activity;
    }

    public void show(DialogFragment dialog) {
        dismiss();
        dialog.show(activity.getSupportFragmentManager(), BOTTOM_SHEET_FRAGMENT_TAG);
    }

    public void dismiss() {
        DialogFragment dialog = (DialogFragment) activity.getSupportFragmentManager().findFragmentByTag(BOTTOM_SHEET_FRAGMENT_TAG);
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
