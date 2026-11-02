package org.transaction.account.domain.exception;

/** Base exception for business-rule violations in the account domain. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
