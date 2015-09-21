package org.wikipedia.page.linkpreview;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Utils;
import org.wikipedia.server.restbase.RbPageLead;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LinkPreviewContents {
    private static final int EXTRACT_MAX_SENTENCES = 2;

    private final PageTitle title;
    public PageTitle getTitle() {
        return title;
    }

    private final String extract;
    public String getExtract() {
        return extract;
    }

    public LinkPreviewContents(@NonNull JSONObject json, @NonNull Site site) throws JSONException {
        title = new PageTitle(json.getString("title"), site);
        extract = makeStringFromSentences(getSentences(removeParens(json.optString("extract")), site), EXTRACT_MAX_SENTENCES);
        if (json.has("thumbnail")) {
            title.setThumbUrl(json.getJSONObject("thumbnail").optString("source"));
        }
        if (json.has("terms") && json.getJSONObject("terms").has("description")) {
            title.setDescription(Utils.capitalizeFirstChar(json.getJSONObject("terms").getJSONArray("description").optString(0)));
        }
    }

    public LinkPreviewContents(@NonNull RbPageLead pageLead, @NonNull Site site) {
        title = new PageTitle(pageLead.getDisplayTitle(), site);
        extract = pageLead.getExtract();
        title.setThumbUrl(pageLead.getLeadImageUrl());
        title.setDescription(pageLead.getDescription());
    }

    /**
     * Remove text contained in parentheses from a string.
     * @param text String to be processed.
     * @return New string that is the same as the original string, but without any
     * content in parentheses.
     */
    public static String removeParens(String text) {
        StringBuilder outStr = new StringBuilder(text.length());
        char c;
        int level = 0;
        int i = 0;
        for (; i < text.length(); i++) {
            c = text.charAt(i);
            if (c == ')' && level == 0) {
                // abort if we have an imbalance of parentheses
                return text;
            }
            if (c == '(') {
                level++;
                continue;
            } else if (c == ')') {
                level--;
                continue;
            }
            if (level == 0) {
                // Remove leading spaces before parentheses
                if (c == ' ' && (i < text.length() - 1) && text.charAt(i + 1) == '(') {
                    continue;
                }
                outStr.append(c);
            }
        }
        // fill in the rest of the string
        if (i + 1 < text.length()) {
            outStr.append(text.substring(i + 1, text.length()));
        }
        // if we had an imbalance of parentheses, then return the original string,
        // instead of the transformed one.
        return (level == 0) ? outStr.toString() : text;
    }

    /**
     * Split a block of text into sentences, taking into account the language in which
     * the text is assumed to be.
     * @param text Text to be transformed into sentences.
     * @param site Site that will provide the language of the given text.
     * @return List of sentences.
     */
    public static List<String> getSentences(String text, Site site) {
        List<String> sentenceList = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(new Locale(site.getLanguageCode()));
        // feed the text into the iterator, with line breaks removed:
        text = text.replaceAll("(\r|\n)", " ");
        iterator.setText(text);
        for (int start = iterator.first(), end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (TextUtils.isGraphic(sentence)) {
                // if it's the first sentence, then remove parentheses from it.
                String formattedSentence = sentenceList.isEmpty() ? removeParens(sentence) : sentence;
                sentenceList.add(formattedSentence);
            }
        }
        // if we couldn't detect any sentences using the BreakIterator, then just return the
        // original text as a single sentence.
        if (sentenceList.isEmpty()) {
            sentenceList.add(text);
        }
        return sentenceList;
    }

    private String makeStringFromSentences(List<String> sentences, int maxSentences) {
        return TextUtils.join(" ", sentences.subList(0, Math.min(maxSentences, sentences.size())));
    }
}
