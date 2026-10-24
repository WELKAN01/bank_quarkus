package org.transaction.account.domain.DTO;

import java.math.BigDecimal;

public record WithdrawAccountRequest(String numero_cuenta, String cuenta_interbancaria,
        BigDecimal amount, String moneda) {
    
}
