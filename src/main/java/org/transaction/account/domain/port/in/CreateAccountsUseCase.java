package org.transaction.account.domain.port.in;

import java.util.List;

import org.transaction.account.domain.DTO.CreateAccountsRequest;
import org.transaction.account.domain.model.account;

import io.smallrye.mutiny.Uni;

public interface CreateAccountsUseCase {
    Uni<List<account>> execute(CreateAccountsRequest request);
}
