package org.wikipedia.editactionfeed;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.EditorTaskCounts;
import org.wikipedia.language.LanguageSettingsInvokeSource;
import org.wikipedia.notifications.NotificationEditorTasksHandler;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.languages.WikipediaLanguagesActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.FooterMarginItemDecoration;
import org.wikipedia.views.HeaderMarginItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE;
import static org.wikipedia.Constants.InvokeSource;
import static org.wikipedia.Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC;
import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;

public class EditTasksFragment extends Fragment {
    private Unbinder unbinder;
    @BindView(R.id.edit_onboarding_view) View editOnboardingView;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.contributions_text) TextView contributionsText;
    @BindView(R.id.task_recyclerview) RecyclerView tasksRecyclerView;
    @BindView(R.id.suggested_edits_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.progress_bar) View progressBar;

    private EditTask addDescriptionsTask;
    private EditTask translateDescriptionsTeaserTask;
    private EditTask translateDescriptionsTask;
    private EditTask addImageCaptionsTask;
    private EditTask translateImageCaptionsTask;

    private List<EditTask> displayedTasks = new ArrayList<>();
    private TaskViewCallback callback = new TaskViewCallback();

    private CompositeDisposable disposables = new CompositeDisposable();

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

        swipeRefreshLayout.setColorSchemeResources(getThemedAttributeId(requireContext(), R.attr.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(this::updateUI);

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        final int topDecorationDp = 16;
        tasksRecyclerView.addItemDecoration(new HeaderMarginItemDecoration(topDecorationDp, 0));
        tasksRecyclerView.addItemDecoration(new FooterMarginItemDecoration(0, topDecorationDp));
        tasksRecyclerView.setAdapter(new RecyclerAdapter(displayedTasks));

        username.setText(AccountUtil.getUserName());
        setUpTasks();
        return view;
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
            holder.getView().setUpViews(items().get(i), callback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void fetchUserContributions() {
        updateDisplayedTasks(null);
        contributionsText.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        disposables.add(ServiceFactory.get(new WikiSite(Service.WIKIDATA_URL)).getEditorTaskCounts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    progressBar.setVisibility(View.GONE);
                    contributionsText.setVisibility(View.VISIBLE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .subscribe(response -> {
                    EditorTaskCounts editorTaskCounts = response.query().editorTaskCounts();
                    NotificationEditorTasksHandler.dispatchEditorTaskResults(requireContext(), editorTaskCounts);
                    int totalEdits = 0;
                    for (int count : editorTaskCounts.getDescriptionEditsPerLanguage().values()) {
                        totalEdits += count;
                    }
                    contributionsText.setText(getResources().getQuantityString(R.plurals.edit_action_contribution_count, totalEdits, totalEdits));
                    updateDisplayedTasks(editorTaskCounts);
                }, throwable -> {
                    L.e(throwable);
                    FeedbackUtil.showError(requireActivity(), throwable);
                }));
    }

    private void updateUI() {
        requireActivity().invalidateOptionsMenu();
        fetchUserContributions();
    }

    private void showOneTimeOnboarding() {
        if (Prefs.showEditTaskOnboarding()) {
            editOnboardingView.setVisibility(View.VISIBLE);
        }
    }

    private void setUpTasks() {
        addDescriptionsTask = new EditTask();
        addDescriptionsTask.setTitle(getString(R.string.title_description_task_title));
        addDescriptionsTask.setDescription(getString(R.string.title_description_task_description));
        addDescriptionsTask.setImagePlaceHolderShown(true);
        addDescriptionsTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_short_text_white_24dp));
        addDescriptionsTask.setNoActionLayout(true);

        translateDescriptionsTeaserTask = new EditTask();
        translateDescriptionsTeaserTask.setTitle(getString(R.string.multilingual_task_title));
        translateDescriptionsTeaserTask.setDescription(getString(R.string.multilingual_task_description));
        translateDescriptionsTeaserTask.setImagePlaceHolderShown(false);
        translateDescriptionsTeaserTask.setNoActionLayout(false);
        translateDescriptionsTeaserTask.setDisabled(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
        translateDescriptionsTeaserTask.setEnabledPositiveActionString(getString(R.string.multilingual_task_positive));
        translateDescriptionsTeaserTask.setEnabledNegativeActionString(getString(R.string.multilingual_task_negative));

        translateDescriptionsTask = new EditTask();
        translateDescriptionsTask.setTitle(getString(R.string.translation_task_title));
        translateDescriptionsTask.setDescription(getString(R.string.translation_task_description));
        translateDescriptionsTask.setImagePlaceHolderShown(true);
        translateDescriptionsTask.setNoActionLayout(true);
        translateDescriptionsTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_translate_title_descriptions));
        translateDescriptionsTask.setNoActionLayout(Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
        translateDescriptionsTask.setDisabled(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());

        addImageCaptionsTask = new EditTask();
        addImageCaptionsTask.setTitle(getString(R.string.image_caption_task_title));
        addImageCaptionsTask.setDescription(getString(R.string.image_caption_task_description));
        addImageCaptionsTask.setImagePlaceHolderShown(true);
        addImageCaptionsTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_caption_images));
        addImageCaptionsTask.setDisabled(true);

        translateImageCaptionsTask = new EditTask();
        translateImageCaptionsTask.setTitle(getString(R.string.translate_caption_task_title));
        translateImageCaptionsTask.setDescription(getString(R.string.translate_caption_task_description));
        translateImageCaptionsTask.setImagePlaceHolderShown(true);
        translateImageCaptionsTask.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_translate_title_descriptions));
        translateImageCaptionsTask.setDisabled(true);
    }

    private void updateDisplayedTasks(@Nullable EditorTaskCounts editorTaskCounts) {
        displayedTasks.clear();
        try {
            if (editorTaskCounts == null) {
                return;
            }

            int targetForTranslateDescriptions = editorTaskCounts.getDescriptionEditTargets().get(1);

            displayedTasks.add(addDescriptionsTask);
            addDescriptionsTask.setDisabled(!Prefs.isSuggestedEditsAddDescriptionsUnlocked());

            if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() < MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                if (Prefs.showTranslateDescriptionsTeaserTask()) {
                    displayedTasks.add(translateDescriptionsTeaserTask);
                    translateDescriptionsTeaserTask.setDisabledDescriptionText(String.format(getString(R.string.translate_description_edit_disable_text), targetForTranslateDescriptions));
                    translateDescriptionsTeaserTask.setDisabled(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
                }
            } else {
                displayedTasks.add(translateDescriptionsTask);
                translateDescriptionsTask.setDisabledDescriptionText(String.format(getString(R.string.translate_description_edit_disable_text), targetForTranslateDescriptions));
                translateDescriptionsTask.setDisabled(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
            }

            // TODO: enable image caption tasks.

        } catch (Exception e) {
            L.e(e);
        } finally {
            tasksRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_suggested_edits_tasks, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_help).setVisible(editOnboardingView.getVisibility() != View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help:
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext());
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.get_started_button)
    void onGetStartedClicked() {
        Prefs.setShowEditTasksOnboarding(false);
        editOnboardingView.setVisibility(View.GONE);
        updateUI();
    }

    @OnClick(R.id.user_contributions_button)
    void onUserContributionsClicked() {
        startActivity(MyContributionsActivity.Companion.newIntent(requireContext()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
    }

    private class TaskViewCallback implements EditTaskView.Callback {
        @Override
        public void onPositiveActionClick(EditTask task) {
            if (task.equals(translateDescriptionsTeaserTask)) {
                requireActivity().startActivityForResult(WikipediaLanguagesActivity.newIntent(requireActivity(),
                        LanguageSettingsInvokeSource.DESCRIPTION_EDITING.text()), ACTIVITY_REQUEST_ADD_A_LANGUAGE);
            }
        }

        @Override
        public void onNegativeActionClick(EditTask task) {
            if (task.equals(translateDescriptionsTeaserTask)) {
                int multilingualTaskPosition = displayedTasks.indexOf(translateDescriptionsTeaserTask);
                displayedTasks.remove(translateDescriptionsTeaserTask);
                tasksRecyclerView.getAdapter().notifyItemChanged(multilingualTaskPosition);
                Prefs.setShowTranslateDescriptionsTeaserTask(false);
            }
        }

        @Override
        public void onViewClick(EditTask task) {
            if (task.equals(addDescriptionsTask)) {
                startActivity(AddTitleDescriptionsActivity.Companion.newIntent(requireActivity(), InvokeSource.EDIT_FEED_TITLE_DESC));
            } else if (task.equals(translateDescriptionsTask)) {
                if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() > 1) {
                    startActivity(AddTitleDescriptionsActivity.Companion.newIntent(requireActivity(), EDIT_FEED_TRANSLATE_TITLE_DESC));
                }
            }
        }
    }
}
