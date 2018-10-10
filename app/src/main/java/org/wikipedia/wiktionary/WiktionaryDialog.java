package org.wikipedia.wiktionary;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.WiktionaryDialogFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.RbDefinition;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.AppTextView;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;
import static org.wikipedia.util.StringUtil.addUnderscores;
import static org.wikipedia.util.StringUtil.hasSectionAnchor;
import static org.wikipedia.util.StringUtil.removeSectionAnchor;
import static org.wikipedia.util.StringUtil.removeUnderscores;

public class WiktionaryDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        void wiktionaryShowDialogForTerm(@NonNull String term);
    }

    private static final String WIKTIONARY_DOMAIN = ".wiktionary.org";
    private static final String TITLE = "title";
    private static final String SELECTED_TEXT = "selected_text";
    private static final String PATH_WIKI = "/wiki/";
    private static final String PATH_CURRENT = "./";

    // Try to get the correct definition from glossary terms: https://en.wiktionary.org/wiki/Appendix:Glossary
    private static String GLOSSARY_OF_TERMS = ":Glossary";

    private static String[] ENABLED_LANGUAGES = {
            "en" // English
    };

    private ProgressBar progressBar;
    private PageTitle pageTitle;
    private String selectedText;
    private RbDefinition currentDefinition;
    private View rootView;
    private WiktionaryDialogFunnel funnel;
    private CompositeDisposable disposables = new CompositeDisposable();

    public static WiktionaryDialog newInstance(@NonNull PageTitle title, @NonNull String selectedText) {
        WiktionaryDialog dialog = new WiktionaryDialog();
        Bundle args = new Bundle();
        args.putParcelable(TITLE, title);
        args.putString(SELECTED_TEXT, selectedText);
        dialog.setArguments(args);
        return dialog;
    }

    public static String[] getEnabledLanguages() {
        return ENABLED_LANGUAGES;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = getArguments().getParcelable(TITLE);
        selectedText = getArguments().getString(SELECTED_TEXT);
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.dialog_wiktionary, container);
        progressBar = rootView.findViewById(R.id.dialog_wiktionary_progress);

        TextView titleText = rootView.findViewById(R.id.wiktionary_definition_dialog_title);
        titleText.setText(sanitizeForDialogTitle(selectedText));
        setConditionalLayoutDirection(rootView, pageTitle.getWikiSite().languageCode());

        loadDefinitions();

        funnel = new WiktionaryDialogFunnel(WikipediaApp.getInstance(), selectedText);

        return rootView;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        funnel.logClose();
    }

    private void loadDefinitions() {
        if (selectedText.trim().isEmpty()) {
            displayNoDefinitionsFound();
            return;
        }

        // TODO: centralize the Wiktionary domain better. Maybe a SharedPreference that defaults to
        disposables.add(ServiceFactory.getRest(new WikiSite(pageTitle.getWikiSite().subdomain() + WIKTIONARY_DOMAIN)).getDefinition(addUnderscores(selectedText))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(RbDefinition::new)
                .subscribe(definition -> {
                    progressBar.setVisibility(View.GONE);
                    currentDefinition = definition;
                    layOutDefinitionsByUsage();
                }, throwable -> {
                    displayNoDefinitionsFound();
                    L.e(throwable);
                }));
    }

    private void displayNoDefinitionsFound() {
        TextView noDefinitionsFoundView = rootView.findViewById(R.id.wiktionary_no_definitions_found);
        noDefinitionsFoundView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void layOutDefinitionsByUsage() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout fullDefinitionsList = rootView.findViewById(R.id.wiktionary_definitions_by_part_of_speech);

        RbDefinition.Usage[] usageList = currentDefinition.getUsagesForLang("en");
        if (usageList == null || usageList.length == 0) {
            displayNoDefinitionsFound();
            return;
        }
        for (RbDefinition.Usage usage : usageList) {
            View usageView = inflater.inflate(R.layout.item_wiktionary_definitions_list, (ViewGroup) rootView, false);
            layOutUsage(usage, usageView, inflater);
            fullDefinitionsList.addView(usageView);
        }
    }

    private void layOutUsage(RbDefinition.Usage currentUsage, View usageView, LayoutInflater inflater) {
        TextView partOfSpeechView = usageView.findViewById(R.id.wiktionary_part_of_speech);
        partOfSpeechView.setText(currentUsage.getPartOfSpeech());
        LinearLayout definitionsForPartOfSpeechList = usageView.findViewById(R.id.list_wiktionary_definitions_with_examples);

        for (int i = 0; i < currentUsage.getDefinitions().length; i++) {
            View definitionContainerView = inflater.inflate(R.layout.item_wiktionary_definition_with_examples, (ViewGroup) rootView, false);
            layOutDefinitionWithExamples(currentUsage.getDefinitions()[i], definitionContainerView, inflater, i + 1);
            definitionsForPartOfSpeechList.addView(definitionContainerView);
        }
    }

    private void layOutDefinitionWithExamples(RbDefinition.Definition currentDefinition, View definitionContainerView, LayoutInflater inflater, int count) {
        AppTextView definitionView = definitionContainerView.findViewById(R.id.wiktionary_definition);
        String definitionWithCount = getCounterString(count) + currentDefinition.getDefinition();
        definitionView.setText(StringUtil.fromHtml(definitionWithCount));
        definitionView.setMovementMethod(linkMovementMethod);

        LinearLayout examplesView = definitionContainerView.findViewById(R.id.wiktionary_examples);
        if (currentDefinition.getExamples() != null) {
            layoutExamples(currentDefinition.getExamples(), examplesView, inflater);
        }
    }

    private String getCounterString(int count) {
        return count + ". ";
    }

    private void layoutExamples(String[] examples, LinearLayout examplesView, LayoutInflater inflater) {
        for (String example : examples) {
            AppTextView exampleView = (AppTextView) inflater.inflate(R.layout.item_wiktionary_example, (ViewGroup) rootView, false);
            exampleView.setText(StringUtil.fromHtml(example));
            exampleView.setMovementMethod(linkMovementMethod);
            examplesView.addView(exampleView);
        }
    }

    private LinkMovementMethodExt linkMovementMethod =
            new LinkMovementMethodExt((@NonNull String url) -> {
                if (url.startsWith(PATH_WIKI) || url.startsWith(PATH_CURRENT)) {
                    dismiss();
                    showNewDialogForLink(url);
                }
            });

    private String getTermFromWikiLink(String url) {
        return removeLinkFragment(url.substring(url.lastIndexOf("/") + 1));
    }

    private String removeLinkFragment(String url) {

        String[] splitUrl = url.split("#");

        return (splitUrl[0].endsWith(GLOSSARY_OF_TERMS) && splitUrl.length > 1) ? splitUrl[1] : splitUrl[0];
    }

    private void showNewDialogForLink(String url) {
        Callback callback = callback();
        if (callback != null) {
            callback.wiktionaryShowDialogForTerm(getTermFromWikiLink(url));
        }
    }

    private String sanitizeForDialogTitle(String text) {
        if (hasSectionAnchor(text)) {
            text = removeSectionAnchor(text);
        }
        return removeUnderscores(text);
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
