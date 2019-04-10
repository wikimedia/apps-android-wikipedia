package org.wikipedia;

import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.login.LoginResult;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ReleaseUtil;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WikipediaAppAdapter extends AppAdapter {

    @Override
    public String getMediaWikiBaseUrl() {
        return Prefs.getMediaWikiBaseUrl();
    }

    @Override
    public String getRestbaseUriFormat() {
        return Prefs.getRestbaseUriFormat();
    }

    @Override
    public OkHttpClient getOkHttpClient(@NonNull WikiSite wikiSite) {
        return OkHttpConnectionFactory.getClient().newBuilder()
                .addInterceptor(new LanguageVariantHeaderInterceptor(wikiSite)).build();
    }

    @Override
    public int getDesiredLeadImageDp() {
        return DimenUtil.calculateLeadImageWidth();
    }

    @Override
    public boolean isLoggedIn() {
        return AccountUtil.isLoggedIn();
    }

    @Override
    public String getUserName() {
        return AccountUtil.getUserName();
    }

    @Override
    public String getPassword() {
        return AccountUtil.getPassword();
    }

    @Override
    public void updateAccount(@NonNull LoginResult result) {
        AccountUtil.updateAccount(null, result);
    }

    @Override
    public SharedPreferenceCookieManager getCookies() {
        return Prefs.getCookies();
    }

    @Override
    public void setCookies(@NonNull SharedPreferenceCookieManager cookies) {
        Prefs.setCookies(cookies);
    }

    @Override
    public boolean logErrorsInsteadOfCrashing() {
        return ReleaseUtil.isProdRelease();
    }

    private static class LanguageVariantHeaderInterceptor implements Interceptor {
        @NonNull private final WikiSite wiki;

        LanguageVariantHeaderInterceptor(@NonNull WikiSite wiki) {
            this.wiki = wiki;
        }

        @Override
        @NonNull
        public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
            Request request = chain.request();
            request = request.newBuilder()
                    .header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(wiki))
                    .build();
            return chain.proceed(request);
        }
    }
}
