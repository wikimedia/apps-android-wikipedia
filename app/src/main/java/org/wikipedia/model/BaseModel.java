package org.wikipedia.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class BaseModel {
    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
}
