package org.wikipedia.readinglist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.views.TextInputDialog;

import java.util.List;

public final class ReadingListTitleDialog {

    public interface Callback {
        void onSuccess(@NonNull CharSequence text);
    }

    public static TextInputDialog readingListTitleDialog(@NonNull Context context,
                                                         @NonNull final String initialTitle,
                                                         @NonNull final List<String> otherTitles,
                                                         @Nullable final Callback callback) {
        return TextInputDialog.newInstance(context,
                new TextInputDialog.Callback() {
                    @Override
                    public void onShow(@NonNull TextInputDialog dialog) {
                        dialog.setHint(R.string.reading_list_name_hint);
                        dialog.setText(initialTitle);
                        dialog.selectAll();
                    }

                    @Override
                    public void onTextChanged(@NonNull CharSequence text, @NonNull TextInputDialog dialog) {
                        String title = text.toString().trim();
                        if (StringUtils.isEmpty(title)) {
                            dialog.setError(dialog.getContext().getString(R.string.reading_list_entry_empty));
                            dialog.setPositiveButtonEnabled(false);
                        } else if (otherTitles.contains(title)) {
                            dialog.setError(dialog.getContext().getString(R.string.reading_list_title_exists, title));
                            dialog.setPositiveButtonEnabled(false);
                        } else {
                            dialog.setError(null);
                            dialog.setPositiveButtonEnabled(true);
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull CharSequence text) {
                        if (callback != null) {
                            callback.onSuccess(text.toString().trim());
                        }
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    private ReadingListTitleDialog() {
    }
}
