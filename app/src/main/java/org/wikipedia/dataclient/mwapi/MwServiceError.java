package org.wikipedia.dataclient.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.ServiceError;

import java.time.Instant;
import java.util.List;

/**
 * Gson POJO for a MediaWiki API error.
 */
@SuppressWarnings("unused")
public class MwServiceError implements ServiceError {
    @Nullable private String code;
    @Nullable private String text;
    @Nullable private String html;
    @Nullable private Data data;

    public MwServiceError() {
    }

    public MwServiceError(@Nullable String code, @Nullable String html) {
        this.code = code;
        this.html = html;
    }

    @Override @NonNull public String getTitle() {
        return StringUtils.defaultString(code);
    }

    @SuppressWarnings("checkstyle:magicnumber")
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
        @Nullable private List<Message> messages;
        @Nullable private BlockInfo blockinfo;

        @Nullable private List<Message> messages() {
            return messages;
        }
    }

    private static final class Message {
        @Nullable private String name;
        @Nullable private String html;

        @NonNull private String html() {
            return StringUtils.defaultString(html);
        }
    }

    public static class BlockInfo {
        private int blockid;
        private int blockedbyid;
        @Nullable private String blockreason;
        @Nullable private String blockedby;
        @Nullable private String blockedtimestamp;
        @Nullable private String blockexpiry;

        public int getBlockId() {
            return blockid;
        }

        @NonNull public String getBlockedBy() {
            return StringUtils.defaultString(blockedby);
        }

        @NonNull public String getBlockReason() {
            return StringUtils.defaultString(blockreason);
        }

        @NonNull public String getBlockTimeStamp() {
            return StringUtils.defaultString(blockedtimestamp);
        }

        @NonNull public String getBlockExpiry() {
            return StringUtils.defaultString(blockexpiry);
        }

        public boolean isBlocked() {
            if (TextUtils.isEmpty(blockexpiry)) {
                return false;
            }
            final Instant now = Instant.now();
            final Instant expiry = Instant.parse(blockexpiry);
            return expiry.isAfter(now);
        }
    }
}
