package org.wikipedia.editing;

public abstract class EditingResult {
    private final String result;

    public EditingResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
