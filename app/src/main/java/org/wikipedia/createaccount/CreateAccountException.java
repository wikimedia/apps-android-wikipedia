package org.wikipedia.createaccount;

import androidx.annotation.NonNull;

/**
 * Exception thrown when an account creation request FAILs
 */
public class CreateAccountException extends RuntimeException {
    CreateAccountException(@NonNull String message) {
        super(message);
    }
}
