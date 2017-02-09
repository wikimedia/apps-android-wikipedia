package org.wikipedia.test.theories;

import org.junit.experimental.theories.ParametersSuppliedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;

@ParametersSuppliedBy(TestedOnBoolSupplier.class) @Retention(RetentionPolicy.RUNTIME) @Target(PARAMETER)
public @interface TestedOnBool { }
