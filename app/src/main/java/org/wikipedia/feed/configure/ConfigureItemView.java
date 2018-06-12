package org.wikipedia.feed.configure;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.FeedContentType;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class ConfigureItemView extends FrameLayout {
    public interface Callback {
        void onCheckedChanged(FeedContentType contentType, boolean checked);
        void onLanguagesChanged(FeedContentType contentType);
    }

    @BindView(R.id.feed_content_type_checkbox) SwitchCompat onSwitch;
    @BindView(R.id.feed_content_type_title) TextView titleView;
    @BindView(R.id.feed_content_type_subtitle) TextView subtitleView;
    @BindView(R.id.feed_content_type_drag_handle) View dragHandleView;
    @BindView(R.id.feed_content_type_lang_list_container) View langListContainer;
    @BindView(R.id.feed_content_type_lang_list) RecyclerView langRecyclerView;
    @Nullable private Callback callback;
    private FeedContentType contentType;
    private LanguageItemAdapter adapter;

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

    public void setContents(@NonNull FeedContentType contentType) {
        this.contentType = contentType;
        titleView.setText(contentType.titleId());
        subtitleView.setText(contentType.subtitleId());
        onSwitch.setChecked(contentType.isEnabled());

        if (contentType.isPerLanguage() && WikipediaApp.getInstance().language().getAppLanguageCodes().size() > 1) {
            langListContainer.setVisibility(VISIBLE);
            adapter = new LanguageItemAdapter(getContext(), contentType);
            langRecyclerView.setAdapter(adapter);
        } else {
            langListContainer.setVisibility(GONE);
        }
    }

    public void setDragHandleTouchListener(OnTouchListener listener) {
        dragHandleView.setOnTouchListener(listener);
    }

    private void init() {
        inflate(getContext(), R.layout.item_feed_content_type, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        langRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    @OnCheckedChanged(R.id.feed_content_type_checkbox) void onCheckedChanged(boolean checked) {
        if (callback != null) {
            callback.onCheckedChanged(contentType, checked);
        }
    }

    @OnClick(R.id.feed_content_type_lang_list_click_target) void onLangClick(View v) {
        showLangSelectDialog();
    }

    private void showLangSelectDialog() {
        ConfigureItemLanguageDialogView view = new ConfigureItemLanguageDialogView(getContext());
        List<String> tempDisabledList = new ArrayList<>(contentType.getLangCodesDisabled());
        view.setContentType(adapter.getLangList(), tempDisabledList);
        new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(contentType.titleId())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    contentType.getLangCodesDisabled().clear();
                    contentType.getLangCodesDisabled().addAll(tempDisabledList);
                    adapter.notifyDataSetChanged();
                    if (callback != null) {
                        callback.onLanguagesChanged(contentType);
                    }
                    boolean atLeastOneEnabled = false;
                    for (String lang : adapter.getLangList()) {
                        if (!tempDisabledList.contains(lang)) {
                            atLeastOneEnabled = true;
                            break;
                        }
                    }
                    onSwitch.setChecked(atLeastOneEnabled);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }
}
