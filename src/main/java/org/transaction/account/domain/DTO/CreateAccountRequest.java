package org.transaction.account.domain.DTO;

import java.math.BigDecimal;

public record CreateAccountRequest(BigDecimal balance,String moneda, String nombre_dueino,String numero_cuenta,String numero_interbbancario) {
    
}
