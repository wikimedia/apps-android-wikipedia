package org.wikipedia.page;

import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

public class ExclusiveBottomSheetPresenter {
    private static final String BOTTOM_SHEET_FRAGMENT_TAG = "bottom_sheet_fragment";
    private FragmentManager fragmentManager;
    private Dialog currentDialog;

    public ExclusiveBottomSheetPresenter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void show(DialogFragment dialog) {
        dismiss();
        dialog.show(fragmentManager, BOTTOM_SHEET_FRAGMENT_TAG);
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
        DialogFragment dialog = (DialogFragment) fragmentManager.findFragmentByTag(BOTTOM_SHEET_FRAGMENT_TAG);
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
