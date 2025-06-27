package org.wikimedia.metrics_platform.config.curation;

import java.util.Collection;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNullableByDefault;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Value;

@Builder @Value
@ParametersAreNullableByDefault
public class CollectionCurationRules<T> implements Predicate<Collection<T>> {
    T contains;
    @SerializedName("does_not_contain") T doesNotContain;
    @SerializedName("contains_all") Collection<T> containsAll;
    @SerializedName("contains_any") Collection<T> containsAny;

    @Override
    public boolean test(Collection<T> value) {
        if (value == null) return false;

        return (contains == null || value.contains(contains))
                && (doesNotContain == null || !value.contains(doesNotContain))
                && (containsAll == null || value.containsAll(containsAll))
                && (containsAny == null || value.stream().filter(v -> containsAny.contains(v)).count() > 0);
    }
}
