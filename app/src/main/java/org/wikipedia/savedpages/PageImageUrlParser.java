package org.wikipedia.savedpages;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.html.ImageElement;
import org.wikipedia.html.ImageTagParser;
import org.wikipedia.html.ParseException;
import org.wikipedia.html.PixelDensityDescriptorParser;
import org.wikipedia.page.Section;
import org.wikipedia.util.DimenUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PageImageUrlParser {
    @NonNull private final ImageTagParser imageParser;
    @NonNull private final PixelDensityDescriptorParser descriptorParser;

    public PageImageUrlParser(@NonNull ImageTagParser imageParser,
                              @NonNull PixelDensityDescriptorParser descriptorParser) {
        this.imageParser = imageParser;
        this.descriptorParser = descriptorParser;
    }

    @NonNull public List<String> parse(@NonNull PageLead lead) {
        return parse(lead, DimenUtil.calculateLeadImageWidth());
    }

    @NonNull public List<String> parse(@NonNull PageRemaining sections) {
        return parse(sections.sections());
    }

    @NonNull public List<String> parse(@NonNull List<Section> sections) {
        return parse(toHtml(sections));
    }

    @NonNull public List<String> parse(@NonNull String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        try {
            return imageElementsToUrls(querySelectorAllImage(doc));
        } catch (ParseException ignore) { }
        return Collections.emptyList();
    }

    @VisibleForTesting @NonNull List<String> parse(@NonNull PageLead lead, int leadImageWidth) {
        List<String> urls = new ArrayList<>();

        if (lead.getTitlePronunciationUrl() != null) {
            urls.add(lead.getTitlePronunciationUrl());
        }

        String leadImageUrl = lead.getLeadImageUrl(leadImageWidth);
        String thumbUrl = lead.getThumbUrl();
        if (leadImageUrl != null) {
            urls.add(leadImageUrl);
        }
        if (thumbUrl != null) {
            urls.add(thumbUrl);
        }

        String html = toHtml(lead);
        try {
            urls.addAll(parse(html));
        } catch (ParseException ignore) { }

        return urls;
    }

    @NonNull private String toHtml(@NonNull PageLead lead) {
        return lead.getLeadSectionContent();
    }

    @NonNull private String toHtml(@NonNull List<Section> sections) {
        StringBuilder html = new StringBuilder();
        for (Section section : sections) {
            html.append(section.getContent());
        }
        return html.toString();
    }

    @NonNull private List<ImageElement> querySelectorAllImage(@NonNull Document doc) {
        List<ImageElement> imgs = new ArrayList<>();

        Elements elements = doc.getElementsByTag(imageParser.tagName());
        for (Element el : elements) {
            try {
                imgs.add(imageParser.parse(descriptorParser, el));
            } catch (ParseException ignore) { }
        }

        return Collections.unmodifiableList(imgs);
    }

    @NonNull private List<String> imageElementsToUrls(@NonNull List<ImageElement> imgs) {
        List<String> urls = new ArrayList<>(imgs.size());
        for (ImageElement img : imgs) {
            urls.addAll(imageElementToUrls(img));
        }
        return Collections.unmodifiableList(urls);
    }

    @NonNull private Collection<String> imageElementToUrls(@NonNull ImageElement img) {
        return img.srcs().values();
    }
}
