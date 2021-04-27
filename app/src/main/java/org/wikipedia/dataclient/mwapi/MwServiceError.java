package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.ServiceError;

import java.util.List;

/**
 * Gson POJO for a MediaWiki API error.
 */
public class MwServiceError implements ServiceError {
    @SuppressWarnings("unused") @Nullable private String code;
    @SuppressWarnings("unused") @Nullable private String text;
    @SuppressWarnings("unused") @Nullable private String html;
    @SuppressWarnings("unused") @Nullable private Data data;

    @Override @NonNull public String getTitle() {
        return StringUtils.defaultString(code);
    }

    @Override @NonNull public String getDetails() {
        return StringUtils.defaultString(html);
    }

    public boolean badToken() {
        return "badtoken".equals(code);
    }

    public boolean badLoginState() {
        return "assertuserfailed".equals(code);
    }

    public boolean hasMessageName(@NonNull String messageName) {
        if (data != null && data.messages() != null) {
            for (Message msg : data.messages()) {
                if (messageName.equals(msg.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable public String getMessageHtml(@NonNull String messageName) {
        if (data != null && data.messages() != null) {
            for (Message msg : data.messages()) {
                if (messageName.equals(msg.name)) {
                    return msg.html();
                }
            }
        }
        return null;
    }

    private static final class Data {
        @SuppressWarnings("unused") @Nullable private List<Message> messages;

        @Nullable private List<Message> messages() {
            return messages;
        }
    }

    private static final class Message {
        @SuppressWarnings("unused") @Nullable private String name;
        @SuppressWarnings("unused") @Nullable private String html;

        @NonNull private String html() {
            return StringUtils.defaultString(html);
        }
    }
}
