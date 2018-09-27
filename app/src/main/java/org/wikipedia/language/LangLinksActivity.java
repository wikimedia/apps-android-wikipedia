package org.wikipedia.language;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
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
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.SiteMatrix;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.log.L;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class LangLinksActivity extends BaseActivity {
    public static final int ACTIVITY_RESULT_LANGLINK_SELECT = 1;

    public static final String ACTION_LANGLINKS_FOR_TITLE = "org.wikipedia.langlinks_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";

    private static final String LANGUAGE_ENTRIES_BUNDLE_KEY = "languageEntries";

    private static final String GOTHIC_LANGUAGE_CODE = "got";

    private List<PageTitle> languageEntries;
    private PageTitle title;

    private WikipediaApp app;
    private CompositeDisposable disposables = new CompositeDisposable();

    @BindView(R.id.langlinks_load_progress) View langLinksProgress;
    @BindView(R.id.langlinks_error) WikiErrorView langLinksError;
    @BindView(R.id.langlink_empty_view) SearchEmptyView langLinksEmpty;
    @BindView(R.id.langlinks_recycler) RecyclerView langLinksList;

    private LangLinksAdapter adapter;
    private String currentSearchQuery;
    private ActionMode actionMode;
    private SearchActionModeCallback searchActionModeCallback;

    @Nullable private List<SiteMatrix.SiteInfo> siteInfoList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_langlinks);
        ButterKnife.bind(this);
        app = WikipediaApp.getInstance();

        if (!ACTION_LANGLINKS_FOR_TITLE.equals(getIntent().getAction())) {
            throw new RuntimeException("Only ACTION_LANGLINKS_FOR_TITLE is supported");
        }

        title = getIntent().getParcelableExtra(EXTRA_PAGETITLE);

        if (savedInstanceState != null && savedInstanceState.containsKey(LANGUAGE_ENTRIES_BUNDLE_KEY)) {
            languageEntries = savedInstanceState.getParcelableArrayList(LANGUAGE_ENTRIES_BUNDLE_KEY);
        }

        langLinksEmpty.setVisibility(View.GONE);
        langLinksProgress.setVisibility(View.VISIBLE);

        fetchLangLinks();

        langLinksError.setRetryClickListener((v) -> {
            ViewAnimations.crossFade(langLinksError, langLinksProgress);
            fetchLangLinks();
        });
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_languages_list, menu);
        MenuItem searchIcon = menu.getItem(0);
        searchIcon.setVisible((languageEntries != null && languageEntries.size() > 0));
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
        super.onBackPressed();
    }

    private class LanguageSearchCallback extends SearchActionModeCallback {
        private LangLinksAdapter langLinksAdapter = (LangLinksAdapter) langLinksList.getAdapter();
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            ViewUtil.finishActionModeWhenTappingOnView(langLinksList, actionMode);
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            currentSearchQuery = s.trim();
            langLinksAdapter.setFilterText(currentSearchQuery);

            if (langLinksList.getAdapter().getItemCount() == 0) {
                langLinksEmpty.setVisibility(View.VISIBLE);
            } else {
                langLinksEmpty.setVisibility(View.GONE);
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            if (!TextUtils.isEmpty(currentSearchQuery)) {
                currentSearchQuery = "";
            }
            langLinksEmpty.setVisibility(View.GONE);
            langLinksAdapter.reset();
            actionMode = null;
        }

        @Override
        protected String getSearchHintString() {
            return getResources().getString(R.string.langlinks_filter_hint);
        }

        @Override
        protected boolean finishActionModeIfKeyboardHiding() {
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (languageEntries != null) {
           outState.putParcelableArrayList(LANGUAGE_ENTRIES_BUNDLE_KEY, new ArrayList<Parcelable>(languageEntries));
        }
    }

    private List<PageTitle> getEntriesByAppLanguages() {
        List<PageTitle> list = new ArrayList<>();

        for (PageTitle entry : languageEntries) {
            if (app.language().getAppLanguageCodes().contains(entry.getWikiSite().languageCode())) {
                list.add(entry);
            }
        }

        return list;
    }

    private void displayLangLinks() {
        if (languageEntries.size() == 0) {
            // TODO: Question: should we use the same empty view for the default empty view and search result empty view?
            langLinksEmpty.setEmptyText(R.string.langlinks_empty);
            ViewAnimations.crossFade(langLinksProgress, langLinksEmpty);
        } else {
            adapter = new LangLinksAdapter(languageEntries, getEntriesByAppLanguages());
            langLinksEmpty.setEmptyText(R.string.langlinks_no_match);
            langLinksList.setAdapter(adapter);
            langLinksList.setLayoutManager(new LinearLayoutManager(this));
            searchActionModeCallback = new LanguageSearchCallback();

            disposables.add(ServiceFactory.get(app.getWikiSite()).getSiteMatrix()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(SiteMatrix::getSites)
                    .doFinally(() -> {
                        langLinksProgress.setVisibility(View.INVISIBLE);
                        adapter.notifyDataSetChanged();
                    })
                    .subscribe(sites -> siteInfoList = sites, L::e));

            ViewAnimations.crossFade(langLinksProgress, langLinksList);
        }

        invalidateOptionsMenu();
    }

    private void fetchLangLinks() {
        if (languageEntries == null) {
            disposables.add(ServiceFactory.get(title.getWikiSite()).getLangLinks(title.getPrefixedText())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {
                        languageEntries = response.query().langLinks();
                        updateLanguageEntriesSupported(languageEntries);
                        sortLanguageEntriesByMru(languageEntries);
                        displayLangLinks();

                    }, caught -> {
                        ViewAnimations.crossFade(langLinksProgress, langLinksError);
                        langLinksError.setError(caught);
                    }));
        } else {
            displayLangLinks();
        }
    }

    private void updateLanguageEntriesSupported(List<PageTitle> languageEntries) {
        boolean haveChineseEntry = false;
        for (ListIterator<PageTitle> it = languageEntries.listIterator(); it.hasNext();) {
            PageTitle link = it.next();
            String languageCode = link.getWikiSite().languageCode();

            if (GOTHIC_LANGUAGE_CODE.equals(languageCode)) {
                // Remove Gothic since it causes Android to segfault.
                it.remove();
            } else if ("be-x-old".equals(languageCode)) {
                // Replace legacy name of тарашкевіца language with the correct name.
                // TODO: Can probably be removed when T111853 is resolved.
                it.remove();
                it.add(new PageTitle(link.getText(), WikiSite.forLanguageCode("be-tarask")));
            } else if (AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE.equals(languageCode)) {
                // Replace Chinese with Simplified and Traditional dialects.
                haveChineseEntry = true;
                it.remove();
                for (String dialect : Arrays.asList(AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                        AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE)) {

                    it.add(new PageTitle((title.isMainPage()) ? SiteInfoClient.getMainPageForLang(dialect) : link.getPrefixedText(),
                            WikiSite.forLanguageCode(dialect)));
                }
            }
        }

        if (!haveChineseEntry) {
            addChineseEntriesIfNeeded(title, languageEntries);
        }
    }

    private void sortLanguageEntriesByMru(List<PageTitle> entries) {
        int addIndex = 0;
        for (String language : app.language().getMruLanguageCodes()) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getWikiSite().languageCode().equals(language)) {
                    PageTitle entry = entries.remove(i);
                    entries.add(addIndex++, entry);
                    break;
                }
            }
        }
    }

    @VisibleForTesting
    public static void addChineseEntriesIfNeeded(@NonNull PageTitle title,
                                                 @NonNull List<PageTitle> languageEntries) {

        // TODO: setup PageTitle in correct variant
        if (title.getWikiSite().languageCode().startsWith(AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE)) {
            String[] chineseLanguageCodes = {AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE, AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE};

            for (String languageCode : chineseLanguageCodes) {
                if (!title.getWikiSite().languageCode().contains(languageCode)) {
                    languageEntries.add(new PageTitle((title.isMainPage()) ? SiteInfoClient.getMainPageForLang(languageCode) : title.getPrefixedText(),
                            WikiSite.forLanguageCode(languageCode)));
                }
            }
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private final class LangLinksAdapter extends RecyclerView.Adapter<DefaultViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;
        @NonNull private final List<PageTitle> originalLanguageEntries;
        @NonNull private final List<PageTitle> appLanguageEntries;
        @NonNull private final List<PageTitle> languageEntries = new ArrayList<>();
        private boolean isSearching;

        private LangLinksAdapter(@NonNull List<PageTitle> languageEntries, @NonNull List<PageTitle> appLanguageEntries) {
            originalLanguageEntries = new ArrayList<>(languageEntries);
            this.appLanguageEntries = appLanguageEntries;
            reset();
        }

        @Override
        public int getItemViewType(int position) {
            return shouldShowSectionHeader(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @Override
        public int getItemCount() {
            return this.languageEntries.size();
        }

        @NonNull
        @Override
        public DefaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            if (viewType == VIEW_TYPE_HEADER) {
                View view = inflater.inflate(R.layout.view_section_header, parent, false);
                return new DefaultViewHolder(languageEntries, view);
            } else {
                View view = inflater.inflate(R.layout.item_langlinks_list_entry, parent, false);
                return new LangLinksItemViewHolder(languageEntries, view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder holder, int pos) {
            holder.bindItem(pos);
        }

        boolean shouldShowSectionHeader(int position) {
            return !isSearching
                    && (position == 0  || (appLanguageEntries.size() > 0
                    && position == appLanguageEntries.size() + 1));
        }

        void setFilterText(String filter) {
            isSearching = true;
            languageEntries.clear();
            filter = filter.toLowerCase(Locale.getDefault());
            for (PageTitle entry : originalLanguageEntries) {
                String languageCode = entry.getWikiSite().languageCode();
                String canonicalName = defaultString(app.language().getAppLanguageCanonicalName(languageCode));
                String localizedName = defaultString(app.language().getAppLanguageLocalizedName(languageCode));
                if (canonicalName.toLowerCase(Locale.getDefault()).contains(filter)
                        || localizedName.toLowerCase(Locale.getDefault()).contains(filter)) {
                    languageEntries.add(entry);
                }
            }
            notifyDataSetChanged();
        }

        void reset() {
            isSearching = false;
            this.languageEntries.clear();
            if (appLanguageEntries.size() > 0) {
                languageEntries.add(new PageTitle(getString(R.string.langlinks_your_wikipedia_languages), app.getWikiSite()));
                languageEntries.addAll(appLanguageEntries);
            }

            List<PageTitle> remainingEntries = getNonDuplicateEntries();
            if (remainingEntries.size() > 0) {
                languageEntries.add(new PageTitle(getString(R.string.languages_list_all_text), app.getWikiSite()));
                languageEntries.addAll(remainingEntries);
            }

            notifyDataSetChanged();
        }

        // To remove the already selected languages and suggested languages from all languages list
        private List<PageTitle> getNonDuplicateEntries() {
            List<PageTitle> list = new ArrayList<>(originalLanguageEntries);
            list.removeAll(appLanguageEntries);
            return list;
        }
    }

    private class DefaultViewHolder extends RecyclerView.ViewHolder {
        private TextView sectionHeaderTextView;
        private final List<PageTitle> languageEntries;

        DefaultViewHolder(@NonNull List<PageTitle> languageEntries, View itemView) {
            super(itemView);
            sectionHeaderTextView = itemView.findViewById(R.id.section_header_text);
            this.languageEntries = languageEntries;
        }

        void bindItem(int position) {
            // TODO: Optimize this
            sectionHeaderTextView.setText(languageEntries.get(position).getDisplayText());
        }
    }

    private class LangLinksItemViewHolder extends DefaultViewHolder implements View.OnClickListener {
        private TextView localizedLanguageNameTextView;
        private TextView nonLocalizedLanguageNameTextView;
        private TextView articleTitleTextView;
        private final List<PageTitle> languageEntries;
        private int pos;

        LangLinksItemViewHolder(@NonNull List<PageTitle> languageEntries, View itemView) {
            super(languageEntries, itemView);
            localizedLanguageNameTextView = itemView.findViewById(R.id.localized_language_name);
            nonLocalizedLanguageNameTextView = itemView.findViewById(R.id.non_localized_language_name);
            articleTitleTextView = itemView.findViewById(R.id.language_subtitle);
            this.languageEntries = languageEntries;
        }

        void bindItem(int position) {
            pos = position;
            PageTitle item = languageEntries.get(position);
            String languageCode = item.getWikiSite().languageCode();
            String localizedLanguageName = app.language().getAppLanguageLocalizedName(languageCode);

            localizedLanguageNameTextView.setText(localizedLanguageName == null ? languageCode : StringUtils.capitalize(localizedLanguageName));
            articleTitleTextView.setText(item.getDisplayText());

            if (langLinksProgress.getVisibility() != View.VISIBLE) {
                String canonicalName = getCanonicalName(languageCode);
                if (TextUtils.isEmpty(canonicalName)
                        || languageCode.equals(app.language().getSystemLanguageCode())) {
                    nonLocalizedLanguageNameTextView.setVisibility(View.GONE);
                } else {
                    // TODO: Fix an issue when app language is zh-hant, the subtitle in zh-hans will display in English
                    nonLocalizedLanguageNameTextView.setText(canonicalName);
                    nonLocalizedLanguageNameTextView.setVisibility(View.VISIBLE);
                }
            }
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            PageTitle langLink = languageEntries.get(pos);
            app.language().addMruLanguageCode(langLink.getWikiSite().languageCode());
            HistoryEntry historyEntry = new HistoryEntry(langLink, HistoryEntry.SOURCE_LANGUAGE_LINK);
            Intent intent = PageActivity.newIntentForCurrentTab(LangLinksActivity.this, historyEntry, langLink);
            setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent);
            hideSoftKeyboard(LangLinksActivity.this);
            finish();
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
}
