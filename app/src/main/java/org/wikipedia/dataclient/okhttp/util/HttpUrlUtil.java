package org.wikipedia.dataclient.okhttp.util;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.HttpUrl;

public final class HttpUrlUtil {
    private static final List<String> RESTBASE_SEGMENT_IDENTIFIERS = Arrays.asList("rest_v1", "v1");

    public static boolean isRestBase(@NonNull HttpUrl url) {
        return !Collections.disjoint(url.encodedPathSegments(), RESTBASE_SEGMENT_IDENTIFIERS);
    }

    public static boolean isMobileView(@NonNull HttpUrl url) {
        return "mobileview".equals(url.queryParameter("action"));
    }

    private HttpUrlUtil() { }
}
