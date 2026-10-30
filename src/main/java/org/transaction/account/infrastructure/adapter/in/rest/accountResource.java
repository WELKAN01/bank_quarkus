package org.transaction.account.infrastructure.adapter.in.rest;
import org.transaction.account.domain.DTO.CreateAccountRequest;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.port.in.CreateAccountUseCase;
import org.transaction.account.domain.port.in.GetAccountUseCase;
import org.transaction.account.domain.port.in.UpdateAccountUseCase;
import org.transaction.account.domain.port.in.withdrawUseCase;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path ("/accounts")
@Produces (MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
public class accountResource {

    @Inject 
    withdrawUseCase withdrawAccountUseCase;

    @Inject
    CreateAccountUseCase createAccountUseCase;

    @Inject 
    UpdateAccountUseCase updateAccountUseCase;

    @Inject 
    GetAccountUseCase getAccountUseCase;


    @POST 
    @Path("/extraccion")
    public Uni<Response> extraccioncuenta(WithdrawAccountRequest withdrawAccountRequest) {

        return withdrawAccountUseCase.execute(withdrawAccountRequest).map(
            cuenta -> Response.status(Response.Status.OK).entity(cuenta).build()
        );
    }


    @POST 
    @Path("/create-account")
    public Uni<Response> crearcuenta(CreateAccountRequest createAccountRequest){
        return createAccountUseCase.execute(createAccountRequest).map(
            cuenta -> Response.status(Response.Status.OK).entity(cuenta).build()

        );
    }

    @PATCH 
    @Path("Update-account")
    public Uni<Response> updatecuenta(UpdateAccountRequest updateAccountRequest){
        return updateAccountUseCase.execute(updateAccountRequest).map(
            cuenta -> Response.status(Response.Status.OK).entity(cuenta).build()
        );
    }

    @GET
    @Path("Info_cuentas")
    public Uni<Response> obtenercuentas(
        @QueryParam("numero_pagina") @DefaultValue ("15") String limit,
        @QueryParam ("pagina_actual") String page ) {
        return getAccountUseCase.execute(limit,page).map(
            cuenta -> Response.status(Response.Status.OK).entity(cuenta).build()
        );
    }
}
