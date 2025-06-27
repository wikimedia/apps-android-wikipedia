package org.wikimedia.metrics_platform.config.curation;

import java.util.Collection;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNullableByDefault;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Value;

@Builder @Value
@ParametersAreNullableByDefault
public class ComparableCurationRules<T extends Comparable<T>> implements Predicate<T> {
    @SerializedName("is_equals") T isEquals;
    @SerializedName("not_equals") T isNotEquals;
    @SerializedName("greater_than") T greaterThan;
    @SerializedName("less_than") T lessThan;
    @SerializedName("greater_than_or_equals") T greaterThanOrEquals;
    @SerializedName("less_than_or_equals") T lessThanOrEquals;
    Collection<T> in;
    @SerializedName("not_in") Collection<T> notIn;

    @SuppressWarnings("CyclomaticComplexity") // Code is readable enough here
    @Override
    public boolean test(T value) {
        if (value == null) return false;

        return (isEquals == null || isEquals.equals(value))
                && (isNotEquals == null || !isNotEquals.equals(value))
                && (greaterThan == null || greaterThan.compareTo(value) < 0)
                && (lessThan == null || lessThan.compareTo(value) > 0)
                && (greaterThanOrEquals == null || greaterThanOrEquals.compareTo(value) <= 0)
                && (lessThanOrEquals == null || lessThanOrEquals.compareTo(value) >= 0)
                && (notIn == null || !notIn.contains(value));
    }
}
