package org.transaction.account.domain.port.out;

import java.math.BigDecimal;

import io.smallrye.mutiny.Uni;

public interface ExchangeRatePort {
    Uni<BigDecimal> getRate(String base,String quote);
    
}
