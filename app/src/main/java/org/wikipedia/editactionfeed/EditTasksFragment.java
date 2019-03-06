package org.wikipedia.editactionfeed;

import static org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TITLE_DESC_SOURCE;
import static org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.descriptions.DescriptionEditHelpActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.views.CircularProgressBar;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.FooterMarginItemDecoration;
import org.wikipedia.views.HeaderMarginItemDecoration;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class EditTasksFragment extends Fragment {
    private Unbinder unbinder;
    @BindView(R.id.edit_onboarding_view) View editOnboardingView;
    @BindView(R.id.circular_progress_bar) CircularProgressBar circularProgressBar;
    @BindView(R.id.level_text) TextView levelText;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.edit_count) TextView editCount;
    @BindView(R.id.contributions_text) TextView contributionsText;
    @BindView(R.id.task_recyclerview) RecyclerView tasksRecyclerView;
    private List<EditTask> tasks = new ArrayList<>();
    private List<EditTaskView.Callback> callbacks = new ArrayList<>();


    public static EditTasksFragment newInstance() {
        return new EditTasksFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_tasks, container, false);
        unbinder = ButterKnife.bind(this, view);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(0f);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        showOneTimeOnboarding();
        updateUI();
        setUpRecyclerView();
        return view;
    }

    private void setUpRecyclerView() {
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        final int topDecorationDp = 16;
        tasksRecyclerView.addItemDecoration(new HeaderMarginItemDecoration(topDecorationDp, 0));
        tasksRecyclerView.addItemDecoration(new FooterMarginItemDecoration(0, topDecorationDp));

        setUpTasks();
        RecyclerAdapter adapter = new RecyclerAdapter(tasks);
        tasksRecyclerView.setAdapter(adapter);
    }

    class RecyclerAdapter extends DefaultRecyclerAdapter<EditTask, EditTaskView> {

        RecyclerAdapter(@NonNull List<EditTask> tasks) {
            super(tasks);
        }

        @NonNull
        @Override public DefaultViewHolder<EditTaskView> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DefaultViewHolder<>(new EditTaskView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder<EditTaskView> holder, int i) {
            holder.getView().setUpViews(items().get(i), callbacks.get(i));
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        updateRecycler();
    }

    private void updateRecycler() {
        if (Prefs.isEditActionTranslateDescriptionsUnlocked() && tasks.size() > 2 && tasksRecyclerView.getAdapter() != null) {
            tasks.get(1).setDisabled(false);
            tasks.get(1).setNoActionLayout(Prefs.isEditActionTranslateDescriptionsUnlocked());
            tasksRecyclerView.getAdapter().notifyItemChanged(1);
        }
    }

    private void updateUI() {
        circularProgressBar.setCurrentProgress(Prefs.getTotalUserDescriptionsEdited());
        levelText.setText(String.format(getString(R.string.editing_level_text), 1)); //Todo: derive the level number dynamically by making use of future gaming logic
        username.setText(AccountUtil.getUserName());
        editCount.setText(String.valueOf(Prefs.getTotalUserDescriptionsEdited()));
        contributionsText.setText(getResources().getQuantityString(R.plurals.edit_action_contribution_count, Prefs.getTotalUserDescriptionsEdited()));
    }

    private void showOneTimeOnboarding() {
        if (Prefs.showEditTaskOnboarding()) {
            editOnboardingView.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setUpTasks() {
        EditTask titleDescriptionEditTask = new EditTask();
        titleDescriptionEditTask.setTitle(getString(R.string.title_description_task_title));
        titleDescriptionEditTask.setDescription(getString(R.string.title_description_task_description));
        titleDescriptionEditTask.setImagePlaceHolderShown(true);
        titleDescriptionEditTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_short_text_white_24dp));
        titleDescriptionEditTask.setNoActionLayout(true);
        tasks.add(titleDescriptionEditTask);
        callbacks.add(new EditTaskView.Callback() {
            @Override
            public void onPositiveActionClick() {
            }

            @Override
            public void onNegativeActionClick() {
            }

            @Override
            public void onViewClick() {
                startActivity(AddTitleDescriptionsActivity.Companion.newIntent(requireActivity(), EDIT_TASKS_TITLE_DESC_SOURCE));
            }
        });

        EditTask multilingualTask = new EditTask();
        multilingualTask.setTitle(getString(R.string.translation_task_title));
        multilingualTask.setDescription(getString(R.string.translation_task_description));
        multilingualTask.setImagePlaceHolderShown(true);
        multilingualTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_translate_title_descriptions));
        multilingualTask.setNoActionLayout(Prefs.isEditActionTranslateDescriptionsUnlocked());
        multilingualTask.setDisabled(!Prefs.isEditActionTranslateDescriptionsUnlocked());
        multilingualTask.setDisabledDescriptionText(String.format(getString(R.string.translate_description_edit_disable_text), 2));
        tasks.add(multilingualTask);
        callbacks.add(new EditTaskView.Callback() {
            @Override
            public void onPositiveActionClick() {
            }

            @Override
            public void onNegativeActionClick() {
            }

            @Override
            public void onViewClick() {
                if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() > 1 && !multilingualTask.getDisabled()) {
                    startActivity(AddTitleDescriptionsActivity.Companion.newIntent(requireActivity(), EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE));
                }
            }
        });

        EditTask imageCaptionEditTask = new EditTask();
        imageCaptionEditTask.setTitle(getString(R.string.image_caption_task_title));
        imageCaptionEditTask.setDescription(getString(R.string.image_caption_task_description));
        imageCaptionEditTask.setImagePlaceHolderShown(true);
        imageCaptionEditTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_caption_images));
        imageCaptionEditTask.setDisabled(true);
        imageCaptionEditTask.setDisabledDescriptionText(String.format(getString(R.string.image_caption_edit_disable_text), 50));
        tasks.add(imageCaptionEditTask);
        callbacks.add(null);

        EditTask imageCaptionTranslationEditTask = new EditTask();
        imageCaptionTranslationEditTask.setTitle(getString(R.string.translate_caption_task_title));
        imageCaptionTranslationEditTask.setDescription(getString(R.string.translate_caption_task_description));
        imageCaptionTranslationEditTask.setImagePlaceHolderShown(true);
        imageCaptionTranslationEditTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_translate_title_descriptions));
        imageCaptionTranslationEditTask.setDisabled(true);
        imageCaptionTranslationEditTask.setDisabledDescriptionText(String.format(getString(R.string.image_caption_edit_disable_text), 50));
        tasks.add(imageCaptionTranslationEditTask);
        callbacks.add(null);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_tasks, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                startActivity(DescriptionEditHelpActivity.newIntent(requireContext()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.get_started_button)
    void onGetStartedClicked() {
        Prefs.setShowEditTasksOnboarding(false);
        editOnboardingView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
    }
}
