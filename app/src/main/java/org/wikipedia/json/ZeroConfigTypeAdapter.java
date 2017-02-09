package org.wikipedia.json;


import android.graphics.Color;
import android.net.Uri;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.wikipedia.zero.ZeroConfig;

import java.io.IOException;

class ZeroConfigTypeAdapter extends TypeAdapter<ZeroConfig> {
    @Override public void write(JsonWriter out, ZeroConfig value) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ZeroConfig read(JsonReader in) throws IOException, NumberFormatException {
        ZeroConfig result = new ZeroConfig();
        in.beginObject();
        while (in.hasNext()) {
            String field = in.nextName();
            String value = in.nextString();
            switch (field) {
                case "message":
                    result.setMessage(value);
                    break;
                case "background":
                    result.setBackground(Color.parseColor(value));
                    break;
                case "foreground":
                    result.setForeground(Color.parseColor(value));
                    break;
                case "exitTitle":
                    result.setExitTitle(value);
                    break;
                case "exitWarning":
                    result.setExitWarning(value);
                    break;
                case "partnerInfoText":
                    result.setPartnerInfoText(value);
                    break;
                case "partnerInfoUrl":
                    result.setPartnerInfoUrl(Uri.parse(value));
                    break;
                case "bannerUrl":
                    result.setBannerUrl(Uri.parse(value));
                    break;
                default:
                    break;
            }
        }
        in.endObject();
        return result;
    }
}
