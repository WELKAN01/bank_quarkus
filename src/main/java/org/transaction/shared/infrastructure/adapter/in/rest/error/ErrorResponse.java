package org.transaction.shared.infrastructure.adapter.in.rest.error;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String code,
        String message,
        Instant timestamp) {
}
