package org.wikipedia.suggestededits;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.wikipedia.R;

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
    @BindView(R.id.unlock_message_container) View unlockMessageContainer;
    @BindView(R.id.unlock_message_text) TextView unlockMessageText;
    @BindView(R.id.unlock_actions_container) View unlockActionContainer;
    @BindView(R.id.unlock_action_positive_button) TextView unlockActionPositiveButton;
    @BindView(R.id.unlock_action_negative_button) TextView unlockActionNegativeButton;
    private SuggestedEditsTask task;
    private Callback callback;

    SuggestedEditsTaskView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_suggested_edits_task, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    void setUpViews(SuggestedEditsTask suggestedEditsTask, Callback callback) {
        this.task = suggestedEditsTask;
        this.callback = callback;
        title.setText(suggestedEditsTask.getTitle());
        description.setText(suggestedEditsTask.getDescription());
        image.setVisibility(suggestedEditsTask.getShowImagePlaceholder() ? VISIBLE : GONE);
        image.setImageDrawable(suggestedEditsTask.getImageDrawable());
        taskInfoLayout.setAlpha(suggestedEditsTask.getDisabled() ? 0.56f : 1.0f);
        unlockMessageContainer.setVisibility(suggestedEditsTask.getDisabled() ? VISIBLE : GONE);
        unlockMessageText.setText(suggestedEditsTask.getUnlockMessageText());
        unlockActionContainer.setVisibility(suggestedEditsTask.getDisabled() ? GONE : VISIBLE);
        unlockActionPositiveButton.setText(suggestedEditsTask.getUnlockActionPositiveButtonString());
        unlockActionNegativeButton.setText(suggestedEditsTask.getUnlockActionNegativeButtonString());
        actionLayout.setVisibility(suggestedEditsTask.getShowActionLayout() ? VISIBLE : GONE);
    }

    @OnClick(R.id.unlock_action_positive_button)
    void onPositiveClick(View v) {
        if (callback != null) {
            callback.onPositiveActionClick(task);
        }
    }

    @OnClick(R.id.unlock_action_negative_button)
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
