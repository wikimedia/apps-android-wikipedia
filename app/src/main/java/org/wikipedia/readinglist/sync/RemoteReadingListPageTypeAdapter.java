package org.wikipedia.readinglist.sync;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.wikipedia.util.UriUtil;

import java.io.IOException;

import static org.wikipedia.readinglist.sync.RemoteReadingLists.RemoteReadingListPage;

public class RemoteReadingListPageTypeAdapter extends TypeAdapter<RemoteReadingListPage> {
    private static final String DELIMIT_CHAR = ":";

    @Override public void write(JsonWriter out, RemoteReadingListPage page) throws IOException {
        out.value(page.lang() + DELIMIT_CHAR
                + page.namespace() + DELIMIT_CHAR
                + UriUtil.encodeURL(page.title()));
    }

    @Override public RemoteReadingListPage read(JsonReader in) throws IOException {
        String[] pageData = in.nextString().split(DELIMIT_CHAR);
        return new RemoteReadingListPage(pageData[0], Integer.parseInt(pageData[1]),
                UriUtil.decodeURL(pageData[2]));
    }
}
