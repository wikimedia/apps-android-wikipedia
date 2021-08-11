package org.wikipedia.json;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class UriJsonAdapter extends JsonAdapter<Uri> {
    @Nullable
    @Override
    public Uri fromJson(JsonReader reader) throws IOException {
        return Uri.parse(reader.nextString());
    }

    @Override
    public void toJson(@NonNull JsonWriter writer, @Nullable Uri value) throws IOException {
        if (value == null) {
            writer.nullValue();
        } else {
            writer.value(value.toString());
        }
    }
}
