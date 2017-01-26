package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.ServiceError;

import java.util.Collections;
import java.util.List;

/**
 * Gson POJO for a MediaWiki API error.
 */
public class MwServiceError implements ServiceError {
    @SuppressWarnings("unused") @Nullable private String code;
    @SuppressWarnings("unused") @Nullable private String info;
    @SuppressWarnings("unused") @Nullable private String docref;
    @SuppressWarnings("unused") @NonNull private List<Message> messages = Collections.emptyList();

    @Override @Nullable public String getTitle() {
        return code;
    }

    @Override @Nullable public String getDetails() {
        return info;
    }

    @Nullable public String getDocRef() {
        return docref;
    }

    public boolean badToken() {
        return "badtoken".equals(code);
    }

    public boolean hasMessageName(@NonNull String messageName) {
        for (Message msg : messages) {
            if (messageName.equals(msg.name)) {
                return true;
            }
        }
        return false;
    }

    @Nullable public String getMessageHtml(@NonNull String messageName) {
        for (Message msg : messages) {
            if (messageName.equals(msg.name)) {
                return msg.getHtmlValue();
            }
        }
        return null;
    }

    @Override public String toString() {
        return "MwServiceError{"
                + "code='" + code + '\''
                + ", info='" + info + '\''
                + ", docref='" + docref + '\''
                + '}';
    }

    private static final class Message {
        @SuppressWarnings("unused") private String name;
        @SuppressWarnings("unused") private MessageHtml html;

        @NonNull private String getHtmlValue() {
            return html != null ? html.value : "";
        }
    }

    private static class MessageHtml {
        @SuppressWarnings("unused") @SerializedName("*") private String value;
    }
}
