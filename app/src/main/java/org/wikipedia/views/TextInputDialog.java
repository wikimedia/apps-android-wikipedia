package org.wikipedia.views;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.wikipedia.R;
import org.wikipedia.util.DeviceUtil;

public final class TextInputDialog extends AlertDialog {

    public interface Callback {
        void onShow(@NonNull TextInputDialog dialog);
        void onTextChanged(@NonNull CharSequence text, @NonNull TextInputDialog dialog);
        void onSuccess(@NonNull CharSequence text);
        void onCancel();
    }

    @Nullable private Callback callback;
    private EditText editText;
    private TextInputLayout editTextContainer;
    private TextInputWatcher watcher = new TextInputWatcher();

    public static TextInputDialog newInstance(@NonNull Context context,
                                              @Nullable final Callback callback) {
        return new TextInputDialog(context)
                .setView(R.layout.dialog_text_input)
                .setCallback(callback);
    }

    public TextInputDialog setCallback(@Nullable Callback callback) {
        this.callback = callback;
        return this;
    }

    public TextInputDialog setView(@LayoutRes int id) {
        View rootView = LayoutInflater.from(getContext()).inflate(id, null);
        editText = rootView.findViewById(R.id.text_input);
        editTextContainer = rootView.findViewById(R.id.text_input_container);
        super.setView(rootView);

        editTextContainer.setErrorEnabled(true);
        return this;
    }

    public void setText(@Nullable CharSequence text) {
        editText.setText(text);
    }

    public void setHint(@StringRes int id) {
        editTextContainer.setHint(getContext().getResources().getString(id));
    }

    public void selectAll() {
        editText.selectAll();
    }

    public void setError(@Nullable CharSequence text) {
        editTextContainer.setError(text);
    }

    public void setPositiveButtonEnabled(boolean enabled) {
        getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
    }

    @Override public void onAttachedToWindow() {
        editText.addTextChangedListener(watcher);
    }

    @Override public void onDetachedFromWindow() {
        editText.removeTextChangedListener(watcher);
    }

    public static class DefaultCallback implements Callback {
        @Override
        public void onShow(@NonNull TextInputDialog dialog) {
        }

        @Override
        public void onTextChanged(@NonNull CharSequence text, @NonNull TextInputDialog dialog) {
        }

        @Override
        public void onSuccess(@NonNull CharSequence text) {
        }

        @Override
        public void onCancel() {
        }
    }

    private TextInputDialog(@NonNull Context context) {
        super(context);

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok),
                (dialog,  which) -> {
                    //DeviceUtil.hideSoftKeyboard(editText);
                    if (callback != null) {
                        callback.onSuccess(editText.getText());
                    }
                });

        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog,  which) -> {
                    //DeviceUtil.hideSoftKeyboard(editText);
                    if (callback != null) {
                        callback.onCancel();
                    }
                });

        setOnShowListener((dialog) -> {
            editText.requestFocus();
            DeviceUtil.showSoftKeyboard(editText);
            if (callback != null) {
                callback.onShow(TextInputDialog.this);
            }
        });

        setOnDismissListener((dialog) -> {
                //DeviceUtil.hideSoftKeyboard(editText);
        });
    }

    private class TextInputWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (callback != null) {
                callback.onTextChanged(charSequence, TextInputDialog.this);
            }
        }

        @Override public void afterTextChanged(Editable editable) {
        }
    }
}
