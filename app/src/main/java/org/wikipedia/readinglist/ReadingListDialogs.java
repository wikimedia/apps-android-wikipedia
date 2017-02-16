package org.wikipedia.readinglist;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;

import java.util.List;

public final class ReadingListDialogs {

    public interface EditDialogListener {
        void onModify(String newTitle, String newDescription, boolean saveOffline);
        void onDelete();
    }

    private interface TitleTextCallback {
        void onEntryMatchesExistingTitle(@NonNull String title);
        void onEntryEmpty();
        void onEntryOk();
    }

    private static class TitleTextWatcher implements TextWatcher {
        @NonNull private List<String> titles;
        @NonNull private TitleTextCallback cb;

        TitleTextWatcher(@NonNull List<String> titles, @NonNull TitleTextCallback cb) {
            this.titles = titles;
            this.cb = cb;
        }

        @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (StringUtils.isEmpty(charSequence)) {
                cb.onEntryEmpty();
                return;
            }

            if (titles.contains(charSequence.toString())) {
                cb.onEntryMatchesExistingTitle(charSequence.toString());
                return;
            }

            cb.onEntryOk();
        }

        @Override public void afterTextChanged(Editable editable) {
        }
    }

    public static AlertDialog createEditDialog(final Context context, final ReadingList readingList,
                                               boolean showDescription, @NonNull final List<String> otherTitles,
                                               @NonNull final EditDialogListener listener) {
        final View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_reading_list_edit, null);
        final EditText titleView = (EditText) rootView.findViewById(R.id.reading_list_title);
        final TextInputLayout titleContainer = (TextInputLayout) rootView.findViewById(R.id.reading_list_title_container);
        final EditText descriptionView = (EditText) rootView.findViewById(R.id.reading_list_description);
        final View deleteView = rootView.findViewById(R.id.reading_list_delete_link);
        deleteView.setVisibility(showDescription ? View.VISIBLE : View.GONE);
        final SwitchCompat saveOfflineSwitch = (SwitchCompat) rootView.findViewById(R.id.reading_list_offline_switch);
        descriptionView.setVisibility(showDescription ? View.VISIBLE : View.GONE);
        saveOfflineSwitch.setVisibility(showDescription ? View.VISIBLE : View.GONE);
        titleContainer.setErrorEnabled(true);

        // TODO: move this to XML once the attribute becomes available.
        final int switchPaddingDp = 4;
        saveOfflineSwitch.setSwitchPadding((int) (switchPaddingDp * DimenUtil.getDensityScalar()));

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(rootView)
                .setPositiveButton(R.string.reading_list_edit_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeviceUtil.hideSoftKeyboard(titleView);
                        DeviceUtil.hideSoftKeyboard(descriptionView);
                        listener.onModify(titleView.getText().toString(), descriptionView.getText().toString(), saveOfflineSwitch.isChecked());
                    }
                }).setNegativeButton(R.string.reading_list_edit_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeviceUtil.hideSoftKeyboard(titleView);
                        DeviceUtil.hideSoftKeyboard(descriptionView);
                    }
                }).create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                titleView.addTextChangedListener(new TitleTextWatcher(otherTitles, new TitleTextCallback() {
                    @Override public void onEntryMatchesExistingTitle(@NonNull String title) {
                        setError(context.getString(R.string.reading_list_title_exists, title));
                    }

                    @Override public void onEntryEmpty() {
                        setError(context.getString(R.string.reading_list_entry_empty));
                    }

                    @Override public void onEntryOk() {
                        titleContainer.setError(null);
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    }

                    private void setError(@NonNull String error) {
                        titleContainer.setError(error);
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    }
                }));

                titleView.setText(readingList.getTitle());
                titleView.selectAll();

                descriptionView.setText(readingList.getDescription());
                saveOfflineSwitch.setChecked(readingList.getSaveOffline());

            }
        });

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                DeviceUtil.hideSoftKeyboard(titleView);
                DeviceUtil.hideSoftKeyboard(descriptionView);
            }
        });

        if (!showDescription) {
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        deleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                listener.onDelete();
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
