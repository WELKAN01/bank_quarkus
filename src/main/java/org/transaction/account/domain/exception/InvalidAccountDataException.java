package org.transaction.account.domain.exception;

public class InvalidAccountDataException extends DomainException {

    public InvalidAccountDataException(String message) {
        super(message);
    }
}
