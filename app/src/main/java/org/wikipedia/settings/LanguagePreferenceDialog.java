package org.wikipedia.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.AppLanguageSelectFunnel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class LanguagePreferenceDialog extends AppCompatDialog {
    private ListView languagesList;

    @NonNull
    private final List<String> languageCodes;

    @NonNull
    private final WikipediaApp app;

    @NonNull
    private final AppLanguageSelectFunnel funnel;

    public LanguagePreferenceDialog(Context context, boolean initiatedFromSearchBar) {
        super(context);
        setContentView(R.layout.dialog_preference_languages);

        app = WikipediaApp.getInstance();
        languageCodes = app.getAppMruLanguageCodes();
        funnel = new AppLanguageSelectFunnel(initiatedFromSearchBar);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }

        languagesList = findViewById(R.id.preference_languages_list);
        EditText languagesFilter = findViewById(R.id.preference_languages_filter);

        languagesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String lang = (String) languagesList.getAdapter().getItem(i);
                if (!lang.equals(app.getAppOrSystemLanguageCode())) {
                    app.setAppLanguageCode(lang);
                    app.setMruLanguageCode(lang);
                    funnel.logSelect();
                }
                dismiss();
            }
        });

        languagesList.setAdapter(new LanguagesAdapter(languageCodes, app));

        int selectedLanguageIndex = languageCodes.indexOf(app.getAppLanguageCode());
        languagesList.setItemChecked(selectedLanguageIndex, true);

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

        funnel.logStart();
    }

    @Override
    public void cancel() {
        funnel.logCancel();
        super.cancel();
    }

    private static final class LanguagesAdapter extends BaseAdapter {
        @NonNull
        private final List<String> originalLanguageCodes;
        @NonNull
        private final List<String> languageCodes;

        @NonNull
        private final WikipediaApp app;

        private LanguagesAdapter(@NonNull List<String> languageCodes, @NonNull WikipediaApp app) {
            originalLanguageCodes = languageCodes;
            this.languageCodes = new ArrayList<>(originalLanguageCodes);
            this.app = app;
        }

        public void setFilterText(String filter) {
            this.languageCodes.clear();
            filter = filter.toLowerCase(Locale.getDefault());
            for (String code : originalLanguageCodes) {
                String localizedName = defaultString(app.getAppLanguageLocalizedName(code));
                String canonicalName = defaultString(app.getAppLanguageCanonicalName(code));
                if (code != null && code.contains(filter)
                        || localizedName.toLowerCase(Locale.getDefault()).contains(filter)
                        || canonicalName.toLowerCase(Locale.getDefault()).contains(filter)) {
                    this.languageCodes.add(code);
                }
            }
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return languageCodes.size();
        }

        @Override
        public String getItem(int position) {
            return languageCodes.get(position);
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

            TextView localizedNameTextView = convertView.findViewById(R.id.localized_language_name);
            TextView canonicalNameTextView = convertView.findViewById(R.id.language_subtitle);

            String languageCode = getItem(position);

            localizedNameTextView.setText(app.getAppLanguageLocalizedName(languageCode));
            canonicalNameTextView.setText(app.getAppLanguageCanonicalName(languageCode));

            return convertView;
        }
    }
}
