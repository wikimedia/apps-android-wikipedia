package org.wikipedia.feed.configure;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.FeedContentType;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class ConfigureItemView extends FrameLayout {
    public interface Callback {
        void onCheckedChanged(FeedContentType contentType, boolean checked);
    }

    @BindView(R.id.feed_content_type_checkbox) CheckBox checkBox;
    @BindView(R.id.feed_content_type_title) TextView titleView;
    @BindView(R.id.feed_content_type_subtitle) TextView subtitleView;
    @BindView(R.id.feed_content_type_drag_handle) View dragHandleView;
    @Nullable private Callback callback;
    private FeedContentType contentType;

    public ConfigureItemView(Context context) {
        super(context);
        init();
    }

    public ConfigureItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConfigureItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setContents(FeedContentType contentType) {
        this.contentType = contentType;
        titleView.setText(contentType.titleId());
        subtitleView.setText(contentType.subtitleId());
        checkBox.setChecked(contentType.isEnabled());
    }

    public void setDragHandleTouchListener(OnTouchListener listener) {
        dragHandleView.setOnTouchListener(listener);
    }

    private void init() {
        inflate(getContext(), R.layout.item_feed_content_type, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @OnCheckedChanged(R.id.feed_content_type_checkbox) void onCheckedChanged(boolean checked) {
        if (callback != null) {
            callback.onCheckedChanged(contentType, checked);
        }
    }
}
