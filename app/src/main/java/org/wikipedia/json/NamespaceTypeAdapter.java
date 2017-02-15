package org.wikipedia.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.wikipedia.page.Namespace;

import java.io.IOException;

public class NamespaceTypeAdapter extends TypeAdapter<Namespace> {

    @Override
    public void write(JsonWriter out, Namespace namespace) throws IOException {
        out.value(namespace.code());
    }

    @Override
    public Namespace read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.STRING) {
            // Prior to 3210ce44, we marshaled Namespace as the name string of the enum, instead of
            // the code number. This introduces a backwards-compatible check for the string value.
            // TODO: remove after April 2017, when all older namespaces have been deserialized.
            return Namespace.valueOf(in.nextString());
        }
        return Namespace.of(in.nextInt());
    }
}
