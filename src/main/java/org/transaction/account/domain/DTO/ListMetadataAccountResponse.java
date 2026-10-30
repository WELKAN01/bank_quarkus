package org.transaction.account.domain.DTO;

import java.util.List;

import org.transaction.account.domain.model.account;

public record ListMetadataAccountResponse(
    int num_pagina,
    int pagina_actual,
    int total_registro,
    List<account> accountList
) {

}
