package org.wikipedia.language;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.AppLanguageSearchingFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.SiteMatrix;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.util.log.L;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.settings.languages.WikipediaLanguagesFragment.ADD_LANGUAGE_INTERACTIONS;
import static org.wikipedia.settings.languages.WikipediaLanguagesFragment.SESSION_TOKEN;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class LanguagesListActivity extends BaseActivity {
    private WikipediaApp app;

    @BindView(R.id.languages_list_load_progress) View progressBar;
    @BindView(R.id.languages_list_empty_view) SearchEmptyView emptyView;
    @BindView(R.id.languages_list_recycler) RecyclerView recyclerView;

    private LanguagesListAdapter adapter;
    private String currentSearchQuery;
    private ActionMode actionMode;
    private SearchActionModeCallback searchActionModeCallback;
    private AppLanguageSearchingFunnel searchingFunnel;
    private int interactionsCount = 0;
    private boolean isLanguageSearched;
    public static final String LANGUAGE_SEARCHED = "language_searched";
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable private List<SiteMatrix.SiteInfo> siteInfoList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_languages_list);
        ButterKnife.bind(this);
        app = WikipediaApp.getInstance();

        emptyView.setEmptyText(R.string.langlinks_no_match);
        emptyView.setVisibility(View.GONE);

        adapter = new LanguagesListAdapter(app.language().getAppMruLanguageCodes(), app.language().getRemainingAvailableLanguageCodes());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar.setVisibility(View.VISIBLE);

        searchActionModeCallback = new LanguagesListActivity.LanguageSearchCallback();
        searchingFunnel = new AppLanguageSearchingFunnel(getIntent().getStringExtra(SESSION_TOKEN));
        requestLanguages();
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_languages_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_language:
                if (actionMode == null) {
                    actionMode = startSupportActionMode(searchActionModeCallback);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard(this);
        Intent returnIntent = new Intent();
        returnIntent.putExtra(LANGUAGE_SEARCHED, isLanguageSearched);
        setResult(RESULT_OK, returnIntent);
        searchingFunnel.logNoLanguageAdded(false, currentSearchQuery);
        super.onBackPressed();
    }

    private class LanguageSearchCallback extends SearchActionModeCallback {
        private LanguagesListAdapter languageAdapter = (LanguagesListAdapter) recyclerView.getAdapter();
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //currentSearchQuery is cleared here, instead of onDestroyActionMode
            // in order to make the most recent search string available to analytics
            currentSearchQuery = "";
            isLanguageSearched = true;
            actionMode = mode;
            ViewUtil.finishActionModeWhenTappingOnView(recyclerView, actionMode);
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            currentSearchQuery = s.trim();
            languageAdapter.setFilterText(currentSearchQuery);

            if (recyclerView.getAdapter().getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            emptyView.setVisibility(View.GONE);
            languageAdapter.reset();
            actionMode = null;
        }

        @Override
        protected String getSearchHintString() {
            return getResources().getString(R.string.search_hint_search_languages);
        }

        @Override
        protected boolean finishActionModeIfKeyboardHiding() {
            return false;
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private final class LanguagesListAdapter
            extends RecyclerView.Adapter<DefaultViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;
        @NonNull private final List<String> originalLanguageCodes;
        @NonNull private final List<String> suggestedLanguageCodes;
        @NonNull private final List<String> languageCodes = new ArrayList<>();
        private boolean isSearching;

        private LanguagesListAdapter(@NonNull List<String> languageCodes, @NonNull List<String> suggestedLanguageCodes) {
            originalLanguageCodes = new ArrayList<>(languageCodes);
            this.suggestedLanguageCodes = suggestedLanguageCodes;
            reset();
        }

        @Override
        public int getItemViewType(int position) {
            return shouldShowSectionHeader(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @Override
        public int getItemCount() {
            return this.languageCodes.size();
        }

        @NonNull
        @Override
        public DefaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            if (viewType == VIEW_TYPE_HEADER) {
                View view = inflater.inflate(R.layout.view_section_header, parent, false);
                return new DefaultViewHolder(languageCodes, view);
            } else {
                View view = inflater.inflate(R.layout.item_language_list_entry, parent, false);
                return new LanguagesListItemHolder(languageCodes, view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder holder, int pos) {
            holder.bindItem(pos);
            if (holder instanceof LanguagesListItemHolder) {
                holder.itemView.setOnClickListener((View view) -> {
                    String lang = languageCodes.get(pos);
                    if (!lang.equals(app.getAppOrSystemLanguageCode())) {
                        app.language().addAppLanguageCode(lang);
                    }
                    interactionsCount++;
                    searchingFunnel.logLanguageAdded(true, lang, currentSearchQuery);
                    hideSoftKeyboard(LanguagesListActivity.this);
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(ADD_LANGUAGE_INTERACTIONS, interactionsCount);
                    returnIntent.putExtra(LANGUAGE_SEARCHED, isLanguageSearched);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                });
            }
        }

        boolean shouldShowSectionHeader(int position) {
            return !isSearching
                    && (position == 0  || (suggestedLanguageCodes.size() > 0
                    && position == suggestedLanguageCodes.size() + 1));
        }

        void setFilterText(String filter) {
            isSearching = true;
            this.languageCodes.clear();
            filter = StringUtils.stripAccents(filter).toLowerCase(Locale.getDefault());
            for (String code : originalLanguageCodes) {
                String localizedName = StringUtils.stripAccents(defaultString(app.language().getAppLanguageLocalizedName(code)));
                String canonicalName = StringUtils.stripAccents(defaultString(getCanonicalName(code)));
                if (code.contains(filter)
                        || localizedName.toLowerCase(Locale.getDefault()).contains(filter)
                        || canonicalName.toLowerCase(Locale.getDefault()).contains(filter)) {
                    this.languageCodes.add(code);
                }
            }
            notifyDataSetChanged();
        }

        void reset() {
            isSearching = false;
            this.languageCodes.clear();
            if (suggestedLanguageCodes.size() > 0) {
                this.languageCodes.add(getString(R.string.languages_list_suggested_text));
                this.languageCodes.addAll(suggestedLanguageCodes);
            }
            this.languageCodes.add(getString(R.string.languages_list_all_text));
            this.languageCodes.addAll(getNonDuplicateLanguageCodesList());
            // should not be able to be searched while the languages are selected
            this.originalLanguageCodes.removeAll(app.language().getAppLanguageCodes());
            notifyDataSetChanged();
        }

        // To remove the already selected languages and suggested languages from all languages list
        private List<String> getNonDuplicateLanguageCodesList() {
            List<String> list = new ArrayList<>(originalLanguageCodes);
            list.removeAll(app.language().getAppLanguageCodes());
            list.removeAll(suggestedLanguageCodes);
            return list;
        }
    }

    @Nullable
    private String getCanonicalName(@NonNull String code) {
        String canonicalName = null;
        if (siteInfoList != null) {
            for (SiteMatrix.SiteInfo info : siteInfoList) {
                if (code.equals(info.code())) {
                    canonicalName = info.localName();
                    break;
                }
            }
        }
        if (TextUtils.isEmpty(canonicalName)) {
            canonicalName = app.language().getAppLanguageCanonicalName(code);
        }
        return canonicalName;
    }

    // TODO: optimize and reuse the header view holder?
    private class DefaultViewHolder extends RecyclerView.ViewHolder {
        private TextView sectionHeaderTextView;
        private List<String> languageCodes;

        DefaultViewHolder(@NonNull List<String> languageCodes, View itemView) {
            super(itemView);
            sectionHeaderTextView = itemView.findViewById(R.id.section_header_text);
            this.languageCodes = languageCodes;
        }

        void bindItem(int position) {
            sectionHeaderTextView.setText(languageCodes.get(position));
        }
    }

    private class LanguagesListItemHolder extends DefaultViewHolder {
        private TextView localizedNameTextView;
        private TextView canonicalNameTextView;
        private List<String> languageCodes;

        LanguagesListItemHolder(@NonNull List<String> languageCodes, View itemView) {
            super(languageCodes, itemView);
            localizedNameTextView = itemView.findViewById(R.id.localized_language_name);
            canonicalNameTextView = itemView.findViewById(R.id.language_subtitle);
            this.languageCodes = languageCodes;
        }

        void bindItem(int position) {

            String languageCode = languageCodes.get(position);

            localizedNameTextView.setText(StringUtils.capitalize(app.language().getAppLanguageLocalizedName(languageCode)));

            String canonicalName = getCanonicalName(languageCode);
            if (progressBar.getVisibility() != View.VISIBLE) {
                canonicalNameTextView.setText(TextUtils.isEmpty(canonicalName)
                        ? app.language().getAppLanguageCanonicalName(languageCode) : canonicalName);
            }
        }
    }

    private void requestLanguages() {
        disposables.add(ServiceFactory.get(app.getWikiSite()).getSiteMatrix()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(SiteMatrix::getSites)
                .doFinally(() -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    adapter.notifyDataSetChanged();
                })
                .subscribe(sites -> siteInfoList = sites, L::e));
    }
}
