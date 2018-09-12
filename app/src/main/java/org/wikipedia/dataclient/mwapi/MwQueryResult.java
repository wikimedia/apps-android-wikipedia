package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.gallery.VideoInfo;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.model.BaseModel;
import org.wikipedia.notifications.Notification;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.SiteInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MwQueryResult extends BaseModel implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings("unused") @Nullable private List<MwQueryPage> pages;
    @SuppressWarnings("unused") @Nullable private List<Redirect> redirects;
    @SuppressWarnings("unused") @Nullable private List<ConvertedTitle> converted;
    @SuppressWarnings("unused") @SerializedName("userinfo") private UserInfo userInfo;
    @SuppressWarnings("unused") @Nullable private List<ListUserResponse> users;
    @SuppressWarnings("unused") @Nullable private Tokens tokens;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("authmanagerinfo")
    @Nullable private MwAuthManagerInfo amInfo;
    @SuppressWarnings("unused") @Nullable private MarkReadResponse echomarkread;
    @SuppressWarnings("unused") @Nullable private MarkReadResponse echomarkseen;
    @SuppressWarnings("unused,NullableProblems") @Nullable private NotificationList notifications;
    @Nullable private Map<String, Notification.UnreadNotificationWikiItem> unreadnotificationpages;
    @SuppressWarnings("unused") @SerializedName("general")
    @Nullable private SiteInfo generalSiteInfo;

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

    @NonNull public Map<String, ImageInfo> images() {
        Map<String, ImageInfo> result = new HashMap<>();
        if (pages != null) {
            for (MwQueryPage page : pages) {
                if (page.imageInfo() != null) {
                    result.put(page.title(), page.imageInfo());
                }
            }
        }
        return result;
    }

    @NonNull public Map<String, VideoInfo> videos() {
        Map<String, VideoInfo> result = new HashMap<>();
        if (pages != null) {
            for (MwQueryPage page : pages) {
                if (page.videoInfo() != null) {
                    result.put(page.title(), page.videoInfo());
                }
            }
        }
        return result;
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

    @NonNull public List<NearbyPage> nearbyPages(@NonNull WikiSite wiki) {
        List<NearbyPage> result = new ArrayList<>();
        if (pages != null) {
            for (MwQueryPage page : pages) {
                NearbyPage nearbyPage = new NearbyPage(page, wiki);
                if (nearbyPage.getLocation() != null) {
                    result.add(nearbyPage);
                }
            }
        }
        return result;
    }

    @Nullable public SiteInfo siteInfo() {
        return generalSiteInfo;
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
        // noinspection ConstantConditions
        for (MwQueryResult.ConvertedTitle convertedTitle : converted) {
            // noinspection ConstantConditions
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

        @Nullable private String csrf() {
            return csrf;
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
}
