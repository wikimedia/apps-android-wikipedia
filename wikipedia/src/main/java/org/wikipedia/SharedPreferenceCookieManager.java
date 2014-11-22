package org.wikipedia;

import android.content.SharedPreferences;
import android.text.TextUtils;
import org.wikipedia.settings.PrefKeys;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SharedPreferenceCookieManager extends CookieManager {

    private final HashMap<String, HashMap<String, String>> cookieJar = new HashMap<String, HashMap<String, String>>();
    private final SharedPreferences prefs;

    public SharedPreferenceCookieManager(SharedPreferences prefs) {
        this.prefs = prefs;
        List<String> domains = makeList(prefs.getString(PrefKeys.getCookieDomainsKey(), ""));
        for (String domain: domains) {
            String key = String.format(PrefKeys.getCookiesForDomain(), domain);
            String cookies = prefs.getString(key, "");
            cookieJar.put(domain, makeCookieMap(makeList(cookies)));
        }
    }

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
        if (uri == null || requestHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        Map<String, List<String>> cookieMap = new HashMap<String, List<String>>();
        ArrayList<String> cookiesList = new ArrayList<String>();

        String domain = uri.getAuthority();

        for (String domainSpec: cookieJar.keySet()) {
            // Very weak domain matching.
            // Primarily to make sure that cookies set for .wikipedia.org are sent for en.wikipedia.org
            // FIXME: Whitelist the domains we accept cookies from/send cookies to. SECURITY!!!1
            if (domain.endsWith(domainSpec)) {
                cookiesList.addAll(makeCookieList(cookieJar.get(domainSpec)));
            }
        }

        cookieMap.put("Cookie", cookiesList);

        return Collections.unmodifiableMap(cookieMap);
    }

    @Override
    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        // pre-condition check
        if (uri == null || responseHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        HashSet<String> domainsModified = new HashSet<String>();

        for (String headerKey : responseHeaders.keySet()) {
            if (headerKey == null || !headerKey.equalsIgnoreCase("Set-Cookie")) {
                continue;
            }

            for (String headerValue : responseHeaders.get(headerKey)) {
                try {
                    List<HttpCookie> cookies = HttpCookie.parse(headerValue);
                    for (HttpCookie cookie : cookies) {
                        // Default to the URI's domain if domain is not explicitly set
                        String domainSpec = cookie.getDomain() == null ? uri.getAuthority() : cookie.getDomain();
                        if (!cookieJar.containsKey(domainSpec)) {
                            cookieJar.put(domainSpec, new HashMap<String, String>());
                        }
                        cookieJar.get(domainSpec).put(cookie.getName(), cookie.getValue());
                        domainsModified.add(domainSpec);
                    }
                } catch (IllegalArgumentException e) {
                    // invalid set-cookie header string
                    // no-op
                }
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PrefKeys.getCookieDomainsKey(), makeString(cookieJar.keySet()));

        for (String domain : domainsModified) {
            String prefKey = String.format(PrefKeys.getCookiesForDomain(), domain);
            editor.putString(prefKey, makeString(makeCookieList(cookieJar.get(domain))));

        }
        editor.apply();
    }

    @Override
    public CookieStore getCookieStore() {
        // We don't actually have one. hehe
        throw new UnsupportedOperationException("We poor. We no have CookieStore");
    }

    public void clearAllCookies() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String domain: cookieJar.keySet()) {
            String key = String.format(PrefKeys.getCookiesForDomain(), domain);
            editor.remove(key);
        }
        editor.remove(PrefKeys.getCookieDomainsKey());
        editor.apply();
        cookieJar.clear();
    }

    private HashMap<String, String> makeCookieMap(List<String> cookies) {
        HashMap<String, String> cookiesMap = new HashMap<String, String>();
        for (String cookie : cookies) {
            if (!cookie.contains("=")) {
                throw new RuntimeException("Cookie " + cookie + " is invalid!");
            }
            String[] parts = cookie.split("=");
            cookiesMap.put(parts[0], parts[1]);
        }
        return cookiesMap;
    }

    private List<String> makeCookieList(Map<String, String> cookies) {
        ArrayList<String> cookiesList = new ArrayList<String>();
        for (Map.Entry<String, String> entry: cookies.entrySet()) {
            cookiesList.add(entry.getKey() + "=" + entry.getValue());
        }
        return cookiesList;
    }

    private String makeString(Iterable<String> list) {
        return TextUtils.join(";", list);
    }

    private List<String> makeList(String str) {
        return Arrays.asList(TextUtils.split(str, ";"));
    }
}
