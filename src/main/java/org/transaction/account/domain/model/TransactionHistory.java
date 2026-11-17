package org.transaction.account.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistory(
    String origen,
    String destino,
    BigDecimal monto,
    String moneda,
    String tipo,
    String descripcion,
    LocalDateTime fecha
) {
    public TransactionHistory(
        String origen,
        String destino,
        BigDecimal monto,
        String moneda,
        String tipo,
        String descripcion
    ) {
        this(origen, destino, monto, moneda, tipo, descripcion, LocalDateTime.now());
    }
}
