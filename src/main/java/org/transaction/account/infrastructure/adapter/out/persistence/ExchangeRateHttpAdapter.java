package org.transaction.account.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.transaction.account.domain.port.out.ExchangeRatePort;
import org.transaction.account.infrastructure.adapter.out.Client.ExchangeRateClient;
import org.transaction.account.infrastructure.adapter.out.Client.DTO.ExchangeRateResponse;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped 
public class ExchangeRateHttpAdapter implements ExchangeRatePort{

    @Inject 
    @RestClient 
    ExchangeRateClient client;

    @Override
    public Uni<BigDecimal> getRate(String from, String to) {
        return client.getRate(from, to)
            .map(ExchangeRateResponse::rate);

    }

}
