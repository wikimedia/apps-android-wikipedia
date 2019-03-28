package org.wikipedia.editactionfeed;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

class EditTaskView extends FrameLayout {
    public interface Callback {
        void onPositiveActionClick(EditTask task);
        void onNegativeActionClick(EditTask task);
        void onViewClick(EditTask task);
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
    private EditTask task;
    private Callback callback;

    EditTaskView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_edit_task, this);
        ButterKnife.bind(this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    void setUpViews(EditTask editTask, Callback callback) {
        this.task = editTask;
        this.callback = callback;
        title.setText(editTask.getTitle());
        description.setText(editTask.getDescription());
        image.setVisibility(editTask.getImagePlaceHolderShown() ? VISIBLE : GONE);
        image.setImageDrawable(editTask.getImageDrawable());
        taskInfoLayout.setAlpha(editTask.getDisabled() ? 0.56f : 1.0f);
        enabledActionLayout.setVisibility(editTask.getDisabled() ? GONE : VISIBLE);
        disabledActionLayout.setVisibility(editTask.getDisabled() ? VISIBLE : GONE);
        disabledTextView.setText(editTask.getDisabledDescriptionText());
        enabledPositiveActionButton.setText(editTask.getEnabledPositiveActionString());
        enabledNegativeActionButton.setText(editTask.getEnabledNegativeActionString());
        actionLayout.setVisibility(editTask.getNoActionLayout() ? GONE : VISIBLE);
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
