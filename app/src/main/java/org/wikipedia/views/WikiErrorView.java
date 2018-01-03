package org.wikipedia.views;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.ThrowableUtil.is404;
import static org.wikipedia.util.ThrowableUtil.isOffline;

public class WikiErrorView extends LinearLayout {
    @BindView(R.id.view_wiki_error_icon) ImageView icon;
    @BindView(R.id.view_wiki_error_text) TextView errorText;
    @BindView(R.id.view_wiki_error_button) TextView button;
    @BindView(R.id.view_wiki_error_footer_text) TextView footerText;
    @BindView(R.id.view_wiki_error_article_content_top_offset) Space contentTopOffset;
    @BindView(R.id.view_wiki_error_article_tab_layout_offset) Space tabLayoutOffset;
    @BindView(R.id.view_wiki_error_footer_layout) View footerLayout;

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
        ErrorType errorType = getErrorType(caught);
        icon.setImageDrawable(ContextCompat.getDrawable(getContext(), errorType.icon()));
        errorText.setText(resources.getString(errorType.text()));
        button.setText(resources.getString(errorType.buttonText()));
        button.setOnClickListener(errorType.buttonClickListener(this));
        if (errorType.hasFooterText()) {
            footerLayout.setVisibility(VISIBLE);
            footerText.setText(resources.getString(errorType.footerText()));
        } else {
            footerLayout.setVisibility(GONE);
        }
    }

    ErrorType getErrorType(@Nullable Throwable caught) {
        if (caught != null && is404(caught)) {
            return ErrorType.PAGE_MISSING;
        }
        if (caught != null && isOffline(caught)) {
            return ErrorType.OFFLINE;
        }
        return ErrorType.GENERIC;
    }

    enum ErrorType {
        PAGE_MISSING(R.drawable.ic_error_black_24dp, R.string.error_page_does_not_exist,
                R.string.page_error_back_to_main) {
            @Nullable @Override
            OnClickListener buttonClickListener(@NonNull WikiErrorView errorView) {
                return errorView.getBackListener();
            }
        },


        PAGE_OFFLINE(R.drawable.ic_no_article, R.string.page_offline_notice_cannot_load_while_offline,
                R.string.page_error_retry, R.string.page_offline_notice_add_to_reading_list) {
            @Nullable @Override
            OnClickListener buttonClickListener(@NonNull WikiErrorView errorView) {
                return errorView.getRetryListener();
            }
        },

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

        boolean hasFooterText() {
            return footerText != 0;
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
