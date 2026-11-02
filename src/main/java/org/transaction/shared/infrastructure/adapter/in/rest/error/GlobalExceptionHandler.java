package org.transaction.shared.infrastructure.adapter.in.rest.error;

import java.time.Instant;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.transaction.account.domain.exception.AccountNotFoundException;
import org.transaction.account.domain.exception.InsufficientFundsException;
import org.transaction.account.domain.exception.InvalidAccountDataException;
import org.transaction.account.domain.exception.InvalidDepositException;
import org.transaction.account.domain.exception.InvalidWithdrawalException;

import jakarta.ws.rs.core.Response;

public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @ServerExceptionMapper
    public Response accountNotFound(AccountNotFoundException exception) {
        return error(Response.Status.NOT_FOUND.getStatusCode(), "ACCOUNT_NOT_FOUND", exception.getMessage());
    }

    @ServerExceptionMapper
    public Response insufficientFunds(InsufficientFundsException exception) {
        return error(422, "INSUFFICIENT_FUNDS", exception.getMessage());
    }

    @ServerExceptionMapper
    public Response invalidAccountData(InvalidAccountDataException exception) {
        return error(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_ACCOUNT_DATA", exception.getMessage());
    }

    @ServerExceptionMapper
    public Response invalidWithdrawal(InvalidWithdrawalException exception) {
        return error(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_WITHDRAWAL", exception.getMessage());
    }

    @ServerExceptionMapper
    public Response unexpected(Exception exception) {
        LOG.error("Error no controlado al procesar la solicitud", exception);
        return error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "INTERNAL_ERROR", "Ocurrió un error inesperado");
    }


    @ServerExceptionMapper
    public Response invalidDeposit(InvalidDepositException exception) {
        return error(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_DEPOSIT", exception.getMessage());
    }



    private Response error(int status, String code, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(status, code, message, Instant.now()))
                .build();
    }
}
