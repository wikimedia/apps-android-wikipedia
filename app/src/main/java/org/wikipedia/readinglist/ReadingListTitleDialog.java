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
        void onSuccess(@NonNull String text, @NonNull String description);
    }

    public static TextInputDialog readingListTitleDialog(@NonNull Context context,
                                                         @NonNull final String title,
                                                         @Nullable final String description,
                                                         @NonNull final List<String> otherTitles,
                                                         @Nullable final Callback callback) {
        return TextInputDialog.newInstance(context, true,
                new TextInputDialog.Callback() {
                    @Override
                    public void onShow(@NonNull TextInputDialog dialog) {
                        dialog.setHint(R.string.reading_list_name_hint);
                        dialog.setSecondaryHint(R.string.reading_list_description_hint);
                        dialog.setText(title);
                        dialog.setSecondaryText(StringUtils.defaultString(description));
                    }

                    @Override
                    public void onTextChanged(@NonNull CharSequence text, @NonNull TextInputDialog dialog) {
                        String title = text.toString().trim();
                        if (StringUtils.isEmpty(title)) {
                            dialog.setError(null);
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
                    public void onSuccess(@NonNull CharSequence text, @NonNull CharSequence secondaryText) {
                        if (callback != null) {
                            callback.onSuccess(text.toString().trim(), secondaryText.toString().trim());
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
