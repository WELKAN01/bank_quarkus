package org.transaction.account.infrastructure.adapter.out.Client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.transaction.account.infrastructure.adapter.out.Client.DTO.ExchangeRateResponse;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path ("/v2/rate")
@RegisterRestClient (configKey = "exchange-api")
@Produces (MediaType.APPLICATION_JSON)
public interface ExchangeRateClient {

    @GET 
    @Path("/{from}/{to}")
    Uni<ExchangeRateResponse> getRate(
        @PathParam ("from") String from,
        @PathParam ("to") String to
    );
}
