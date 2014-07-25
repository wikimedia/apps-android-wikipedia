package org.wikipedia.search;

public class FullSearchSuggestionException extends Exception {
    private final String suggestion;

    public FullSearchSuggestionException(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSuggestion() {
        return suggestion;
    }

}
