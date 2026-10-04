package com.example.studentskillsplacementtracker.util;

import android.content.Context;

import com.example.studentskillsplacementtracker.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Maps Firebase Authentication failures to friendly, user facing messages.
 *
 * Shared by MainActivity (login) and RegisterActivity (registration) so both
 * screens report errors the same way. Raw Firebase exception messages are never
 * shown to the user.
 */
public final class AuthErrorMapper {

    private AuthErrorMapper() {
        // Utility class - no instances.
    }

    /**
     * Returns a friendly message for a Firebase Authentication failure.
     */
    public static String getMessage(Context context, Exception exception) {

        if (exception == null) {
            return context.getString(R.string.err_auth_generic);
        }

        // Weak password (registration)
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return context.getString(R.string.err_weak_password);
        }

        // Email already registered
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return context.getString(R.string.err_email_in_use);
        }

        // The errorCode must be read before the instanceof checks for the
        // exception sub-classes, because several Firebase exceptions share a
        // parent class.
        String errorCode = (exception instanceof FirebaseAuthException)
                ? ((FirebaseAuthException) exception).getErrorCode()
                : null;

        // Disabled account. Checked before the broader "invalid user" case below
        // because ERROR_USER_DISABLED is also a FirebaseAuthInvalidUserException.
        if ("ERROR_USER_DISABLED".equals(errorCode)) {
            return context.getString(R.string.err_user_disabled);
        }

        // Malformed email address
        if ("ERROR_INVALID_EMAIL".equals(errorCode)) {
            return context.getString(R.string.err_email_invalid);
        }

        // Wrong password
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return context.getString(R.string.err_invalid_credentials);
        }

        // Unknown / deleted account. Reported with exactly the same message as a
        // bad password so the login screen cannot be used to discover which email
        // addresses have an account.
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return context.getString(R.string.err_invalid_credentials);
        }

        // Connectivity problems
        if (exception instanceof FirebaseNetworkException
                || "ERROR_NETWORK_REQUEST_FAILED".equals(errorCode)) {
            return context.getString(R.string.err_network);
        }

        if ("ERROR_OPERATION_NOT_ALLOWED".equals(errorCode)) {
            return context.getString(R.string.err_operation_not_allowed);
        }

        if ("ERROR_TOO_MANY_REQUESTS".equals(errorCode)) {
            return context.getString(R.string.err_too_many_requests);
        }

        // Reasonable generic authentication failure
        return context.getString(R.string.err_auth_generic);
    }
}
