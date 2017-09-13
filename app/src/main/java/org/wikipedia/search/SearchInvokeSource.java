package org.wikipedia.search;

import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

public enum SearchInvokeSource implements EnumCode {
    TOOLBAR(0),
    WIDGET(1),
    INTENT_SHARE(2),
    INTENT_PROCESS_TEXT(3),
    FEED_BAR(4),
    VOICE(5),
    APP_SHORTCUTS(6);

    private static final EnumCodeMap<SearchInvokeSource> MAP = new EnumCodeMap<>(SearchInvokeSource.class);

    private final int code;

    public static SearchInvokeSource of(int code) {
        return MAP.get(code);
    }

    @Override public int code() {
        return code;
    }

    SearchInvokeSource(int code) {
        this.code = code;
    }

    public boolean fromIntent() {
        return code == WIDGET.code() || code == INTENT_SHARE.code()
                || code == INTENT_PROCESS_TEXT.code();
    }
}
