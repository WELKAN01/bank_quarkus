package org.transaction.account.domain.exception;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException() {
        super("Saldo insuficiente para realizar el retiro");
    }
}
