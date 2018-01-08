package org.wikipedia.offline;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.FileUtil.bytesToGB;
import static org.wikipedia.util.FileUtil.bytesToUserVisibleUnit;

public class DiskUsageView extends LinearLayout {
    @BindView(R.id.view_disk_usage_size_text) TextView sizeText;
    @BindView(R.id.view_disk_usage_text_free) TextView usageFreeText;
    @BindView(R.id.view_disk_usage_bar_other) View otherBar;
    @BindView(R.id.view_disk_usage_bar_other_separator) View otherSeparator;
    @BindView(R.id.view_disk_usage_bar_used) View usedBar;
    @BindView(R.id.view_disk_usage_bar_used_separator) View usedSeparator;
    @BindView(R.id.view_disk_usage_bar_free) View freeBar;

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
        long availableBytes = path.getFreeSpace();
        float availableGB = bytesToGB(availableBytes);
        float otherGB = bytesToGB(path.getTotalSpace());
        float usedGB = bytesToGB(usedBytes);
        otherGB -= usedGB;

        sizeText.setText(bytesToUserVisibleUnit(getContext(), usedBytes));
        usageFreeText.setText(getResources().getString(R.string.storage_size_free_v2, bytesToUserVisibleUnit(getContext(), availableBytes)));

        setUsageBarWeight(usedBar, usedGB);
        usedSeparator.setVisibility(usedGB > 0f ? VISIBLE : GONE);
        setUsageBarWeight(otherBar, otherGB);
        otherSeparator.setVisibility(otherGB > 0f ? VISIBLE : GONE);
        setUsageBarWeight(freeBar, availableGB);
    }

    private void init() {
        inflate(getContext(), R.layout.view_disk_usage, this);
        ButterKnife.bind(this);
        setOrientation(VERTICAL);
        update(0);
    }

    private void setUsageBarWeight(@NonNull View barView, float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) barView.getLayoutParams();
        params.weight = weight;
        barView.setLayoutParams(params);
    }
}
