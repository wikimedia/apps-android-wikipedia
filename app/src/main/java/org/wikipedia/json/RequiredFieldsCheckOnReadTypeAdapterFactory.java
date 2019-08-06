package org.wikipedia.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

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
import java.util.Set;

/**
 * TypeAdapterFactory that provides TypeAdapters that return null values for objects that are
 * missing fields annotated with @Required.
 *
 * BEWARE: This means that a List or other Collection of objects that have @Required fields can
 * contain null elements after deserialization!
 *
 * TODO: Handle null values in lists during deserialization, perhaps with a new @RequiredElements
 * annotation and another corresponding TypeAdapter(Factory).
 */
class RequiredFieldsCheckOnReadTypeAdapterFactory implements TypeAdapterFactory {
    @Nullable @Override public final <T> TypeAdapter<T> create(@NonNull Gson gson, @NonNull TypeToken<T> typeToken) {
        Class<?> rawType = typeToken.getRawType();
        Set<Field> requiredFields = collectRequiredFields(rawType);

        if (requiredFields.isEmpty()) {
            return null;
        }

        setFieldsAccessible(requiredFields, true);
        return new Adapter<>(gson.getDelegateAdapter(this, typeToken), requiredFields);
    }

    @NonNull private Set<Field> collectRequiredFields(@NonNull Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Set<Field> required = new ArraySet<>();
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
