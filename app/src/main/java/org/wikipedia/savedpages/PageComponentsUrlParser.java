package org.wikipedia.savedpages;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikipedia.html.ParseException;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;

class PageComponentsUrlParser {

    @VisibleForTesting
    @NonNull
    public List<String> parse(@NonNull String html) {
        List<String> urls = new ArrayList<>();

        try {
            Document document = Jsoup.parse(html);
            // parsing css styles
            Elements css = document.select("link[rel=stylesheet]");
            for (Element element : css) {
                String url = element.attr("href");
                urls.add(url);
            }

            // parsing javascript files
            Elements javascript = document.select("script");
            for (Element element : javascript) {
                String url = element.attr("src");
                urls.add(url);
            }
        } catch (ParseException e) {
            L.d("Parsing exception" + e);
        }

        return urls;
    }

    PageComponentsUrlParser() {
    }
}
