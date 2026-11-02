package org.transaction.account.domain.exception;

public class InvalidDepositException extends DomainException {

    public InvalidDepositException(String message) {
        super(message);
    }
}
