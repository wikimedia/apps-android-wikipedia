package org.wikipedia.interlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import org.wikipedia.*;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;

import java.util.ArrayList;
import java.util.List;

public class LangLinksActivity extends ThemedActionBarActivity {
    public static final int ACTIVITY_RESULT_LANGLINK_SELECT = 1;

    public static final String ACTION_LANGLINKS_FOR_TITLE = "org.wikipedia.langlinks_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";

    private ArrayList<PageTitle> langLinks;
    private PageTitle title;

    private WikipediaApp app;

    private ListView langLinksList;
    private View langLinksProgress;
    private View langLinksContainer;
    private View langLinksEmpty;
    private View langLinksNoMatch;
    private View langLinksError;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp)getApplicationContext();

        setContentView(R.layout.activity_langlinks);

        if (!getIntent().getAction().equals(ACTION_LANGLINKS_FOR_TITLE)) {
            throw new RuntimeException("Only ACTION_LANGLINKS_FOR_TITLE is supported");
        }

        langLinksList = (ListView) findViewById(R.id.langlinks_list);
        langLinksProgress = findViewById(R.id.langlinks_load_progress);
        langLinksContainer = findViewById(R.id.langlinks_list_container);
        langLinksEmpty = findViewById(R.id.langlinks_empty);
        langLinksNoMatch = findViewById(R.id.langlinks_no_match);
        langLinksError = findViewById(R.id.langlinks_error);
        EditText langLinksFilter = (EditText) findViewById(R.id.langlinks_filter);
        Button langLinksErrorRetry = (Button) findViewById(R.id.langlinks_error_retry);

        title = getIntent().getParcelableExtra(EXTRA_PAGETITLE);

        if (savedInstanceState != null && savedInstanceState.containsKey("langlinks")) {
            langLinks = savedInstanceState.getParcelableArrayList("langlinks");
        }

        fetchLangLinks();

        langLinksErrorRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewAnimations.crossFade(langLinksError, langLinksProgress);
                fetchLangLinks();
            }
        });

        langLinksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle langLink = (PageTitle) parent.getAdapter().getItem(position);
                app.addLanguageToMruList(langLink.getSite().getLanguage());
                HistoryEntry historyEntry = new HistoryEntry(langLink, HistoryEntry.SOURCE_LANGUAGE_LINK);
                Intent intent = new Intent();
                intent.setClass(LangLinksActivity.this, PageActivity.class);
                intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                intent.putExtra(PageActivity.EXTRA_PAGETITLE, langLink);
                intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, historyEntry);
                setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent);
                Utils.hideSoftKeyboard(LangLinksActivity.this);
                finish();
            }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.hideSoftKeyboard(LangLinksActivity.this);
                finish();
                return true;
            default:
                throw new RuntimeException("WAT");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (langLinks != null) {
           outState.putParcelableArrayList("langlinks", langLinks);
        }
    }

    private void displayLangLinks() {
        if (langLinks.size() == 0) {
            ViewAnimations.crossFade(langLinksProgress, langLinksEmpty);
        } else {
            langLinksList.setAdapter(new LangLinksAdapter(langLinks, app));
            ViewAnimations.crossFade(langLinksProgress, langLinksContainer);
        }
    }

    private void fetchLangLinks() {
        if (langLinks == null) {
            new LangLinksFetchTask(this, title) {
                @Override
                public void onFinish(ArrayList<PageTitle> result) {
                    langLinks = result;

                    List<String> mru = app.getLanguageMruList();
                    // Rearrange language list based on the mru list
                    int addIndex = 0;
                    for (String langCode : mru) {
                        for (int i = 0; i < result.size(); i++) {
                            if (langLinks.get(i).getSite().getLanguage().equals(langCode)) {
                                PageTitle preferredLink = langLinks.remove(i);
                                langLinks.add(addIndex++, preferredLink);
                                break;
                            }
                        }
                    }

                    // Also get rid of goddamn 'got', since that just segfaults android everywhere
                    for (int i = 0; i < result.size(); i++) {
                        if (langLinks.get(i).getSite().getLanguage().equals("got")) {
                            langLinks.remove(i);
                            break;
                        }
                    }
                    displayLangLinks();
                }

                @Override
                public void onCatch(Throwable caught) {
                    ViewAnimations.crossFade(langLinksProgress, langLinksError);
                    // Not sure why this is required, but without it tapping retry hides langLinksError
                    // FIXME: INVESTIGATE WHY THIS HAPPENS!
                    // Also happens in {@link PageViewFragment}
                    langLinksError.setVisibility(View.VISIBLE);
                }
            }.execute();
        } else {
            displayLangLinks();
        }
    }

    private static final class LangLinksAdapter extends BaseAdapter {
        private final ArrayList<PageTitle> origLangLinks;
        private final ArrayList<PageTitle> langLinks;
        private final WikipediaApp app;

        private LangLinksAdapter(ArrayList<PageTitle> langLinks, WikipediaApp app) {
            this.origLangLinks = langLinks;
            this.langLinks = new ArrayList<PageTitle>(origLangLinks);
            this.app = app;
        }

        public void setFilterText(String filter) {
            this.langLinks.clear();
            filter = filter.toLowerCase();
            for (PageTitle l: origLangLinks) {
                int langIndex = app.findWikiIndex(l.getSite().getLanguage());
                String canonicalLang = app.canonicalNameFor(langIndex);
                String localLang = app.localNameFor(langIndex);
                if (canonicalLang.toLowerCase().contains(filter)
                        || localLang.toLowerCase().contains(filter)) {
                    this.langLinks.add(l);
                }
            }
            notifyDataSetInvalidated();
        }
        @Override
        public int getCount() {
            return langLinks.size();
        }

        @Override
        public Object getItem(int position) {
            return langLinks.get(position);
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

            TextView langNameText = (TextView) convertView.findViewById(R.id.language_list_language_name);
            TextView langLocalNameText = (TextView) convertView.findViewById(R.id.language_list_language_local_name);

            PageTitle langLink = (PageTitle) getItem(position);

            String wikiCode = langLink.getSite().getLanguage();

            int langIndex = app.findWikiIndex(wikiCode);

            langNameText.setText(app.canonicalNameFor(langIndex));
            langLocalNameText.setText(app.localNameFor(langIndex));

            return convertView;
        }
    }
}