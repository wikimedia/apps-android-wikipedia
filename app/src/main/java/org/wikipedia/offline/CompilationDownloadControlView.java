package org.wikipedia.offline;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.util.FeedbackUtil;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.util.FileUtil.bytesToUserVisibleUnit;

public class CompilationDownloadControlView extends LinearLayout {
    @BindView(R.id.compilation_download_widget_progress_text) TextView progressText;
    @BindView(R.id.compilation_download_widget_progress_time_remaining) TextView timeRemainingText;
    @BindView(R.id.compilation_download_progress) ProgressBar progressBar;
    @BindView(R.id.compilation_download_widget_button_cancel) ImageView cancelButton;

    @Nullable private Callback callback;

    public interface Callback {
        void onCancel();
    }

    public static boolean shouldShowControls(@Nullable DownloadManagerItem item) {
        return item != null && (item.status() == DownloadManager.STATUS_PENDING
                || item.status() == DownloadManager.STATUS_RUNNING
                || item.status() == DownloadManager.STATUS_PAUSED);
    }

    public CompilationDownloadControlView(Context context) {
        super(context);
        init();
    }

    public CompilationDownloadControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompilationDownloadControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CompilationDownloadControlView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void update(@Nullable DownloadManagerItem item) {
        if (item == null) {
            return;
        }
        if (item.status() == DownloadManager.STATUS_RUNNING) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress((int) (item.bytesDownloaded()
                    * Constants.PROGRESS_BAR_MAX_VALUE / item.bytesTotal()));
            timeRemainingText.setVisibility(VISIBLE);
        } else {
            progressBar.setIndeterminate(true);
            timeRemainingText.setVisibility(GONE);
        }
        progressText.setText(getString(R.string.offline_compilation_download_progress_text_v2,
                bytesToUserVisibleUnit(getContext(), item.bytesDownloaded()), bytesToUserVisibleUnit(getContext(), item.bytesTotal())));
        long bytesPerMin = item.bytesPerSec() * TimeUnit.MINUTES.toSeconds(1);
        if (bytesPerMin >= 0) {
            long minsRemaining = (item.bytesTotal() - item.bytesDownloaded()) / bytesPerMin;
            timeRemainingText.setText(getQuantityString(R.plurals.offline_compilation_download_time_remaining,
                    (int) minsRemaining, minsRemaining));
        }
    }

    @OnClick(R.id.compilation_download_widget_button_cancel)
    void onCancelClicked() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.compilation_download_cancel_confirm)
                .setPositiveButton(R.string.compilation_download_cancel_confirm_yes, (dialog, i) -> {
                    if (callback != null) {
                        callback.onCancel();
                    }
                })
                .setNegativeButton(R.string.compilation_download_cancel_confirm_no, null)
                .show();
    }

    private void init() {
        inflate(getContext(), R.layout.view_compilation_download_widget, this);
        ButterKnife.bind(this);
        progressBar.setMax(Constants.PROGRESS_BAR_MAX_VALUE);
        FeedbackUtil.setToolbarButtonLongPressToast(cancelButton);
    }

    private String getQuantityString(int id, int amt, Object... formatArgs) {
        return getResources().getQuantityString(id, amt, formatArgs);
    }

    private String getString(int id, Object... formatArgs) {
        return getResources().getString(id, formatArgs);
    }
}
