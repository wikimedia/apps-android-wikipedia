package org.wikipedia.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
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
        int ns = in.nextInt();
        return Namespace.of(ns);
    }
}