package org.transaction.account.domain.DTO;

public record UpdateAccountRequest(
    String numero_cuenta,
    String cuenta_interbancaria,
    AccountPatch cambios
) {
}
