package org.wikipedia.settings;

import android.content.*;
import android.preference.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import org.wikipedia.*;

import java.util.*;

public class LanguagePreference extends DialogPreference {
    private ListView languagesList;
    private EditText languagesFilter;

    private final String[] languages;
    private final WikipediaApp app;

    public LanguagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setDialogLayoutResource(R.layout.dialog_preference_languages);
        languages = context.getResources().getStringArray(R.array.preference_language_keys);
        app = (WikipediaApp) context.getApplicationContext();
        int langIndex = app.findWikiIndex(app.getPrimaryLanguage());
        setSummary(app.localNameFor(langIndex));
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        languagesFilter = (EditText) view.findViewById(R.id.preference_languages_filter);
        languagesList = (ListView) view.findViewById(R.id.preference_languages_list);

        languagesList.setAdapter(new LanguagesAdapter(languages, app));

        int selectedLangIndex = Arrays.asList(languages).indexOf(app.getPrimaryLanguage());
        languagesList.setItemChecked(selectedLangIndex, true);
        languagesList.setSelection(selectedLangIndex - 1);

        languagesFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                ((LanguagesAdapter) languagesList.getAdapter()).setFilterText(s.toString());
            }
        });

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String lang = (String) languagesList.getAdapter().getItem(languagesList.getCheckedItemPosition());
            app.setPrimaryLanguage(lang);
            int langIndex = app.findWikiIndex(app.getPrimaryLanguage());
            setSummary(app.localNameFor(langIndex));
        }
    }

    public String getCurrentLanguageDisplayString() {
        Locale locale = new Locale(app.getPrimaryLanguage());
        return locale.getDisplayLanguage(locale);
    }

    private static class LanguagesAdapter extends BaseAdapter {
        private final String[] originalLanguages;
        private final ArrayList<String> languages;
        private final WikipediaApp app;

        private LanguagesAdapter(String[] languages, WikipediaApp app) {
            this.originalLanguages = languages;
            this.languages = new ArrayList(Arrays.asList(languages));
            this.app = app;
        }

        public void setFilterText(String filter) {
            this.languages.clear();
            filter = filter.toLowerCase();
            for (String s: originalLanguages) {
                int langIndex = app.findWikiIndex(s);
                String canonicalLang = app.canonicalNameFor(langIndex);
                String localLang = app.localNameFor(langIndex);
                if (s.contains(filter)
                        || canonicalLang.toLowerCase().contains(filter)
                        || localLang.toLowerCase().contains(filter)) {
                    this.languages.add(s);
                }
            }
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return languages.size();
        }

        @Override
        public Object getItem(int position) {
            return languages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_activated_2, parent, false);
            }

            TextView nameText = (TextView) convertView.findViewById(android.R.id.text1);
            TextView localNameText = (TextView) convertView.findViewById(android.R.id.text2);

            String wikiCode = (String) getItem(position);

            int langIndex = app.findWikiIndex(wikiCode);

            nameText.setText(app.canonicalNameFor(langIndex));
            localNameText.setText(app.localNameFor(langIndex));
            return convertView;
        }
    }
}
