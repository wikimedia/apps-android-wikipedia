package org.wikipedia.page;

import android.location.Location;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.wikipedia.util.log.L;

import java.io.IOException;

public class GeoTypeAdapter extends TypeAdapter<Location> {
    @Override
    public void write(JsonWriter out, Location value) throws IOException {
        out.beginObject();
        out.name(GeoUnmarshaller.LATITUDE).value(value.getLatitude());
        out.name(GeoUnmarshaller.LONGITUDE).value(value.getLongitude());
        out.endObject();
    }

    @Override
    public Location read(JsonReader in) throws IOException {
        Location ret = new Location((String) null);

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch(name) {
                case GeoUnmarshaller.LATITUDE:
                    ret.setLatitude(in.nextDouble());
                    break;
                case GeoUnmarshaller.LONGITUDE:
                    ret.setLongitude(in.nextDouble());
                    break;
                default:
                    L.d("name=" + name);
                    break;
            }
        }
        in.endObject();

        return ret;
    }
}
