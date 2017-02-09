package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

// ugly code. there are no seams into CacheControl and CacheControl(Builder) does not preserve all
// directives
final class CacheControlUtil {
    @NonNull static String forceNetRequest(@NonNull String cacheControl) {
        return replaceDirective(cacheControl, "no-cache", "no-cache");
    }

    @NonNull static String replaceMaxStale(@NonNull String cacheControl, int maxStale) {
        return replaceDirective(cacheControl, "max-stale(=[0-9]*)?", "max-stale=" + maxStale);
    }

    @NonNull static String replaceDirective(@NonNull String cacheControl,
                                            @NonNull String removeDirective,
                                            @NonNull String replaceDirective) {
        String ret = removeDirective(cacheControl, removeDirective);
        ret += (StringUtils.isBlank(ret) ? "" : ", ") + replaceDirective;
        return ret;
    }

    @NonNull static String removeDirective(@NonNull String cacheControl, @NonNull String directive) {
        return cacheControl.replaceAll(directive + ", |,? ?" + directive, "");
    }

    private CacheControlUtil() { }
}
