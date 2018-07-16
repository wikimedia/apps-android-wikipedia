package org.wikipedia.settings.languages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.AppLanguageSettingsFunnel;
import org.wikipedia.language.LanguageSettingsInvokeSource;
import org.wikipedia.language.LanguagesListActivity;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.MultiSelectActionModeCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE;
import static org.wikipedia.language.LanguagesListActivity.LANGUAGE_SEARCHED;
import static org.wikipedia.settings.languages.WikipediaLanguagesActivity.INVOKE_SOURCE_EXTRA;

public class WikipediaLanguagesFragment extends Fragment implements WikipediaLanguagesItemView.Callback {
    public static final String ACTIVITY_RESULT_LANG_POSITION_DATA = "activity_result_lang_position_data";
    public static final String ADD_LANGUAGE_INTERACTIONS = "add_language_interactions";
    public static final String SESSION_TOKEN = "session_token";

    @BindView(R.id.wikipedia_languages_recycler) RecyclerView recyclerView;
    private WikipediaApp app;
    private Unbinder unbinder;
    private ItemTouchHelper itemTouchHelper;
    private List<String> wikipediaLanguages = new ArrayList<>();
    private WikipediaLanguageItemAdapter adapter;
    private ActionMode actionMode;
    private MultiSelectCallback multiSelectCallback = new MultiSelectCallback();
    private List<String> selectedCodes = new ArrayList<>();
    private static final int NUM_HEADERS = 1;
    private static final int NUM_FOOTERS = 1;
    private AppLanguageSettingsFunnel funnel;
    private String invokeSource;
    private String initialLanguageList;
    private int interactionsCount;
    private boolean isLanguageSearched = false;

    @NonNull public static WikipediaLanguagesFragment newInstance(@NonNull String invokeSource) {
        WikipediaLanguagesFragment instance = new WikipediaLanguagesFragment();
        Bundle args = new Bundle();
        args.putString(INVOKE_SOURCE_EXTRA, invokeSource);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wikipedia_languages, container, false);
        app = WikipediaApp.getInstance();
        invokeSource = requireActivity().getIntent().getStringExtra(INVOKE_SOURCE_EXTRA);
        initialLanguageList = StringUtil.listToJsonArrayString(app.language().getAppLanguageCodes());
        funnel = new AppLanguageSettingsFunnel();
        unbinder = ButterKnife.bind(this, view);

