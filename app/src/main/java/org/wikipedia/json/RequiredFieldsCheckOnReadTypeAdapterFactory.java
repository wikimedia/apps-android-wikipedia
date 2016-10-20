package org.wikipedia.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.wikipedia.json.annotations.Required;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class RequiredFieldsCheckOnReadTypeAdapterFactory implements TypeAdapterFactory {
    @Nullable
    @Override
    public final <T> TypeAdapter<T> create(@NonNull Gson gson, @NonNull TypeToken<T> typeToken) {
        Class<?> rawType = typeToken.getRawType();
        Set<Field> requiredFields = collectRequiredFields(rawType);

        if (requiredFields.isEmpty()) {
            return null;
        }

        setFieldsAccessible(requiredFields, true);
        return new Adapter<>(gson.getDelegateAdapter(this, typeToken), requiredFields);
    }

    @NonNull
    private Set<Field> collectRequiredFields(@NonNull Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Set<Field> required = new HashSet<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Required.class)) {
                required.add(field);
            }
        }
        return Collections.unmodifiableSet(required);
    }

    private void setFieldsAccessible(Iterable<Field> fields, boolean accessible) {
        for (Field field : fields) {
            field.setAccessible(accessible);
        }
    }

    private static final class Adapter<T> extends TypeAdapter<T> {
        @NonNull private final TypeAdapter<T> delegate;
        @NonNull private final Set<Field> requiredFields;

        private Adapter(@NonNull TypeAdapter<T> delegate, @NonNull final Set<Field> requiredFields) {
            this.delegate = delegate;
            this.requiredFields = requiredFields;
        }

        @Override public void write(JsonWriter out, T value) throws IOException {
            delegate.write(out, value);
        }

        @Override @Nullable public T read(JsonReader in) throws IOException {
            T deserialized = delegate.read(in);
            return allRequiredFieldsPresent(deserialized, requiredFields) ? deserialized : null;
        }

        private boolean allRequiredFieldsPresent(@NonNull T deserialized,
                                                 @NonNull Set<Field> required) {
            for (Field field : required) {
                try {
                    if (field.get(deserialized) == null) {
                        return false;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new JsonParseException(e);
                }
            }
            return true;
        }
    }
}
