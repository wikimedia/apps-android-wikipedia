package org.wikipedia.createaccount;

import com.mobsandgeeks.saripaar.annotation.ValidateUsing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ValidateUsing(OptionalEmailRule.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OptionalEmail {
    boolean allowLocal() default false;
    int sequence() default -1;
    int messageResId() default -1;
    String message() default "Invalid email";
}