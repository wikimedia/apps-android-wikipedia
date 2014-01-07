package org.wikipedia.interlanguage;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.wikipedia.Page;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Utils;

import java.util.ArrayList;
import java.util.Locale;

public class LangLinksActivity extends Activity {
    public static final String ACTION_LANGLINKS_FOR_TITLE = "org.wikipedia.langlinks_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";

    private ArrayList<PageTitle> langLinks;
    private PageTitle title;

    private ListView langLinksList;
    private EditText langLinksFilter;
    private View langLinksProgress;
    private View langLinksContainer;
    private View langLinksEmpty;
    private View langLinksError;
    private Button langLinksErrorRetry;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_langlinks);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getIntent().getAction().equals(ACTION_LANGLINKS_FOR_TITLE)) {
            throw new RuntimeException("Only ACTION_LANGLINKS_FOR_TITLE is supported");
        }

        langLinksList = (ListView) findViewById(R.id.langlinks_list);
        langLinksFilter = (EditText) findViewById(R.id.langlinks_filter);
        langLinksProgress = findViewById(R.id.langlinks_load_progress);
        langLinksContainer = findViewById(R.id.langlinks_list_container);
        langLinksEmpty = findViewById(R.id.langlinks_empty);
        langLinksError = findViewById(R.id.langlinks_error);
        langLinksErrorRetry = (Button) findViewById(R.id.langlinks_error_retry);

        title = getIntent().getParcelableExtra(EXTRA_PAGETITLE);

        if (savedInstanceState != null && savedInstanceState.containsKey("langlinks")) {
            langLinks = savedInstanceState.getParcelableArrayList("langlinks");
        }

        fetchLangLinks();

        langLinksErrorRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.crossFade(langLinksError, langLinksProgress);
                fetchLangLinks();
            }
        });
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
            Utils.crossFade(langLinksProgress, langLinksEmpty);
        } else {
            langLinksList.setAdapter(new LangLinksAdapter(langLinks));
            Utils.crossFade(langLinksProgress, langLinksContainer);
        }
    }

    private void fetchLangLinks() {
        if (langLinks == null) {
            new LangLinksFetchTask(this, title) {
                @Override
                public void onFinish(ArrayList<PageTitle> result) {
                    langLinks = result;
                    displayLangLinks();
                }

                @Override
                public void onCatch(Throwable caught) {
                    Utils.crossFade(langLinksProgress, langLinksError);
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

    private static class LangLinksAdapter extends BaseAdapter {
        private final ArrayList<PageTitle> origLangLinks;
        private final ArrayList<PageTitle> langLinks;

        private LangLinksAdapter(ArrayList<PageTitle> langLinks) {
            this.origLangLinks = langLinks;
            this.langLinks = new ArrayList<PageTitle>(origLangLinks);
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

            TextView titleText = (TextView) convertView.findViewById(R.id.language_list_page_title);
            TextView langNameText = (TextView) convertView.findViewById(R.id.language_list_language_name);
            TextView langLocalNameText = (TextView) convertView.findViewById(R.id.language_list_language_local_name);

            PageTitle langLink = (PageTitle) getItem(position);

            Locale locale = new Locale(Utils.toJavaLanguageCode(langLink.getSite().getLanguage()));

            langNameText.setText(locale.getDisplayLanguage(locale));
            if (locale.getDisplayLanguage().equals(langNameText.getText().toString())) {
                langLocalNameText.setText("");
            } else {
                langLocalNameText.setText(locale.getDisplayLanguage());
            }
            titleText.setText(langLink.getDisplayText());

            return convertView;
        }
    }
}