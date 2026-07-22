package edu.ohsu.cmp.ecareplan.util.logging;

import java.util.List;
import java.util.regex.Pattern;

public class LogRedactor {
    private static final String REDACTION = "[REDACTED]";

    private static final List<Pattern> REDACTION_PATTERNS = List.of(
            Pattern.compile("(Patient/[^&\\s]+)")
    );

    private LogRedactor() {
    }

    public static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String redacted = value;

        for (Pattern pattern : REDACTION_PATTERNS) {
            redacted = pattern.matcher(redacted).replaceAll(REDACTION);
        }

        return redacted;
    }
}
