package org.wikipedia.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import org.wikipedia.json.annotations.Required;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
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
class RequiredFieldsCheckOnReadJsonAdapterFactory implements JsonAdapter.Factory {
    @Nullable
    @Override
    public JsonAdapter<?> create(@NonNull Type type, @NonNull Set<? extends Annotation> annotations,
                                 @NonNull Moshi moshi) {
        Class<?> rawType = Types.getRawType(type);
        Set<Field> requiredFields = collectRequiredFields(rawType);
        if (requiredFields.isEmpty()) {
            return null;
        }
        for (Field field : requiredFields) {
            field.setAccessible(true);
        }
        final JsonAdapter<?> adapter = moshi.nextAdapter(this, rawType, annotations);
        return new Adapter<>(adapter, requiredFields);
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

    private static final class Adapter<T> extends JsonAdapter<T> {
        @NonNull private final JsonAdapter<T> delegate;
        @NonNull private final Set<Field> requiredFields;

        private Adapter(@NonNull JsonAdapter<T> delegate, @NonNull final Set<Field> requiredFields) {
            this.delegate = delegate;
            this.requiredFields = requiredFields;
        }

        @Override
        public void toJson(@NonNull JsonWriter writer, @Nullable T value) throws IOException {
            delegate.toJson(writer, value);
        }

        @Nullable
        @Override
        public T fromJson(@NonNull JsonReader reader) throws IOException {
            final T deserialized = delegate.fromJson(reader);
            if (deserialized != null) {
                return allRequiredFieldsPresent(deserialized, requiredFields) ? deserialized : null;
            } else {
                return null;
            }
        }

        private boolean allRequiredFieldsPresent(@NonNull T deserialized, @NonNull Set<Field> required) throws IOException {
            for (Field field : required) {
                try {
                    if (field.get(deserialized) == null) {
                        return false;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IOException(e);
                }
            }
            return true;
        }
    }
}
