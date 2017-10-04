package org.wikipedia.json;

import android.net.Uri;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.wikipedia.dataclient.WikiSite;

import java.io.IOException;

public class WikiSiteTypeAdapter extends TypeAdapter<WikiSite> {
    private static final String DOMAIN = "domain";
    private static final String LANGUAGE_CODE = "languageCode";

    @Override public void write(JsonWriter out, WikiSite value) throws IOException {
        out.beginObject();
        out.name(DOMAIN);
        out.value(value.url());

        out.name(LANGUAGE_CODE);
        out.value(value.languageCode());
        out.endObject();
    }

    @Override public WikiSite read(JsonReader in) throws IOException {
        // todo: legacy; remove in June 2018
        if (in.peek() == JsonToken.STRING) {
            return new WikiSite(Uri.parse(in.nextString()));
        }

        String domain = null;
        String languageCode = null;
        in.beginObject();
        while (in.hasNext()) {
            String field = in.nextName();
            String val = in.nextString();
            switch (field) {
                case DOMAIN:
                    domain = val;
                    break;
                case LANGUAGE_CODE:
                    languageCode = val;
                    break;
                default: break;
            }
        }
        in.endObject();

        if (domain == null) {
            throw new JsonParseException("Missing domain");
        }

        // todo: legacy; remove in June 2018
        if (languageCode == null) {
            return new WikiSite(domain);
        }
        return new WikiSite(domain, languageCode);
    }
}
