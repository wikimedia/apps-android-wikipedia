package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.Protection;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.notifications.Notification;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.SiteInfo;
import org.wikipedia.util.DateUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class MwQueryResult implements PostProcessingTypeAdapter.PostProcessable {
    @Nullable private List<MwQueryPage> pages;
    @Nullable private List<Redirect> redirects;
    @Nullable private List<ConvertedTitle> converted;
    @SerializedName("userinfo") private UserInfo userInfo;
    @Nullable private List<ListUserResponse> users;
    @Nullable private Tokens tokens;
    @SerializedName("authmanagerinfo") @Nullable private MwAuthManagerInfo amInfo;
    @Nullable private MarkReadResponse echomarkread;
    @Nullable private MarkReadResponse echomarkseen;
    @Nullable private NotificationList notifications;
    @Nullable private Map<String, Notification.UnreadNotificationWikiItem> unreadnotificationpages;
    @SerializedName("general") @Nullable private SiteInfo generalSiteInfo;
    @SerializedName("wikimediaeditortaskscounts") @Nullable private EditorTaskCounts editorTaskCounts;
    @Nullable private List<WatchlistItem> watchlist;
    @SerializedName("usercontribs") @Nullable private List<UserContribution> userContributions;

    @Nullable public List<MwQueryPage> pages() {
        return pages;
    }

    @Nullable public MwQueryPage firstPage() {
        if (pages != null && pages.size() > 0) {
            return pages.get(0);
        }
        return null;
    }

    @Nullable public UserInfo userInfo() {
        return userInfo;
    }

    @Nullable public String csrfToken() {
        return tokens != null ? tokens.csrf() : null;
    }

    @Nullable public String watchToken() {
        return  tokens != null ? tokens.watch() : null;
    }

    @Nullable public String createAccountToken() {
        return tokens != null ? tokens.createAccount() : null;
    }

    @Nullable public String loginToken() {
        return tokens != null ? tokens.login() : null;
    }

    @Nullable public NotificationList notifications() {
        return notifications;
    }

    @Nullable public Map<String, Notification.UnreadNotificationWikiItem> unreadNotificationWikis() {
        return unreadnotificationpages;
    }

    @Nullable public MarkReadResponse getEchoMarkSeen() {
        return echomarkseen;
    }

    @Nullable public String captchaId() {
        String captchaId = null;
        if (amInfo != null) {
            for (MwAuthManagerInfo.Request request : amInfo.requests()) {
                if ("CaptchaAuthenticationRequest".equals(request.id())) {
                    captchaId = request.fields().get("captchaId").value();
                }
            }
        }
        return captchaId;
    }

    @Nullable public ListUserResponse getUserResponse(@NonNull String userName) {
        if (users != null) {
            for (ListUserResponse user : users) {
                // MediaWiki user names are case sensitive, but the first letter is always capitalized.
                if (StringUtils.capitalize(userName).equals(user.name())) {
                    return user;
                }
            }
        }
        return null;
    }

    @NonNull public List<PageTitle> langLinks() {
        List<PageTitle> result = new ArrayList<>();
        if (pages == null || pages.isEmpty() || pages.get(0).langLinks() == null) {
            return result;
        }
        // noinspection ConstantConditions
        for (MwQueryPage.LangLink link : pages.get(0).langLinks()) {
            PageTitle title = new PageTitle(link.title(), WikiSite.forLanguageCode(link.lang()));
            result.add(title);
        }
        return result;
    }

    @Nullable public SiteInfo siteInfo() {
        return generalSiteInfo;
    }

    @Nullable public EditorTaskCounts editorTaskCounts() {
        return editorTaskCounts;
    }

    @NonNull public List<UserContribution> userContributions() {
        return userContributions != null ? userContributions : Collections.emptyList();
    }

    @NonNull public List<WatchlistItem> getWatchlist() {
        return watchlist != null ? watchlist : Collections.emptyList();
    }

    public boolean isEditProtected() {
        if (firstPage() == null || userInfo() == null) {
            return false;
        }
        for (Protection protection : firstPage().protection()) {
            if (protection.getType().equals("edit") && !userInfo().groups().contains(protection.getLevel())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postProcess() {
        resolveConvertedTitles();
        resolveRedirectedTitles();
    }

    private void resolveRedirectedTitles() {
        if (redirects == null || pages == null) {
            return;
        }
        for (MwQueryPage page : pages) {
            for (MwQueryResult.Redirect redirect : redirects) {
                // TODO: Looks like result pages and redirects can also be matched on the "index"
                // property.  Confirm in the API docs and consider updating.
                if (page.title().equals(redirect.to())) {
                    page.redirectFrom(redirect.from());
                    if (redirect.toFragment() != null) {
                        page.appendTitleFragment(redirect.toFragment());
                    }
                }
            }
        }
    }

    private void resolveConvertedTitles() {
        if (converted == null || pages == null) {
            return;
        }
        for (MwQueryResult.ConvertedTitle convertedTitle : converted) {
            for (MwQueryPage page : pages) {
                if (page.title().equals(convertedTitle.to())) {
                    page.convertedFrom(convertedTitle.from());
                    page.convertedTo(convertedTitle.to());
                }
            }
        }
    }

    private static class Redirect {
        @SuppressWarnings("unused") private int index;
        @SuppressWarnings("unused") @Nullable private String from;
        @SuppressWarnings("unused") @Nullable private String to;
        @SuppressWarnings("unused") @SerializedName("tofragment") @Nullable private String toFragment;

        @Nullable public String to() {
            return to;
        }

        @Nullable public String from() {
            return from;
        }

        @Nullable public String toFragment() {
            return toFragment;
        }
    }

    public static class ConvertedTitle {
        @SuppressWarnings("unused") @Nullable private String from;
        @SuppressWarnings("unused") @Nullable private String to;

        @Nullable public String to() {
            return to;
        }

        @Nullable public String from() {
            return from;
        }
    }

    private static class Tokens {
        @SuppressWarnings("unused,NullableProblems") @SerializedName("csrftoken")
        @Nullable private String csrf;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("createaccounttoken")
        @Nullable private String createAccount;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("logintoken")
        @Nullable private String login;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("watchtoken")
        @Nullable private String watch;

        @Nullable private String csrf() {
            return csrf;
        }
        @Nullable private String watch() {
            return watch;
        }

        @Nullable private String createAccount() {
            return createAccount;
        }

        @Nullable private String login() {
            return login;
        }
    }

    public static class MarkReadResponse {
        @SuppressWarnings("unused") @Nullable private String result;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String timestamp;

        @Nullable public String getResult() {
            return result;
        }

        @Nullable public String getTimestamp() {
            return timestamp;
        }
    }

    public static class NotificationList {
        @SuppressWarnings("unused") private int count;
        @SuppressWarnings("unused") private int rawcount;
        @SuppressWarnings("unused") @Nullable private Notification.SeenTime seenTime;
        @SuppressWarnings("unused") @Nullable private List<Notification> list;
        @SuppressWarnings("unused") @SerializedName("continue") @Nullable private String continueStr;

        @Nullable public List<Notification> list() {
            return list;
        }

        @Nullable public String getContinue() {
            return continueStr;
        }

        public int getCount() {
            return count;
        }

        @Nullable public Notification.SeenTime getSeenTime() {
            return seenTime;
        }
    }

    public static class WatchlistItem {
        private int pageid;
        private long revid;
        @SerializedName("old_revid") private long oldRevid;
        private int ns;
        @Nullable private String title;
        @Nullable private String user;
        @Nullable private String timestamp;
        @Nullable private String comment;
        @Nullable private String parsedcomment;
        @Nullable private String logtype;
        private boolean anon;
        private boolean bot;
        @SerializedName("new") private boolean isNew;
        private boolean minor;
        private int oldlen;
        private int newlen;
        private WikiSite wiki;

        public int getNs() {
            return ns;
        }

        @NonNull public String getTitle() {
            return StringUtils.defaultString(title);
        }

        @NonNull public String getLogType() {
            return StringUtils.defaultString(logtype);
        }

        @NonNull public Date getDate() {
            return DateUtil.iso8601DateParse(StringUtils.defaultString(timestamp));
        }

        @NonNull public String getParsedComment() {
            return StringUtils.defaultString(parsedcomment);
        }

        public void setWiki(WikiSite wiki) {
            this.wiki = wiki;
        }

        public WikiSite getWiki() {
            return wiki;
        }

        @NonNull public String getUser() {
            return StringUtils.defaultString(user);
        }

        public int getOldlen() {
            return oldlen;
        }

        public int getNewlen() {
            return newlen;
        }

        public boolean isAnon() {
            return anon;
        }

        public long getRevid() {
            return revid;
        }
    }
}
