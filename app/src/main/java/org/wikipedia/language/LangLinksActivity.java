package org.wikipedia.language;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import retrofit2.Call;

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
    private LangLinksClient client;

    private ListView langLinksList;
    private View langLinksProgress;
    private View langLinksContainer;
    private View langLinksEmpty;
    private View langLinksNoMatch;
    private WikiErrorView langLinksError;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.page_status_bar_color));

        setContentView(R.layout.activity_langlinks);

        if (!getIntent().getAction().equals(ACTION_LANGLINKS_FOR_TITLE)) {
            throw new RuntimeException("Only ACTION_LANGLINKS_FOR_TITLE is supported");
        }

        langLinksList = findViewById(R.id.langlinks_list);
        langLinksProgress = findViewById(R.id.langlinks_load_progress);
        langLinksContainer = findViewById(R.id.langlinks_list_container);
        langLinksEmpty = findViewById(R.id.langlinks_empty);
        langLinksNoMatch = findViewById(R.id.langlinks_no_match);
        langLinksError = findViewById(R.id.langlinks_error);
        EditText langLinksFilter = findViewById(R.id.langlinks_filter);

        title = getIntent().getParcelableExtra(EXTRA_PAGETITLE);

        if (savedInstanceState != null && savedInstanceState.containsKey(LANGUAGE_ENTRIES_BUNDLE_KEY)) {
            languageEntries = savedInstanceState.getParcelableArrayList(LANGUAGE_ENTRIES_BUNDLE_KEY);
        }

        client = new LangLinksClient();
        fetchLangLinks();

        langLinksError.setRetryClickListener((v) -> {
            ViewAnimations.crossFade(langLinksError, langLinksProgress);
            fetchLangLinks();
        });

        langLinksList.setOnItemClickListener((parent, view, position, id) -> {
            PageTitle langLink = (PageTitle) parent.getAdapter().getItem(position);
            app.setMruLanguageCode(langLink.getWikiSite().languageCode());
            HistoryEntry historyEntry = new HistoryEntry(langLink, HistoryEntry.SOURCE_LANGUAGE_LINK);
            Intent intent = PageActivity.newIntentForCurrentTab(LangLinksActivity.this, historyEntry, langLink);
            setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent);
            hideSoftKeyboard(LangLinksActivity.this);
            finish();
        });

        langLinksFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // the languages might not be loaded yet...
                if (langLinksList.getAdapter() == null) {
                    return;
                }
                ((LangLinksAdapter) langLinksList.getAdapter()).setFilterText(s.toString());

                //Check if there are no languages that match the filter
                if (langLinksList.getAdapter().getCount() == 0) {
                    langLinksNoMatch.setVisibility(View.VISIBLE);
                } else {
                    langLinksNoMatch.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard(this);
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (languageEntries != null) {
           outState.putParcelableArrayList(LANGUAGE_ENTRIES_BUNDLE_KEY, new ArrayList<Parcelable>(languageEntries));
        }
    }

    private void displayLangLinks() {
        if (languageEntries.size() == 0) {
            ViewAnimations.crossFade(langLinksProgress, langLinksEmpty);
        } else {
            langLinksList.setAdapter(new LangLinksAdapter(languageEntries, app));
            ViewAnimations.crossFade(langLinksProgress, langLinksContainer);
        }
    }

    private void fetchLangLinks() {
        if (languageEntries == null) {
            client.request(title.getWikiSite(), title, new ClientCallback());
        } else {
            displayLangLinks();
        }
    }

    private class ClientCallback implements LangLinksClient.Callback {
        @Override public void success(@NonNull Call<MwQueryResponse> call,
                                      @NonNull List<PageTitle> links) {
            languageEntries = links;
            updateLanguageEntriesSupported(languageEntries);
            sortLanguageEntriesByMru(languageEntries);
            displayLangLinks();
        }

        @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                      @NonNull Throwable caught) {
            ViewAnimations.crossFade(langLinksProgress, langLinksError);
            langLinksError.setError(caught);
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

                        it.add(new PageTitle((title.isMainPage()) ? MainPageNameData.valueFor(dialect) : link.getText(),
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
            for (String language : app.getMruLanguageCodes()) {
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getWikiSite().languageCode().equals(language)) {
                        PageTitle entry = entries.remove(i);
                        entries.add(addIndex++, entry);
                        break;
                    }
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
                    languageEntries.add(new PageTitle((title.isMainPage()) ? MainPageNameData.valueFor(languageCode) : title.getText(),
                            WikiSite.forLanguageCode(languageCode)));
                }
            }
        }
    }

    private static final class LangLinksAdapter extends BaseAdapter {
        private final List<PageTitle> originalLanguageEntries;
        private final List<PageTitle> languageEntries;
        private final WikipediaApp app;

        private LangLinksAdapter(List<PageTitle> languageEntries, WikipediaApp app) {
            this.originalLanguageEntries = languageEntries;
            this.languageEntries = new ArrayList<>(originalLanguageEntries);
            this.app = app;
        }

        public void setFilterText(String filter) {
            languageEntries.clear();
            filter = filter.toLowerCase(Locale.getDefault());
            for (PageTitle entry : originalLanguageEntries) {
                String languageCode = entry.getWikiSite().languageCode();
                String canonicalName = defaultString(app.getAppLanguageCanonicalName(languageCode));
                String localizedName = defaultString(app.getAppLanguageLocalizedName(languageCode));
                if (canonicalName.toLowerCase(Locale.getDefault()).contains(filter)
                        || localizedName.toLowerCase(Locale.getDefault()).contains(filter)) {
                    languageEntries.add(entry);
                }
            }
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return languageEntries.size();
        }

        @Override
        public PageTitle getItem(int position) {
            return languageEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language_list_entry, parent, false);
            }

            PageTitle item = getItem(position);
            String languageCode = item.getWikiSite().languageCode();
            String localizedLanguageName = app.getAppLanguageLocalizedName(languageCode);

            TextView localizedLanguageNameTextView = convertView.findViewById(R.id.localized_language_name);
            TextView articleTitleTextView = convertView.findViewById(R.id.language_subtitle);

            localizedLanguageNameTextView.setText(localizedLanguageName);
            articleTitleTextView.setText(item.getDisplayText());

            return convertView;
        }
    }
}
