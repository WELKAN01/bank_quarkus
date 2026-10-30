package org.transaction.account.domain.port.out;
import java.math.BigDecimal;
import java.util.Optional;

import org.transaction.account.domain.DTO.ListMetadataAccountResponse;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.model.account;

import io.smallrye.mutiny.Uni;

public interface accountRepositoryPort {
    Uni<Optional<account>> buscarCuentaParaRetiro(WithdrawAccountRequest request);
    Uni<account> save(account account);
    Uni<Optional<account>> extraermontocuenta(
        WithdrawAccountRequest request,
        BigDecimal montoEnMonedaDeCuenta);

    Uni<ListMetadataAccountResponse> ListAccountBank(String num_pagina,String pagina) ;
    Uni<Optional<account>> actualizarCuenta(UpdateAccountRequest request);
}
