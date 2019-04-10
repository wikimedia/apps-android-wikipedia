package org.wikipedia.suggestededits;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

class SuggestedEditsTaskView extends FrameLayout {
    public interface Callback {
        void onPositiveActionClick(SuggestedEditsTask task);
        void onNegativeActionClick(SuggestedEditsTask task);
        void onViewClick(SuggestedEditsTask task);
    }
    @BindView(R.id.task_info_layout) View taskInfoLayout;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.image) ImageView image;
    @BindView(R.id.action_layout) View actionLayout;
    @BindView(R.id.disabled_action_layout) View disabledActionLayout;
    @BindView(R.id.enabled_action_layout) View enabledActionLayout;
    @BindView(R.id.disabled_text) TextView disabledTextView;
    @BindView(R.id.positive_button) TextView enabledPositiveActionButton;
    @BindView(R.id.negative_button) TextView enabledNegativeActionButton;
    private SuggestedEditsTask task;
    private Callback callback;

    SuggestedEditsTaskView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_edit_task, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    void setUpViews(SuggestedEditsTask suggestedEditsTask, Callback callback) {
        this.task = suggestedEditsTask;
        this.callback = callback;
        title.setText(suggestedEditsTask.getTitle());
        description.setText(suggestedEditsTask.getDescription());
        image.setVisibility(suggestedEditsTask.getImagePlaceHolderShown() ? VISIBLE : GONE);
        image.setImageDrawable(suggestedEditsTask.getImageDrawable());
        taskInfoLayout.setAlpha(suggestedEditsTask.getDisabled() ? 0.56f : 1.0f);
        enabledActionLayout.setVisibility(suggestedEditsTask.getDisabled() ? GONE : VISIBLE);
        disabledActionLayout.setVisibility(suggestedEditsTask.getDisabled() ? VISIBLE : GONE);
        disabledTextView.setText(suggestedEditsTask.getDisabledDescriptionText());
        enabledPositiveActionButton.setText(suggestedEditsTask.getEnabledPositiveActionString());
        enabledNegativeActionButton.setText(suggestedEditsTask.getEnabledNegativeActionString());
        actionLayout.setVisibility(suggestedEditsTask.getNoActionLayout() ? GONE : VISIBLE);
    }

    @OnClick(R.id.positive_button)
    void onPositiveClick(View v) {
        if (callback != null) {
            callback.onPositiveActionClick(task);
        }
    }

    @OnClick(R.id.negative_button)
    void onNegativeClick(View v) {
        if (callback != null) {
            callback.onNegativeActionClick(task);
        }
    }

    @OnClick(R.id.task_info_layout)
    void onClick(View v) {
        if (callback != null && !task.getDisabled()) {
            callback.onViewClick(task);
        }
    }
}
