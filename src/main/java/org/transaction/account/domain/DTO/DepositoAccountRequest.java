package org.transaction.account.domain.DTO;

import java.math.BigDecimal;

public record DepositoAccountRequest(String numero_cuenta, String cuenta_interbancaria,
        BigDecimal amount, String moneda) {
    
}
