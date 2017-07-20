package org.wikipedia.offline;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.ResourceUtil;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DiskUsageView extends LinearLayout {
    private static final int KILOBYTE = 1000;

    @BindView(R.id.view_disk_usage_size_text) TextView sizeText;
    @BindView(R.id.view_disk_usage_text_app) TextView usageAppText;
    @BindView(R.id.view_disk_usage_text_free) TextView usageFreeText;
    @BindView(R.id.view_disk_usage_bar_other) View otherBar;
    @BindView(R.id.view_disk_usage_bar_used) View usedBar;
    @BindView(R.id.view_disk_usage_bar_free) View freeBar;
    @BindView(R.id.view_disk_usage_dot_other) View otherDot;
    @BindView(R.id.view_disk_usage_dot_used) View usedDot;
    @BindView(R.id.view_disk_usage_dot_free) View freeDot;

    public DiskUsageView(Context context) {
        super(context);
        init();
    }

    public DiskUsageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DiskUsageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DiskUsageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void update(long usedBytes) {
        File path = Environment.getDataDirectory();
        float availableGB = bytesToGB(path.getFreeSpace());
        float otherGB = bytesToGB(path.getTotalSpace());
        float usedGB = bytesToGB(usedBytes);
        otherGB -= usedGB;

        sizeText.setText(getResources().getString(R.string.storage_size_format, usedGB));
        usageFreeText.setText(getResources().getString(R.string.storage_size_free, availableGB));

        setUsageBarWeight(otherBar, otherGB);
        setUsageBarWeight(freeBar, availableGB);
        setUsageBarWeight(usedBar, usedGB);
    }

    private void init() {
        inflate(getContext(), R.layout.view_disk_usage, this);
        ButterKnife.bind(this);
        setOrientation(VERTICAL);

        usageAppText.setText(R.string.app_name);
        setDotTint(otherDot, R.attr.window_inverse_color);
        setDotTint(usedDot, R.attr.colorAccent);
        setDotTint(freeDot, R.attr.window_background_color);

        update(0);
    }

    private void setUsageBarWeight(@NonNull View barView, float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) barView.getLayoutParams();
        params.weight = weight;
        barView.setLayoutParams(params);
    }

    private void setDotTint(@NonNull View dotView, @AttrRes int id) {
        ViewCompat.setBackgroundTintList(dotView, new ColorStateList(new int[][]{new int[]{}},
                new int[]{ResourceUtil.getThemedColor(getContext(), id)}));
    }

    private float bytesToGB(long bytes) {
        return (float) bytes / KILOBYTE / KILOBYTE / KILOBYTE;
    }
}
