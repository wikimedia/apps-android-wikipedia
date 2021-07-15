package org.wikipedia.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.UriUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Notification {
    public static final String CATEGORY_SYSTEM = "system";
    public static final String CATEGORY_SYSTEM_NO_EMAIL = "system-noemail"; // default welcome
    public static final String MILESTONE_EDIT = "milestone-edit";
    public static final String CATEGORY_EDIT_USER_TALK = "edit-user-talk";
    public static final String CATEGORY_EDIT_THANK = "edit-thank";
    public static final String CATEGORY_REVERTED = "reverted";
    public static final String CATEGORY_LOGIN_FAIL = "login-fail";
    public static final String CATEGORY_MENTION = "mention"; // combines "mention", "mention-failure" and "mention-success"

    @SuppressWarnings("unused") @Nullable private String wiki;
    @SuppressWarnings("unused") private long id;
    @SuppressWarnings("unused") @Nullable private String type;
    @SuppressWarnings("unused") @Nullable private String category;
    @SuppressWarnings("unused") private long revid;

    @SuppressWarnings("unused") @Nullable private Title title;
    @SuppressWarnings("unused") @Nullable private Agent agent;
    @SuppressWarnings("unused") @Nullable private Timestamp timestamp;
    @SuppressWarnings("unused") @SerializedName("*") @Nullable private Contents contents;
    @SuppressWarnings("unused") @Nullable private Map<String, Source> sources;

    @NonNull public String wiki() {
        return StringUtils.defaultString(wiki);
    }

    public long id() {
        return id;
    }

    public long key() {
        return id + wiki().hashCode();
    }

    @NonNull public String type() {
        return StringUtils.defaultString(type);
    }

    @NonNull public String category() {
        return StringUtils.defaultString(category);
    }

    @Nullable public Agent agent() {
        return agent;
    }

    @Nullable public Title title() {
        return title;
    }

    public long revID() {
        return revid;
    }

    @Nullable Contents getContents() {
        return contents;
    }

    @NonNull Date getTimestamp() {
        return timestamp != null ? timestamp.date() : new Date();
    }

    @NonNull String getUtcIso8601() {
        return StringUtils.defaultString(timestamp != null ? timestamp.utciso8601 : null);
    }

    @Nullable Map<String, Source> getSources() {
        return sources;
    }

    public boolean isFromWikidata() {
        return wiki().equals("wikidatawiki");
    }

    @Override public String toString() {
        return Long.toString(id);
    }

    public static class Title {
        @SuppressWarnings("unused") @Nullable private String full;
        @SuppressWarnings("unused") @Nullable private String text;
        @SuppressWarnings("unused") @Nullable private String namespace;
        @SuppressWarnings("unused") @SerializedName("namespace-key") private int namespaceKey;

        @NonNull public String text() {
            return StringUtils.defaultString(text);
        }

        @NonNull public String full() {
            return StringUtils.defaultString(full);
        }

        public boolean isMainNamespace() {
            return namespaceKey == 0;
        }

        public void setFull(@NonNull String title) {
            full = title;
        }
    }

    public static class Agent {
        @SuppressWarnings("unused") private int id;
        @SuppressWarnings("unused") @Nullable private String name;

        @NonNull public String name() {
            return StringUtils.defaultString(name);
        }
    }

    public static class Timestamp {
        @SuppressWarnings("unused") @Nullable private String utciso8601;

        public Date date() {
            return DateUtil.iso8601DateParse(utciso8601);
        }
    }

    public static class Link {
        @SuppressWarnings("unused") @Nullable private String url;
        @SuppressWarnings("unused") @Nullable private String label;
        @SuppressWarnings("unused") @Nullable private String tooltip;
        @SuppressWarnings("unused") @Nullable private String description;
        @SuppressWarnings("unused") @Nullable private String icon;

        @NonNull public String getUrl() {
            return UriUtil.decodeURL(StringUtils.defaultString(url));
        }

        @NonNull public String getTooltip() {
            return StringUtils.defaultString(tooltip);
        }

        @NonNull public String getLabel() {
            return StringUtils.defaultString(label);
        }

        @NonNull public String getIcon() {
            return StringUtils.defaultString(icon);
        }
    }

    public static class Links {
        @SuppressWarnings("unused") @Nullable private JsonElement primary;
        @SuppressWarnings("unused") @Nullable private List<Link> secondary;
        private Link primaryLink;

        @Nullable public Link getPrimary() {
            if (primary == null) {
                return null;
            }
            if (primaryLink == null && primary instanceof JsonObject) {
                primaryLink = GsonUtil.getDefaultGson().fromJson(primary, Link.class);
            }
            return primaryLink;
        }

        @Nullable public List<Link> getSecondary() {
            return secondary;
        }
    }

    public static class Source {
        @SuppressWarnings("unused") @Nullable private String title;
        @SuppressWarnings("unused") @Nullable private String url;
        @SuppressWarnings("unused") @Nullable private String base;

        @NonNull public String getTitle() {
            return StringUtils.defaultString(title);
        }

        @NonNull public String getUrl() {
            return UriUtil.decodeURL(StringUtils.defaultString(url));
        }

        @NonNull public String getBase() {
            return StringUtils.defaultString(base);
        }
    }

    public static class Contents {
        @SuppressWarnings("unused") @Nullable private String header;
        @SuppressWarnings("unused") @Nullable private String compactHeader;
        @SuppressWarnings("unused") @Nullable private String body;
        @SuppressWarnings("unused") @Nullable private String icon;
        @SuppressWarnings("unused") @Nullable private String iconUrl;
        @SuppressWarnings("unused") @Nullable private Links links;

        @NonNull public String getHeader() {
            return StringUtils.defaultString(header);
        }

        @NonNull public String getCompactHeader() {
            return StringUtils.defaultString(compactHeader);
        }

        @NonNull public String getBody() {
            return StringUtils.defaultString(body);
        }

        @NonNull public String getIconUrl() {
            return UriUtil.decodeURL(StringUtils.defaultString(iconUrl));
        }

        @Nullable public Links getLinks() {
            return links;
        }
    }

    public static class UnreadNotificationWikiItem {
        @SuppressWarnings("unused") private int totalCount;
        @SuppressWarnings("unused") @Nullable private Source source;

        public int getTotalCount() {
            return totalCount;
        }

        @Nullable public Source getSource() {
            return source;
        }
    }

    public static class SeenTime {
        @SuppressWarnings("unused") @Nullable private String alert;
        @SuppressWarnings("unused") @Nullable private String message;

        @Nullable public String getAlert() {
            return alert;
        }

        @Nullable public String getMessage() {
            return message;
        }
    }
}
