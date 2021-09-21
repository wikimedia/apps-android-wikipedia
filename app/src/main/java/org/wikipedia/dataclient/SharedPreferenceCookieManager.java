package org.wikipedia.dataclient;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public final class SharedPreferenceCookieManager implements CookieJar {
    private static final String CENTRALAUTH_PREFIX = "centralauth_";
    private static SharedPreferenceCookieManager INSTANCE;

    // Map: domain -> list of cookies
    private final Map<String, List<Cookie>> cookieJar;

    @NonNull
    public static SharedPreferenceCookieManager getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = Prefs.INSTANCE.getCookies();
            } catch (Exception e) {
                L.logRemoteErrorIfProd(e);
            }
        }
        if (INSTANCE == null) {
            INSTANCE = new SharedPreferenceCookieManager();
        }
        return INSTANCE;
    }

    public SharedPreferenceCookieManager(Map<String, List<Cookie>> cookieJar) {
        this.cookieJar = cookieJar;
    }

    private SharedPreferenceCookieManager() {
        cookieJar = new HashMap<>();
    }

    public Map<String, List<Cookie>> getCookieJar() {
        return cookieJar;
    }

    private void persistCookies() {
        Prefs.INSTANCE.setCookies(this);
    }

    public synchronized void clearAllCookies() {
        cookieJar.clear();
        persistCookies();
    }

    @Nullable public synchronized String getCookieByName(@NonNull String name) {
        for (String domainSpec: cookieJar.keySet()) {
            for (Cookie cookie : cookieJar.get(domainSpec)) {
                if (cookie.name().equals(name)) {
                    return cookie.value();
                }
            }
        }
        return null;
    }

    @Override
    public synchronized void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        if (cookies.isEmpty()) {
            return;
        }
        boolean cookieJarModified = false;
        for (Cookie cookie : cookies) {
            // Default to the URI's domain if cookie's domain is not explicitly set
            String domainSpec = TextUtils.isEmpty(cookie.domain()) ? url.uri().getAuthority() : cookie.domain();
            if (!cookieJar.containsKey(domainSpec)) {
                cookieJar.put(domainSpec, new ArrayList<>());
            }

            List<Cookie> cookieList = cookieJar.get(domainSpec);
            if (cookie.expiresAt() < System.currentTimeMillis() || "deleted".equals(cookie.value())) {
                Iterator<Cookie> i = cookieList.iterator();
                while (i.hasNext()) {
                    if (i.next().name().equals(cookie.name())) {
                        i.remove();
                        cookieJarModified = true;
                    }
                }
            } else {
                Iterator<Cookie> i = cookieList.iterator();
                boolean exists = false;
                while (i.hasNext()) {
                    Cookie c = i.next();
                    if (c.equals(cookie)) {
                        // an identical cookie already exists, so we don't need to update it.
                        exists = true;
                        break;
                    } else if (c.name().equals(cookie.name())) {
                        // it's a cookie with the same name, but different contents, so remove the
                        // current cookie, so that the new one will be added.
                        i.remove();
                    }
                }
                if (!exists) {
                    cookieList.add(cookie);
                    cookieJarModified = true;
                }
            }
        }
        if (cookieJarModified) {
            persistCookies();
        }
    }

    @Override
    public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> cookieList = new ArrayList<>();
        String domain = url.uri().getAuthority();

        for (String domainSpec : cookieJar.keySet()) {
            List<Cookie> cookiesForDomainSpec = cookieJar.get(domainSpec);

            if (domain.endsWith(domainSpec)) {
                buildCookieList(cookieList, cookiesForDomainSpec, null);
            } else if (domainSpec.endsWith("wikipedia.org")) {
                // For sites outside the wikipedia.org domain, transfer the centralauth cookies
                // from wikipedia.org unconditionally.
                buildCookieList(cookieList, cookiesForDomainSpec, CENTRALAUTH_PREFIX);
            }
        }
        return cookieList;
    }

    private void buildCookieList(@NonNull List<Cookie> outList, @NonNull List<Cookie> inList, @Nullable String prefix) {
        Iterator<Cookie> i = inList.iterator();
        boolean cookieJarModified = false;
        while (i.hasNext()) {
            Cookie cookie = i.next();
            if (prefix != null && !cookie.name().startsWith(prefix)) {
                continue;
            }
            // But wait, is the cookie expired?
            if (cookie.expiresAt() < System.currentTimeMillis()) {
                i.remove();
                cookieJarModified = true;
            } else {
                outList.add(cookie);
            }
        }
        if (cookieJarModified) {
            persistCookies();
        }
    }
}
