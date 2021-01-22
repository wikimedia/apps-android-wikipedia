package org.wikipedia.savedpages;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;

class PageComponentsUrlParser {

    @NonNull
    public List<String> parse(@NonNull String html, @NonNull WikiSite site) {
        List<String> urls = new ArrayList<>();

        try {
            Document document = Jsoup.parse(html);
            // parsing css styles
            Elements css = document.select("link[rel=stylesheet]");
            for (Element element : css) {
                String url = element.attr("href");
                if (!TextUtils.isEmpty(url)) {
                    urls.add(UriUtil.resolveProtocolRelativeUrl(site, url));
                }
            }

            // parsing javascript files
            Elements javascript = document.select("script");
            for (Element element : javascript) {
                String url = element.attr("src");
                if (!TextUtils.isEmpty(url)) {
                    urls.add(UriUtil.resolveProtocolRelativeUrl(site, url));
                }
            }
        } catch (Exception e) {
            L.d("Parsing exception" + e);
        }

        return urls;
    }

    PageComponentsUrlParser() {
    }
}