        prepareWikipediaLanguagesList();
        setupRecyclerView();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE
                && resultCode == RESULT_OK) {
            interactionsCount += data.getIntExtra(ADD_LANGUAGE_INTERACTIONS, 0);
            isLanguageSearched = (isLanguageSearched) || data.getBooleanExtra(LANGUAGE_SEARCHED, false);
            prepareWikipediaLanguagesList();
            requireActivity().invalidateOptionsMenu();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        funnel.logLanguageSetting(invokeSource, initialLanguageList, StringUtil.listToJsonArrayString(app.language().getAppLanguageCodes()), interactionsCount, isLanguageSearched);
        recyclerView.setAdapter(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wikipedia_languages, menu);
        if (app.language().getAppLanguageCodes().size() <= 1) {
            MenuItem overflowMenu = menu.getItem(0);
            overflowMenu.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_wikipedia_languages_remove:
                beginRemoveLanguageMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void prepareWikipediaLanguagesList() {
        wikipediaLanguages.clear();
        wikipediaLanguages.addAll(app.language().getAppLanguageCodes());
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        adapter = new WikipediaLanguageItemAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        itemTouchHelper = new ItemTouchHelper(new RearrangeableItemTouchHelperCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onCheckedChanged(int position) {
        toggleSelectedLanguage(wikipediaLanguages.get(position));
    }

    @Override
    public void onLongPress(int position) {
        if (actionMode == null) {
            beginRemoveLanguageMode();
        }
        toggleSelectedLanguage(wikipediaLanguages.get(position));
        adapter.notifyDataSetChanged();
    }

    private void updateWikipediaLanguages() {
        app.language().setAppLanguageCodes(wikipediaLanguages);
        adapter.notifyDataSetChanged();
        requireActivity().invalidateOptionsMenu();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private final class WikipediaLanguageItemAdapter extends RecyclerView.Adapter<DefaultViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;
        private static final int VIEW_TYPE_FOOTER = 2;
        private boolean checkboxEnabled;

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else if (position == getItemCount() - 1) {
                return VIEW_TYPE_FOOTER;
            } else {
                return VIEW_TYPE_ITEM;
            }
        }

        @Override
        public int getItemCount() {
            return wikipediaLanguages.size() + NUM_HEADERS + NUM_FOOTERS;
        }

        @Override
        public DefaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            if (viewType == VIEW_TYPE_HEADER) {
                View view = inflater.inflate(R.layout.view_section_header, parent, false);
                return new HeaderViewHolder(view);
            } else if (viewType == VIEW_TYPE_FOOTER) {
                View view = inflater.inflate(R.layout.view_wikipedia_language_footer, parent, false);
                return new FooterViewHolder(view);
            } else {
                return new WikipediaLanguageItemHolder(new WikipediaLanguagesItemView(getContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder holder, int pos) {
            if (holder instanceof WikipediaLanguageItemHolder) {
                WikipediaLanguageItemHolder itemHolder = ((WikipediaLanguageItemHolder) holder);
                itemHolder.bindItem(wikipediaLanguages.get(pos - NUM_HEADERS), pos - NUM_FOOTERS);
                itemHolder.getView().setCheckBoxEnabled(checkboxEnabled);
                itemHolder.getView().setCheckBoxChecked(selectedCodes.contains(wikipediaLanguages.get(pos - NUM_HEADERS)));
                itemHolder.getView().setDragHandleEnabled(wikipediaLanguages.size() > 1 && !checkboxEnabled);
                itemHolder.getView().setOnClickListener(view -> {
                    if (actionMode != null) {
                        toggleSelectedLanguage(wikipediaLanguages.get(pos - NUM_HEADERS));
                        adapter.notifyDataSetChanged();
                    } else if (launchedFromSearch()) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(ACTIVITY_RESULT_LANG_POSITION_DATA, pos - NUM_HEADERS);
                        requireActivity().setResult(RESULT_OK, resultIntent);
                        requireActivity().finish();
                    }
                });
            }
        }

        @Override public void onViewAttachedToWindow(@NonNull DefaultViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof WikipediaLanguageItemHolder) {
                WikipediaLanguageItemHolder itemHolder = ((WikipediaLanguageItemHolder) holder);
                itemHolder.getView().setDragHandleTouchListener((v, event) -> {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            interactionsCount++;
                            itemTouchHelper.startDrag(holder);
                            break;
                        case MotionEvent.ACTION_UP:
                            v.performClick();
                            break;
                        default:
                            break;
                    }
                    return false;
                });
                itemHolder.getView().setCallback(WikipediaLanguagesFragment.this);
            } else if (holder instanceof FooterViewHolder) {
                holder.getView().setVisibility(checkboxEnabled ? View.GONE : View.VISIBLE);
                holder.getView().setOnClickListener(v -> {
                    Intent intent = new Intent(requireActivity(), LanguagesListActivity.class);
                    intent.putExtra(SESSION_TOKEN, funnel.getSessionToken());
                    startActivityForResult(intent, ACTIVITY_REQUEST_ADD_A_LANGUAGE);
                    finishActionMode();
                });
            }
        }
        @Override public void onViewDetachedFromWindow(@NonNull DefaultViewHolder holder) {
            if (holder instanceof WikipediaLanguageItemHolder) {
                WikipediaLanguageItemHolder itemHolder = ((WikipediaLanguageItemHolder) holder);
                itemHolder.getView().setCallback(null);
                itemHolder.getView().setDragHandleTouchListener(null);
            }
            super.onViewDetachedFromWindow(holder);
        }

        void onMoveItem(int oldPosition, int newPosition) {
            Collections.swap(wikipediaLanguages, oldPosition - NUM_HEADERS, newPosition - NUM_FOOTERS);
            notifyItemMoved(oldPosition, newPosition);
        }

        void onCheckboxEnabled(boolean enabled) {
            checkboxEnabled = enabled;
        }
    }

    private final class RearrangeableItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private final WikipediaLanguageItemAdapter adapter;

        RearrangeableItemTouchHelperCallback(WikipediaLanguageItemAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof WikipediaLanguageItemHolder
                    ? makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) : -1;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (target instanceof WikipediaLanguageItemHolder) {
                adapter.onMoveItem(source.getAdapterPosition(), target.getAdapterPosition());
            }
            return true;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            recyclerView.post(() -> {
                if (isAdded()) {
                    updateWikipediaLanguages();
                }
            });
        }
    }

    // TODO: optimize and reuse the header view holder
    private class HeaderViewHolder extends DefaultViewHolder<View> {
        HeaderViewHolder(View itemView) {
            super(itemView);
            TextView sectionText = itemView.findViewById(R.id.section_header_text);
            sectionText.setText(R.string.wikipedia_languages_your_languages_text);
        }
    }

    private class WikipediaLanguageItemHolder extends DefaultViewHolder<WikipediaLanguagesItemView> {
        WikipediaLanguageItemHolder(WikipediaLanguagesItemView itemView) {
            super(itemView);
        }

        void bindItem(String languageCode, int position) {
            getView().setContents(languageCode, app.language().getAppLanguageLocalizedName(languageCode), position);
        }
    }

    private class FooterViewHolder extends DefaultViewHolder<View> {
        FooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    private boolean launchedFromSearch() {
        String source = requireActivity().getIntent().hasExtra(INVOKE_SOURCE_EXTRA) ? requireActivity().getIntent().getStringExtra(INVOKE_SOURCE_EXTRA) : "";
        return source.equals(LanguageSettingsInvokeSource.SEARCH.text());
    }

    private void setMultiSelectEnabled(boolean enabled) {
        adapter.onCheckboxEnabled(enabled);
        adapter.notifyDataSetChanged();
        requireActivity().invalidateOptionsMenu();
    }

    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void beginRemoveLanguageMode() {
        ((AppCompatActivity) requireActivity()).startSupportActionMode(multiSelectCallback);
        setMultiSelectEnabled(true);
    }

    private void toggleSelectedLanguage(String code) {
        if (selectedCodes.contains(code)) {
            selectedCodes.remove(code);
        } else {
            selectedCodes.add(code);
        }
    }

    private void unselectAllLanguages() {
        selectedCodes.clear();
        adapter.notifyDataSetChanged();
    }

    private void deleteSelectedLanguages() {
        app.language().removeAppLanguageCodes(selectedCodes);
        interactionsCount++;
        prepareWikipediaLanguagesList();
        unselectAllLanguages();
    }

    private class MultiSelectCallback extends MultiSelectActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            super.onCreateActionMode(mode, menu);
            mode.setTitle(R.string.wikipedia_languages_remove_action_mode_title);
            mode.getMenuInflater().inflate(R.menu.menu_action_mode_wikipedia_languages, menu);
            actionMode = mode;
            selectedCodes.clear();
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onDeleteSelected() {
            showRemoveLanguagesDialog();
            // TODO: add snackbar for undo action?
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            unselectAllLanguages();
            setMultiSelectEnabled(false);
            actionMode = null;
            super.onDestroyActionMode(mode);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public void showRemoveLanguagesDialog() {
        if (selectedCodes.size() > 0) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(requireActivity());
            if (selectedCodes.size() < wikipediaLanguages.size()) {
                alertDialog
                        .setTitle(getResources().getQuantityString(R.plurals.wikipedia_languages_remove_dialog_title, selectedCodes.size()))
                        .setMessage(R.string.wikipedia_languages_remove_dialog_content)
                        .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                            deleteSelectedLanguages();
                            finishActionMode();
                        })
                        .setNegativeButton(android.R.string.cancel, null);
            } else {
                alertDialog
                        .setTitle(R.string.wikipedia_languages_remove_warning_dialog_title)
                        .setMessage(R.string.wikipedia_languages_remove_warning_dialog_content)
                        .setPositiveButton(android.R.string.ok, null);
            }
            alertDialog.show();
        }
    }
}
