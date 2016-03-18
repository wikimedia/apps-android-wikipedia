package org.wikipedia.page;

import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class ExclusiveBottomSheetPresenter {
    private static final String BOTTOM_SHEET_FRAGMENT_TAG = "bottom_sheet_fragment";
    private FragmentActivity activity;
    private Dialog currentDialog;

    public ExclusiveBottomSheetPresenter(FragmentActivity activity) {
        this.activity = activity;
    }

    public void show(DialogFragment dialog) {
        dismiss();
        dialog.show(activity.getSupportFragmentManager(), BOTTOM_SHEET_FRAGMENT_TAG);
    }

    public void show(Dialog dialog) {
        dismiss();
        currentDialog = dialog;
        currentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                currentDialog = null;
            }
        });
        currentDialog.show();
    }

    public void dismiss() {
        DialogFragment dialog = (DialogFragment) activity.getSupportFragmentManager().findFragmentByTag(BOTTOM_SHEET_FRAGMENT_TAG);
        if (dialog != null) {
            dialog.dismiss();
        }
        if (currentDialog != null) {
            currentDialog.setOnDismissListener(null);
            currentDialog.dismiss();
        }
        currentDialog = null;
    }
}
