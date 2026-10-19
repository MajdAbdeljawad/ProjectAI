package com.fitcoach.app.backend.service;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RecoveryOutputGuard {

    private static final Pattern DISALLOWED = Pattern.compile(
            "you have pneumonia|diagnosed with|diagnosed pneumonia|diagnose pneumonia|invented (resting )?heart rate|invent resting heart rate",
            Pattern.CASE_INSENSITIVE
    );

    private RecoveryOutputGuard() {
    }

    public static void assertNoDisallowedClaims(Object... parts) {
        StringBuilder blob = new StringBuilder();
        for (Object part : parts) {
            blob.append(part);
        }
        if (DISALLOWED.matcher(blob.toString().toLowerCase(Locale.ROOT)).find()) {
            throw new RecoveryProviderException(
                    "The model output claimed a diagnosis or invented a measurement, so it was rejected.",
                    "MALFORMED_RESPONSE"
            );
        }
    }
}
