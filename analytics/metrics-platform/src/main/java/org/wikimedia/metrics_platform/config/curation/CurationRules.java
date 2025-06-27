package org.wikimedia.metrics_platform.config.curation;

import java.util.Collection;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNullableByDefault;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Value;

@Builder @Value
@ParametersAreNullableByDefault
public class CurationRules<T> implements Predicate<T> {
    @SerializedName("equals") T isEquals;
    @SerializedName("not_equals") T isNotEquals;
    Collection<T> in;
    @SerializedName("not_in") Collection<T> notIn;

    @Override
    public boolean test(T value) {
        if (value == null) return false;

        return (isEquals == null || isEquals.equals(value))
                && (isNotEquals == null || !isNotEquals.equals(value))
                && (in == null || in.contains(value))
                && (notIn == null || !notIn.contains(value));
    }
}
