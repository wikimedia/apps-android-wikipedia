package org.wikimedia.wikipedia.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.wikimedia.wikipedia.R;
import org.wikimedia.wikipedia.Utils;
import org.wikimedia.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

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
        setSummary(Utils.getLangDisplayString(app.getPrimaryLanguage()));
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        languagesFilter = (EditText) view.findViewById(R.id.preference_languages_filter);
        languagesList = (ListView) view.findViewById(R.id.preference_languages_list);

        languagesList.setAdapter(new LanguagesAdapter(languages));

        int selectedLangIndex = Arrays.asList(languages).indexOf(app.getPrimaryLanguage());
        languagesList.setItemChecked(selectedLangIndex, true);
        languagesList.setSelection(selectedLangIndex - 1);

        languagesFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                ((LanguagesAdapter)languagesList.getAdapter()).setFilterText(s.toString());
            }
        });

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String lang = (String) languagesList.getAdapter().getItem(languagesList.getCheckedItemPosition());
            app.setPrimaryLanguage(lang);
            setSummary(Utils.getLangDisplayString(lang));
        }
    }

    public String getCurrentLanguageDisplayString() {
        Locale locale = new Locale(app.getPrimaryLanguage());
        return locale.getDisplayLanguage(locale);
    }

    private static class LanguagesAdapter extends BaseAdapter {
        private final String[] originalLanguages;
        private final ArrayList<String> languages;

        private LanguagesAdapter(String[] languages) {
            this.originalLanguages = languages;
            this.languages = new ArrayList(Arrays.asList(languages));
        }

        public void setFilterText(String filter) {
            this.languages.clear();
            filter = filter.toLowerCase();
            for (String s: originalLanguages) {
                Locale locale = new Locale(Utils.toJavaLanguageCode(s));
                if (s.contains(filter)
                        || locale.getDisplayLanguage().toLowerCase().contains(filter)
                        || locale.getDisplayLanguage(locale).toLowerCase().contains(filter)) {
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

            Locale locale = new Locale(Utils.toJavaLanguageCode((String) getItem(position)));

            nameText.setText(locale.getDisplayLanguage(locale));
            localNameText.setText(locale.getDisplayLanguage());
            return convertView;
        }
    }
}
