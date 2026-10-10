package com.example.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;

/** Logs useful AI failures while scrubbing credentials from provider messages. */
public final class SafeAiErrorLogger {
    private static final Pattern GOOGLE_API_KEY = Pattern.compile("AIza[0-9A-Za-z_-]{20,}");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(authorization\\s*[:=]\\s*bearer\\s+)[^\\s,;]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_PARAMETER = Pattern.compile("(?i)([?&](?:key|api_key)=)[^&\\s]+", Pattern.CASE_INSENSITIVE);

    private SafeAiErrorLogger() { }

    public static void logFailure(Logger logger, String operation, Throwable error) {
        List<String> causes = new ArrayList<>();
        Throwable current = error;
        for (int i = 0; current != null && i < 6; i++, current = current.getCause()) {
            String message = current.getMessage();
            causes.add(current.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + scrub(message)));
        }
        Throwable safeTrace = new Throwable(String.join(" <- ", causes));
        safeTrace.setStackTrace(error.getStackTrace());
        logger.error("{} failed (exception details redacted):", operation, safeTrace);
    }

    private static String scrub(String message) {
        return KEY_PARAMETER.matcher(BEARER_TOKEN.matcher(GOOGLE_API_KEY.matcher(message)
                .replaceAll("[REDACTED]")).replaceAll("$1[REDACTED]")).replaceAll("$1[REDACTED]");
    }
}