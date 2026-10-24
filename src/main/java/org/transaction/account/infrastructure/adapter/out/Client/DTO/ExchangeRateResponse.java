package org.transaction.account.infrastructure.adapter.out.Client.DTO;

import java.math.BigDecimal;

public record ExchangeRateResponse(
    String date,
    String base,
    String quote,
    BigDecimal rate
) {
    
}
