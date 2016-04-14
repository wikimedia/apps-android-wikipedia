package org.wikipedia.readinglist;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.DimenUtil;

public final class ReadingListDialogs {

    public static AlertDialog createEditDialog(Context context, final ReadingList readingList,
                                               boolean showDescription,
                                               @NonNull final Runnable onSuccess,
                                               @Nullable final Runnable onDelete) {
        final View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_reading_list_edit, null);
        final EditText titleView = (EditText) rootView.findViewById(R.id.reading_list_title);
        final EditText descriptionView = (EditText) rootView.findViewById(R.id.reading_list_description);
        final View deleteView = rootView.findViewById(R.id.reading_list_delete_link);
        deleteView.setVisibility(onDelete != null ? View.VISIBLE : View.GONE);
        final SwitchCompat saveOfflineSwitch = (SwitchCompat) rootView.findViewById(R.id.reading_list_offline_switch);
        descriptionView.setVisibility(showDescription ? View.VISIBLE : View.GONE);

        // TODO: move this to XML once the attribute becomes available.
        final int switchPaddingDp = 4;
        saveOfflineSwitch.setSwitchPadding((int) (switchPaddingDp * DimenUtil.getDensityScalar()));

        titleView.setText(readingList.getTitle());
        titleView.selectAll();

        descriptionView.setText(readingList.getDescription());
        saveOfflineSwitch.setChecked(readingList.getSaveOffline());

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(rootView)
                .setPositiveButton(R.string.reading_list_edit_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        readingList.setTitle(titleView.getText().toString());
                        readingList.setDescription(descriptionView.getText().toString());
                        readingList.setSaveOffline(saveOfflineSwitch.isChecked());
                        onSuccess.run();
                    }
                }).setNegativeButton(R.string.reading_list_edit_cancel, null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager) WikipediaApp.getInstance().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, 0);
            }
        });
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        deleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onDelete != null) {
                    alertDialog.dismiss();
                    onDelete.run();
                }
            }
        });

        return alertDialog;
    }

    public static AlertDialog createDeleteDialog(Context context, @NonNull final Runnable onSuccess) {
        return new AlertDialog.Builder(context)
                .setMessage(R.string.reading_list_delete_confirm)
                .setPositiveButton(R.string.reading_list_edit_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onSuccess.run();
                    }
                })
                .setNegativeButton(R.string.reading_list_edit_cancel, null)
                .create();
    }

    private ReadingListDialogs() {
    }
}
