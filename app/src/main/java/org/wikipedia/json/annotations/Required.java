package org.wikipedia.json.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Annotate fields in Retrofit POJO classes with this to enforce their presence in order to return
 * an instantiated object.
 *
 * E.g.: @NonNull @Required private String title;
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface Required {
}
