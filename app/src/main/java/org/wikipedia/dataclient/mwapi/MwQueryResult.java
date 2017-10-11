package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.gallery.VideoInfo;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.model.BaseModel;
import org.wikipedia.nearby.NearbyPage;
import org.wikipedia.notifications.Notification;
import org.wikipedia.page.PageTitle;
import org.wikipedia.useroption.dataclient.UserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MwQueryResult extends BaseModel implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings("unused") @Nullable private List<MwQueryPage> pages;
    @SuppressWarnings("unused") @Nullable private List<Redirect> redirects;
    @SuppressWarnings("unused") @Nullable private List<ConvertedTitle> converted;
    @SuppressWarnings("unused") @SerializedName("userinfo") private UserInfo userInfo;
    @SuppressWarnings("unused") @Nullable private List<ListUsersResponse> users;
    @SuppressWarnings("unused") @Nullable private Tokens tokens;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("authmanagerinfo")
    @Nullable private MwAuthManagerInfo amInfo;
    @SuppressWarnings("unused") @Nullable private MarkReadResponse echomarkread;
    @SuppressWarnings("unused,NullableProblems")
    @Nullable private NotificationList notifications;

    @Nullable public List<MwQueryPage> pages() {
        return pages;
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

    @Nullable public List<Notification> notifications() {
        return notifications != null ? notifications.list() : null;
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

    @NonNull public Set<String> getGroupsFor(@NonNull String userName) {
        if (users != null) {
            for (ListUsersResponse user : users) {
                final Set<String> groups = user.getGroupsFor(userName);
                if (groups != null) {
                    return groups;
                }
            }
        }
        return Collections.emptySet();
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

    @Nullable public String wikitext() {
        if (pages != null) {
            for (MwQueryPage page : pages) {
                if (page.revisions() != null && page.revisions().get(0) != null) {
                    return page.revisions().get(0).content();
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

    @NonNull public List<NearbyPage> nearbyPages() {
        List<NearbyPage> result = new ArrayList<>();
        if (pages != null) {
            for (MwQueryPage page : pages) {
                NearbyPage nearbyPage = new NearbyPage(page);
                if (nearbyPage.getLocation() != null) {
                    result.add(nearbyPage);
                }
            }
        }
        return result;
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

    private static class ConvertedTitle {
        @SuppressWarnings("unused") @Nullable private String from;
        @SuppressWarnings("unused") @Nullable private String to;

        @Nullable public String to() {
            return to;
        }

        @Nullable public String from() {
            return from;
        }
    }

    private static class ListUsersResponse {
        @SuppressWarnings("unused") @SerializedName("name") @Nullable private String name;
        @SuppressWarnings("unused") @Nullable private List<String> groups;

        @Nullable Set<String> getGroupsFor(@NonNull String userName) {
            if (userName.equals(name) && groups != null) {
                Set<String> groupNames = new ArraySet<>();
                groupNames.addAll(groups);
                return Collections.unmodifiableSet(groupNames);
            }
            return null;
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

    private static class MarkReadResponse {
        @SuppressWarnings("unused") @Nullable private String result;

        @Nullable public String result() {
            return result;
        }
    }

    private static class NotificationList {
        @SuppressWarnings("unused") @Nullable private List<Notification> list;

        @Nullable private List<Notification> list() {
            return list;
        }
    }
}
