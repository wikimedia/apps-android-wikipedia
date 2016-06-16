package org.wikipedia.wiktionary;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.WiktionaryDialogFunnel;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.MainActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.PageServiceFactory;
import org.wikipedia.server.PageService;
import org.wikipedia.server.restbase.RbPageService;
import org.wikipedia.server.restbase.RbDefinition;
import org.wikipedia.util.log.L;
import org.wikipedia.views.AppTextView;

import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;
import static org.wikipedia.util.StringUtil.addUnderscores;
import static org.wikipedia.util.StringUtil.removeUnderscores;
import static org.wikipedia.util.StringUtil.hasSectionAnchor;
import static org.wikipedia.util.StringUtil.removeSectionAnchor;

public class WiktionaryDialog extends ExtendedBottomSheetDialogFragment {
    private static final String WIKTIONARY_DOMAIN = ".wiktionary.org";
    private static final String TITLE = "title";
    private static final String SELECTED_TEXT = "selected_text";
    private static final String PATH_WIKI = "/wiki/";
    private static final String PATH_CURRENT = "./";

    private static String[] ENABLED_LANGUAGES = {
            "en" // English
    };

    private ProgressBar progressBar;
    private PageTitle pageTitle;
    private String selectedText;
    private RbDefinition currentDefinition;
    private View rootView;
    private WiktionaryDialogFunnel funnel;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.dialog_wiktionary, container);
        progressBar = (ProgressBar) rootView.findViewById(R.id.dialog_wiktionary_progress);

        TextView titleText = (TextView) rootView.findViewById(R.id.wiktionary_definition_dialog_title);
        titleText.setText(sanitizeForDialogTitle(selectedText));
        setConditionalLayoutDirection(rootView, pageTitle.getSite().languageCode());

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
        //       https://wiktionary.org.
        PageService pageService = PageServiceFactory.create(new Site(pageTitle.getSite().languageCode() + WIKTIONARY_DOMAIN));
        if (pageService instanceof RbPageService) {
            ((RbPageService) pageService).define(
                    addUnderscores(selectedText),
                    definitionOnLoadCallback);
        } else {
            L.e("Wiktionary definitions require mobile content service loading!");
            displayNoDefinitionsFound();
        }
    }

    private RbDefinition.Callback definitionOnLoadCallback = new RbDefinition.Callback() {
        @Override
        public void success(RbDefinition definition) {
            if (!isAdded()) {
                return;
            }
            if (!definition.hasError()) {
                progressBar.setVisibility(View.GONE);
                currentDefinition = definition;
                layOutDefinitionsByUsage();
            } else {
                definition.logError("Wiktionary definition request failed");
            }
        }

        @Override
        public void failure(Throwable throwable) {
            if (!isAdded()) {
                return;
            }
            displayNoDefinitionsFound();
            L.e("Wiktionary definition fetch error: " + throwable);
        }
    };

    private void displayNoDefinitionsFound() {
        TextView noDefinitionsFoundView = (TextView) rootView.findViewById(R.id.wiktionary_no_definitions_found);
        noDefinitionsFoundView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void layOutDefinitionsByUsage() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout fullDefinitionsList = (LinearLayout) rootView.findViewById(R.id.wiktionary_definitions_by_part_of_speech);

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
        TextView partOfSpeechView = (TextView) usageView.findViewById(R.id.wiktionary_part_of_speech);
        partOfSpeechView.setText(currentUsage.getPartOfSpeech());
        LinearLayout definitionsForPartOfSpeechList = (LinearLayout) usageView.findViewById(R.id.list_wiktionary_definitions_with_examples);

        for (int i = 0; i < currentUsage.getDefinitions().length; i++) {
            View definitionContainerView = inflater.inflate(R.layout.item_wiktionary_definition_with_examples, (ViewGroup) rootView, false);
            layOutDefinitionWithExamples(currentUsage.getDefinitions()[i], definitionContainerView, inflater, i + 1);
            definitionsForPartOfSpeechList.addView(definitionContainerView);
        }
    }

    private void layOutDefinitionWithExamples(RbDefinition.Definition currentDefinition, View definitionContainerView, LayoutInflater inflater, int count) {
        AppTextView definitionView = (AppTextView) definitionContainerView.findViewById(R.id.wiktionary_definition);
        String definitionWithCount = getCounterString(count) + currentDefinition.getDefinition();
        definitionView.setText(Html.fromHtml(definitionWithCount));
        definitionView.setMovementMethod(linkMovementMethod);

        LinearLayout examplesView = (LinearLayout) definitionContainerView.findViewById(R.id.wiktionary_examples);
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
            exampleView.setText(Html.fromHtml(example));
            exampleView.setMovementMethod(linkMovementMethod);
            examplesView.addView(exampleView);
        }
    }

    private LinkMovementMethodExt linkMovementMethod =
            new LinkMovementMethodExt(new LinkMovementMethodExt.UrlHandler() {
                @Override
                public void onUrlClick(String url) {
                    if (url.startsWith(PATH_WIKI) || url.startsWith(PATH_CURRENT)) {
                        dismiss();
                        if (currentPageFragmentExists()) {
                            showNewDialogForLink(url);
                        }
                    }
                }
            });

    private boolean currentPageFragmentExists() {
        return getActivity() != null && getActivity() instanceof MainActivity
                && ((MainActivity) getActivity()).getCurPageFragment() != null;
    }

    private String getTermFromWikiLink(String url) {
        return removeLinkFragment(url.substring(url.lastIndexOf("/") + 1));
    }

    private String removeLinkFragment(String url) {
        return url.split("#")[0];
    }

    private void showNewDialogForLink(String url) {
        ((MainActivity) getActivity()).getCurPageFragment().getShareHandler()
                .showWiktionaryDefinition(getTermFromWikiLink(url));
    }

    private String sanitizeForDialogTitle(String text) {
        if (hasSectionAnchor(text)) {
            text = removeSectionAnchor(text);
        }
        return removeUnderscores(text);
    }
}
