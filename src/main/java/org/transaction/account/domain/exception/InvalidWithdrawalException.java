package org.transaction.account.domain.exception;

public class InvalidWithdrawalException extends DomainException {

    public InvalidWithdrawalException(String message) {
        super(message);
    }
}
