package org.wikipedia.html;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImageElement {
    private static final PixelDensityDescriptor DESCRIPTOR_DEFAULT = null;

    @NonNull private final Map<PixelDensityDescriptor, String> srcs;
    // todo: add support for width descriptors which require parsing sizes

    public ImageElement(@Nullable String src, @NonNull Map<PixelDensityDescriptor, String> srcSet) {
        @SuppressWarnings("checkstyle:hiddenfield") Map<PixelDensityDescriptor, String> srcs = new HashMap<>(srcSet);
        if (!StringUtils.isBlank(src)) {
            srcs.put(DESCRIPTOR_DEFAULT, src);
        }
        this.srcs = Collections.unmodifiableMap(srcs);
    }

    @NonNull public Map<PixelDensityDescriptor, String> srcs() {
        return srcs;
    }

    @Nullable public String src() {
        return src(null);
    }

    @Nullable public String src(@Nullable PixelDensityDescriptor descriptor) {
        return srcs.get(descriptor);
    }
}
