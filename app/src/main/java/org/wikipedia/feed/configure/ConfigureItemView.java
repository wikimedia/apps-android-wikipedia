package org.wikipedia.feed.configure;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.databinding.ItemFeedContentTypeBinding;
import org.wikipedia.feed.FeedContentType;

import java.util.ArrayList;
import java.util.List;

public class ConfigureItemView extends FrameLayout {
    public interface Callback {
        void onCheckedChanged(FeedContentType contentType, boolean checked);
        void onLanguagesChanged(FeedContentType contentType);
    }

    private SwitchCompat onSwitch;
    private TextView titleView;
    private TextView subtitleView;
    private View dragHandleView;
    private View langListContainer;
    private RecyclerView langRecyclerView;
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
        final ItemFeedContentTypeBinding binding =
                ItemFeedContentTypeBinding.inflate(LayoutInflater.from(getContext()));
        onSwitch = binding.feedContentTypeCheckbox;
        titleView = binding.feedContentTypeTitle;
        subtitleView = binding.feedContentTypeSubtitle;
        dragHandleView = binding.feedContentTypeDragHandle;
        langListContainer = binding.feedContentTypeLangListContainer;
        langRecyclerView = binding.feedContentTypeLangList;

        onSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (callback != null) {
                callback.onCheckedChanged(contentType, isChecked);
            }
        }));
        binding.feedContentTypeLangListClickTarget.setOnClickListener(v -> showLangSelectDialog());

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        langRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void showLangSelectDialog() {
        ConfigureItemLanguageDialogView view = new ConfigureItemLanguageDialogView(getContext());
        List<String> tempDisabledList = new ArrayList<>(contentType.getLangCodesDisabled());
        view.setContentType(adapter.getLangList(), tempDisabledList);
        new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(contentType.titleId())
                .setPositiveButton(R.string.customize_lang_selection_dialog_ok_button_text, (dialog, which) -> {
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
                .setNegativeButton(R.string.customize_lang_selection_dialog_cancel_button_text, null)
                .create()
                .show();
    }
}
