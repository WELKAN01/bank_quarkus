package org.transaction.account.domain.exception;

public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException() {
        super("La cuenta no fue encontrada");
    }
}
