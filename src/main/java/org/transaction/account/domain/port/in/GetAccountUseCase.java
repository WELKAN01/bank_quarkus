package org.transaction.account.domain.port.in;

import org.transaction.account.domain.DTO.ListMetadataAccountResponse;

import io.smallrye.mutiny.Uni;

/**
 * GetAccountUseCase
 */
public interface GetAccountUseCase{
    Uni<ListMetadataAccountResponse> execute(String num_pagina,String pagina_actual);
}

