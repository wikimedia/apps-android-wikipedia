package org.wikipedia.html;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Element;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImageTagParser {
    @NonNull private static final String TAG_NAME = "img";
    @NonNull private static final String ATTR_SRC = "src";
    @NonNull private static final String ATTR_SRC_SET = "srcset";
    @NonNull private static final String DEFAULT_DESCRIPTOR = "1x";

    @NonNull public String tagName() {
        return TAG_NAME;
    }

    @NonNull public ImageElement parse(@NonNull PixelDensityDescriptorParser descriptorParser,
                                       @NonNull Element el) {
        String src = el.attr(ATTR_SRC);
        String srcSet = el.attr(ATTR_SRC_SET);
        return new ImageElement(src, parseSrcSet(descriptorParser, srcSet));
    }

    @NonNull private Map<PixelDensityDescriptor, String> parseSrcSet(@NonNull PixelDensityDescriptorParser descriptorParser,
                                                                     @Nullable String srcSet) {
        if (StringUtils.isBlank(srcSet)) {
            return Collections.emptyMap();
        }

        Map<PixelDensityDescriptor, String> srcs = new HashMap<>();
        for (String src : srcSet.split(",")) {
            try {
                Pair<String, String> urlDescriptor = parseSrc(src.trim());
                PixelDensityDescriptor descriptor = descriptorParser.parse(urlDescriptor.getRight());
                srcs.put(descriptor, urlDescriptor.getLeft());
            } catch (ParseException ignore) { }
        }

        return Collections.unmodifiableMap(srcs);
    }

    @NonNull private Pair<String, String> parseSrc(@NonNull String src) {
        String[] urlDescriptor = src.split("\\s");
        if (urlDescriptor.length == 0 || StringUtils.isBlank(urlDescriptor[0])) {
            throw new ParseException("srcset source has no URL");
        }
        String url = urlDescriptor[0];
        String descriptor = urlDescriptor.length > 1 ? urlDescriptor[1] : DEFAULT_DESCRIPTOR;
        return new ImmutablePair<>(url, descriptor);
    }
}
