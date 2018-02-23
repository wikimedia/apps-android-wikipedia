package org.wikipedia.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.AppLanguageSelectFunnel;
import org.wikipedia.language.SiteMatrixClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class LanguagePreferenceDialog extends AppCompatDialog {
    private ListView languagesList;
    private ProgressBar progressBar;
    @NonNull private final List<String> languageCodes;
    @NonNull private final WikipediaApp app;
    @NonNull private final AppLanguageSelectFunnel funnel;
    private LanguagesAdapter adapter;

    private final SiteMatrixCallback siteMatrixCallback = new SiteMatrixCallback();
    @Nullable private List<SiteMatrixClient.SiteInfo> siteInfoList;

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
        progressBar = findViewById(R.id.preference_languages_progress_bar);

        languagesList.setOnItemClickListener((adapterView, view, i, l) -> {
            String lang = (String) languagesList.getAdapter().getItem(i);
            if (!lang.equals(app.getAppOrSystemLanguageCode())) {
                app.setAppLanguageCode(lang);
                app.setMruLanguageCode(lang);
                funnel.logSelect();
            }
            dismiss();
        });

        adapter = new LanguagesAdapter(languageCodes);
        languagesList.setAdapter(adapter);

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

        progressBar.setVisibility(View.VISIBLE);
        new SiteMatrixClient().request(app.getWikiSite(), siteMatrixCallback);

        funnel.logStart();
    }

    @Override
    public void cancel() {
        funnel.logCancel();
        super.cancel();
    }

    @Nullable private String getCanonicalName(@NonNull String code) {
        String canonicalName = null;
        if (siteInfoList != null) {
            for (SiteMatrixClient.SiteInfo info : siteInfoList) {
                if (code.equals(info.code())) {
                    canonicalName = info.localName();
                }
            }
        }
        if (TextUtils.isEmpty(canonicalName)) {
            canonicalName = app.getAppLanguageCanonicalName(code);
        }
        return canonicalName;
    }

    private final class LanguagesAdapter extends BaseAdapter {
        @NonNull private final List<String> originalLanguageCodes;
        @NonNull private final List<String> languageCodes;

        private LanguagesAdapter(@NonNull List<String> languageCodes) {
            originalLanguageCodes = languageCodes;
            this.languageCodes = new ArrayList<>(originalLanguageCodes);
        }

        void setFilterText(String filter) {
            this.languageCodes.clear();
            filter = StringUtils.stripAccents(filter).toLowerCase(Locale.getDefault());
            for (String code : originalLanguageCodes) {
                String localizedName = StringUtils.stripAccents(defaultString(app.getAppLanguageLocalizedName(code)));
                String canonicalName = StringUtils.stripAccents(defaultString(getCanonicalName(code)));
                if (code.contains(filter)
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

            String canonicalName = getCanonicalName(languageCode);
            if (progressBar.getVisibility() != View.VISIBLE) {
                canonicalNameTextView.setText(TextUtils.isEmpty(canonicalName)
                        ? app.getAppLanguageCanonicalName(languageCode) : canonicalName);
            }

            return convertView;
        }
    }

    private class SiteMatrixCallback implements SiteMatrixClient.Callback {
        @Override
        public void success(@NonNull Call<SiteMatrixClient.SiteMatrix> call, @NonNull List<SiteMatrixClient.SiteInfo> sites) {
            if (!isShowing()) {
                return;
            }
            progressBar.setVisibility(View.INVISIBLE);
            siteInfoList = sites;
            adapter.notifyDataSetInvalidated();
        }

        @Override
        public void failure(@NonNull Call<SiteMatrixClient.SiteMatrix> call, @NonNull Throwable caught) {
            if (!isShowing()) {
                return;
            }
            progressBar.setVisibility(View.INVISIBLE);
            adapter.notifyDataSetInvalidated();
        }
    }
}
