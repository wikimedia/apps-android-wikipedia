package org.wikipedia.suggestededits;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_DESC;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_DESC;
import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;

public class SuggestedEditsTasksFragment extends Fragment {
    private Unbinder unbinder;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.contributions_text) TextView contributionsText;
    @BindView(R.id.task_recyclerview) RecyclerView tasksRecyclerView;
    @BindView(R.id.suggested_edits_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.progress_bar) View progressBar;

    private SuggestedEditsTask addDescriptionsTask;
    private SuggestedEditsTask translateDescriptionsTask;
    private SuggestedEditsTask addImageCaptionsTask;
    private SuggestedEditsTask translateImageCaptionsTask;
    private SuggestedEditsTask multilingualTeaserTask;

    private List<SuggestedEditsTask> displayedTasks = new ArrayList<>();
    private TaskViewCallback callback = new TaskViewCallback();

    private CompositeDisposable disposables = new CompositeDisposable();

    public static SuggestedEditsTasksFragment newInstance() {
        return new SuggestedEditsTasksFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false);
        unbinder = ButterKnife.bind(this, view);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(0f);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        swipeRefreshLayout.setColorSchemeResources(getThemedAttributeId(requireContext(), R.attr.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(this::updateUI);

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        final int topDecorationDp = 16;
        tasksRecyclerView.addItemDecoration(new HeaderMarginItemDecoration(topDecorationDp, 0));
        tasksRecyclerView.addItemDecoration(new FooterMarginItemDecoration(0, topDecorationDp));
        tasksRecyclerView.setAdapter(new RecyclerAdapter(displayedTasks));

        username.setText(AccountUtil.getUserName());
        setUpTasks();
        return view;
    }

    class RecyclerAdapter extends DefaultRecyclerAdapter<SuggestedEditsTask, SuggestedEditsTaskView> {

        RecyclerAdapter(@NonNull List<SuggestedEditsTask> tasks) {
            super(tasks);
        }

        @NonNull
        @Override public DefaultViewHolder<SuggestedEditsTaskView> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DefaultViewHolder<>(new SuggestedEditsTaskView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder<SuggestedEditsTaskView> holder, int i) {
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
                    for (int count : editorTaskCounts.getCaptionEditsPerLanguage().values()) {
                        totalEdits += count;
                    }
                    contributionsText.setText(getResources().getQuantityString(R.plurals.suggested_edits_contribution_count, totalEdits, totalEdits));
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


    private void setUpTasks() {
        addDescriptionsTask = new SuggestedEditsTask();
        addDescriptionsTask.setTitle(getString(R.string.suggested_edits_task_add_description_title));
        addDescriptionsTask.setDescription(getString(R.string.suggested_edits_task_add_description_description));
        addDescriptionsTask.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_short_text_white_24dp));

        translateDescriptionsTask = new SuggestedEditsTask();
        translateDescriptionsTask.setTitle(getString(R.string.suggested_edits_task_translation_title));
        translateDescriptionsTask.setDescription(getString(R.string.suggested_edits_task_translation_description));
        translateDescriptionsTask.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_translate_title_descriptions));

        addImageCaptionsTask = new SuggestedEditsTask();
        addImageCaptionsTask.setTitle(getString(R.string.suggested_edits_task_image_caption_title));
        addImageCaptionsTask.setDescription(getString(R.string.suggested_edits_task_image_caption_description));
        addImageCaptionsTask.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_caption_images));
        addImageCaptionsTask.setDisabled(true);

        translateImageCaptionsTask = new SuggestedEditsTask();
        translateImageCaptionsTask.setTitle(getString(R.string.suggested_edits_task_translate_caption_title));
        translateImageCaptionsTask.setDescription(getString(R.string.suggested_edits_task_translate_caption_description));
        translateImageCaptionsTask.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_caption_translate));

        multilingualTeaserTask = new SuggestedEditsTask();
        multilingualTeaserTask.setTitle(getString(R.string.suggested_edits_task_multilingual_title));
        multilingualTeaserTask.setDescription(getString(R.string.suggested_edits_task_multilingual_description));
        multilingualTeaserTask.setShowImagePlaceholder(false);
        multilingualTeaserTask.setShowActionLayout(true);
        multilingualTeaserTask.setDisabled(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
        multilingualTeaserTask.setUnlockActionPositiveButtonString(getString(R.string.suggested_edits_task_multilingual_positive));
        multilingualTeaserTask.setUnlockActionNegativeButtonString(getString(R.string.suggested_edits_task_multilingual_negative));
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
                if (Prefs.showSuggestedEditsMultilingualTeaserTask()) {
                    displayedTasks.add(multilingualTeaserTask);
                    multilingualTeaserTask.setUnlockMessageText(String.format(getString(R.string.suggested_edits_task_translate_description_edit_disable_text), targetForTranslateDescriptions));
                    multilingualTeaserTask.setDisabled(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
                }
            } else {
                displayedTasks.add(translateDescriptionsTask);
                translateDescriptionsTask.setUnlockMessageText(String.format(getString(R.string.suggested_edits_task_translate_description_edit_disable_text), targetForTranslateDescriptions));
                translateDescriptionsTask.setShowActionLayout(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
                translateDescriptionsTask.setDisabled(!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked());
            }

            int targetForTranslateCaptions = editorTaskCounts.getCaptionEditTargets().get(1);

            if (Prefs.isSuggestedEditsAddCaptionsUnlocked()) {
                displayedTasks.add(addImageCaptionsTask);
                addImageCaptionsTask.setDisabled(false);

                if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                    displayedTasks.add(translateImageCaptionsTask);
                    translateImageCaptionsTask.setUnlockMessageText(String.format(getString(R.string.suggested_edits_task_translate_description_edit_disable_text), targetForTranslateCaptions));
                    translateImageCaptionsTask.setShowActionLayout(!Prefs.isSuggestedEditsTranslateCaptionsUnlocked());
                    translateImageCaptionsTask.setDisabled(Prefs.isSuggestedEditsTranslateCaptionsUnlocked());
                }
            }

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help:
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext());
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @OnClick(R.id.user_contributions_button)
    void onUserContributionsClicked() {
        startActivity(SuggestedEditsContributionsActivity.Companion.newIntent(requireContext()));
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

    private class TaskViewCallback implements SuggestedEditsTaskView.Callback {
        @Override
        public void onPositiveActionClick(SuggestedEditsTask task) {
            if (task.equals(multilingualTeaserTask)) {
                requireActivity().startActivityForResult(WikipediaLanguagesActivity.newIntent(requireActivity(),
                        LanguageSettingsInvokeSource.DESCRIPTION_EDITING.text()), ACTIVITY_REQUEST_ADD_A_LANGUAGE);
            }
        }

        @Override
        public void onNegativeActionClick(SuggestedEditsTask task) {
            if (task.equals(multilingualTeaserTask)) {
                int multilingualTaskPosition = displayedTasks.indexOf(multilingualTeaserTask);
                displayedTasks.remove(multilingualTeaserTask);
                tasksRecyclerView.getAdapter().notifyItemChanged(multilingualTaskPosition);
                Prefs.setShowSuggestedEditsMultilingualTeaserTask(false);
            }
        }

        @Override
        public void onViewClick(SuggestedEditsTask task) {
            if (task.equals(addDescriptionsTask)) {
                startActivity(SuggestedEditsCardsActivity.Companion.newIntent(requireActivity(), SUGGESTED_EDITS_ADD_DESC));
            } else if (task.equals(translateDescriptionsTask)) {
                if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() > 1) {
                    startActivity(SuggestedEditsCardsActivity.Companion.newIntent(requireActivity(), SUGGESTED_EDITS_TRANSLATE_DESC));
                }
            } else if (task.equals(addImageCaptionsTask)) {
                 startActivity(SuggestedEditsCardsActivity.Companion.newIntent(requireActivity(), SUGGESTED_EDITS_ADD_CAPTION));
            } else if (task.equals(translateImageCaptionsTask)) {
                if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() > 1) {
                    startActivity(SuggestedEditsCardsActivity.Companion.newIntent(requireActivity(), SUGGESTED_EDITS_TRANSLATE_CAPTION));
                }
            }
        }
    }
}
