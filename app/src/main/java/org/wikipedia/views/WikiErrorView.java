package org.wikipedia.views;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.ThrowableUtil.is404;

public class WikiErrorView extends LinearLayout {
    @BindView(R.id.view_wiki_error_icon) ImageView icon;
    @BindView(R.id.view_wiki_error_text) TextView errorText;
    @BindView(R.id.view_wiki_error_button) TextView button;

    @Nullable private OnClickListener retryListener;
    @Nullable private OnClickListener backListener;

    public WikiErrorView(Context context) {
        this(context, null);
    }

    public WikiErrorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WikiErrorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflate(context, R.layout.view_wiki_error, this);
        ButterKnife.bind(this);
    }

    @Nullable public OnClickListener getRetryListener() {
        return retryListener;
    }

    public void setRetryClickListener(@Nullable OnClickListener listener) {
        retryListener = listener;
    }

    @Nullable public OnClickListener getBackListener() {
        return backListener;
    }

    public void setBackClickListener(@Nullable OnClickListener listener) {
        backListener = listener;
    }

    public void setError(@Nullable Throwable caught) {
        Resources resources = getContext().getResources();
        ErrorType errorType = caught != null && is404(getContext(), caught) ? ErrorType.GENERIC : ErrorType.OFFLINE;
        icon.setImageDrawable(ContextCompat.getDrawable(getContext(), errorType.icon()));
        errorText.setText(resources.getString(errorType.text()));
        button.setText(resources.getString(errorType.buttonText()));
        button.setOnClickListener(errorType.buttonClickListener(this));
    }

    private enum ErrorType {
        OFFLINE(R.drawable.ic_portable_wifi_off_black_24px, R.string.view_wiki_error_message_offline,
                R.string.page_error_retry) {
            @Nullable @Override
            OnClickListener buttonClickListener(@NonNull WikiErrorView errorView) {
                return errorView.getRetryListener();
            }
        },

        GENERIC(R.drawable.ic_error_black_24dp, R.string.error_message_generic,
                R.string.page_error_back_to_main) {
            @Nullable @Override
            OnClickListener buttonClickListener(@NonNull WikiErrorView errorView) {
                return errorView.getBackListener();
            }
        };

        @DrawableRes private int icon;
        @StringRes private int text;
        @StringRes private int buttonText;
        @StringRes private int footerText;

        @DrawableRes int icon() {
            return icon;
        }

        @StringRes int text() {
            return text;
        }

        @StringRes int buttonText() {
            return buttonText;
        }

        @StringRes int footerText() {
            return footerText;
        }

        @Nullable abstract OnClickListener buttonClickListener(@NonNull WikiErrorView errorView);

        ErrorType(@DrawableRes int icon, @StringRes int text, @StringRes int buttonText) {
            this.icon = icon;
            this.text = text;
            this.buttonText = buttonText;
        }

        ErrorType(@DrawableRes int icon, @StringRes int text, @StringRes int buttonText, @StringRes int footerText) {
            this.icon = icon;
            this.text = text;
            this.buttonText = buttonText;
            this.footerText = footerText;
        }
    }
}
