package org.wikipedia.page;

import android.app.Dialog;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.readinglist.AddToReadingListDialog;

public class ExclusiveBottomSheetPresenter {
    private static final String BOTTOM_SHEET_FRAGMENT_TAG = "bottom_sheet_fragment";
    private Dialog currentDialog;

    public void showAddToListDialog(@NonNull FragmentManager fm, @NonNull PageTitle title,
                                    @NonNull InvokeSource source) {
        show(fm, AddToReadingListDialog.newInstance(title, source));
    }

    public void showAddToListDialog(@NonNull FragmentManager fm, @NonNull PageTitle title,
                                    @NonNull InvokeSource source,
                                    @Nullable DialogInterface.OnDismissListener listener) {
        show(fm, AddToReadingListDialog.newInstance(title, source, listener));
    }

    public void show(@NonNull FragmentManager manager, @NonNull DialogFragment dialog) {
        if (manager.isStateSaved() || manager.isDestroyed()) {
            return;
        }
        dismiss(manager);
        dialog.show(manager, BOTTOM_SHEET_FRAGMENT_TAG);
    }

    public void show(@NonNull FragmentManager manager, @NonNull Dialog dialog) {
        if (manager.isStateSaved() || manager.isDestroyed()) {
            return;
        }
        dismiss(manager);
        currentDialog = dialog;
        currentDialog.setOnDismissListener((dialogInterface) -> currentDialog = null);
        currentDialog.show();
    }

    public void dismiss(@NonNull FragmentManager manager) {
        if (manager.isStateSaved() || manager.isDestroyed()) {
            return;
        }
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(BOTTOM_SHEET_FRAGMENT_TAG);
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
