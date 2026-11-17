package org.transaction.account.domain.DTO;

import java.math.BigDecimal;

public record TransferAccountRequest(
    String numero_cuenta_origen,
    String cuenta_interbancaria_origen,
    String numero_cuenta_destino,
    String cuenta_interbancaria_destino,
    BigDecimal amount,
    String moneda,
    String descripcion) {
}
