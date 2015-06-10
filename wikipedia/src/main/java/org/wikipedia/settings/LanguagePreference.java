package org.wikipedia.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.List;

import static org.wikipedia.util.StringUtil.emptyIfNull;

public class LanguagePreference extends DialogPreference {
    private static final float LIST_DISABLED_ALPHA = .5f;
    private static final float LIST_ENABLED_ALPHA = 1;

    private SwitchCompat systemLanguageSwitch;
    private EditText languagesFilter;
    private ListView languagesList;

    @NonNull
    private final List<String> languageCodes;

    @NonNull
    private final WikipediaApp app;

    public LanguagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setDialogLayoutResource(R.layout.dialog_preference_languages);

        app = WikipediaApp.getInstance();

        languageCodes = app.getAppMruLanguageCodes();

        updateSummary();

        hideDialogButtons();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        systemLanguageSwitch = (SwitchCompat) view.findViewById(R.id.system_language_switch);
        languagesList = (ListView) view.findViewById(R.id.preference_languages_list);
        languagesFilter = (EditText) view.findViewById(R.id.preference_languages_filter);

        systemLanguageSwitch.setChecked(app.isSystemLanguageEnabled());
        setSystemLanguageEnabled(app.isSystemLanguageEnabled());
        systemLanguageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSystemLanguageEnabled(isChecked);
                if (isChecked) {
                    closeDialog();
                }
            }
        });

        languagesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String lang = (String) languagesList.getAdapter().getItem(i);
                app.setAppLanguageCode(lang);
                app.setMruLanguageCode(lang);
                updateSummary();
                closeDialog();
            }
        });

        languagesList.setAdapter(new LanguagesAdapter(languageCodes, app));

        if (!app.isSystemLanguageEnabled()) {
            int selectedLanguageIndex = languageCodes.indexOf(app.getAppLanguageCode());
            languagesList.setItemChecked(selectedLanguageIndex, true);
        }

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

    private void updateSummary() {
        setSummary(app.getAppLanguageLocalizedName());
    }

    private void setSystemLanguageEnabled(boolean enabled) {
        languagesFilter.setEnabled(!enabled);
        languagesList.setEnabled(!enabled);

        ViewUtil.setAlpha(languagesList, enabled ? LIST_DISABLED_ALPHA : LIST_ENABLED_ALPHA);

        if (enabled) {
            languagesList.clearChoices();
            app.setSystemLanguageEnabled();
        }
    }

    private void hideDialogButtons() {
        setPositiveButtonText(null);
        setNegativeButtonText(null);
    }

    private void closeDialog() {
        getDialog().dismiss();
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
            filter = filter.toLowerCase();
            for (String code : originalLanguageCodes) {
                String localizedName = emptyIfNull(app.getAppLanguageLocalizedName(code));
                String canonicalName = emptyIfNull(app.getAppLanguageCanonicalName(code));
                if (code != null && code.contains(filter)
                        || localizedName.toLowerCase().contains(filter)
                        || canonicalName.toLowerCase().contains(filter)) {
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
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_item_activated_2, parent, false);
            }

            TextView localizedNameTextView = (TextView) convertView.findViewById(android.R.id.text1);
            TextView canonicalNameTextView = (TextView) convertView.findViewById(android.R.id.text2);

            String languageCode = getItem(position);

            localizedNameTextView.setText(app.getAppLanguageLocalizedName(languageCode));
            canonicalNameTextView.setText(app.getAppLanguageCanonicalName(languageCode));

            return convertView;
        }
    }
}
