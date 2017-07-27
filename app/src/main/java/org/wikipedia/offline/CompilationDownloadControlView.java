package org.wikipedia.offline;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.util.FileUtil.bytesToGB;

public class CompilationDownloadControlView extends LinearLayout {
    @BindView(R.id.compilation_download_widget_progress_text) TextView progressText;
    @BindView(R.id.compilation_download_widget_progress_time_remaining) TextView timeRemainingText;
    @BindView(R.id.compilation_download_progress) ProgressBar progressBar;
    @BindView(R.id.compilation_download_widget_button_pause_resume) ImageView pauseResumeButton;

    private Compilation comp;
    private boolean downloading;

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

    void setCompilation(@NonNull Compilation comp) {
        this.comp = comp;
        updateViews(0f, 0);
    }

    private void updateViews(float amtDownloaded, int minsRemaining) {
        progressText.setText(getString(R.string.offline_compilation_download_progress_text,
                amtDownloaded, bytesToGB(comp.size())));
        timeRemainingText.setText(getQuantityString(R.plurals.offline_compilation_download_time_remaining,
                minsRemaining, minsRemaining));
    }

    @OnClick(R.id.compilation_download_widget_button_pause_resume)
    void onPlayPauseToggleClicked() {
        togglePlayPause();
    }

    @OnClick(R.id.compilation_download_widget_button_cancel)
    void onCancelClicked() {
        // cancelDownload();
    }

    private void togglePlayPause() {
        if (downloading) {
            // pauseDownload();
        } else {
            // resumeDownload();
        }
        downloading = !downloading;
        pauseResumeButton.setImageDrawable(downloading ? getPauseIcon() : getResumeIcon());
        updateProgressBar();
    }

    private void init() {
        inflate(getContext(), R.layout.view_compilation_download_widget, this);
        ButterKnife.bind(this);
    }

    private void updateProgressBar() {
        progressBar.setIndeterminate(downloading);
    }

    private Drawable getPauseIcon() {
        return ContextCompat.getDrawable(getContext(), R.drawable.ic_pause_white_24px);
    }

    private Drawable getResumeIcon() {
        return ContextCompat.getDrawable(getContext(), R.drawable.ic_play_arrow_white_24px);
    }

    private String getQuantityString(int id, int amt, Object... formatArgs) {
        return getResources().getQuantityString(id, amt, formatArgs);
    }

    private String getString(int id, Object... formatArgs) {
        return getResources().getString(id, formatArgs);
    }
}
