package org.transaction.account.domain.port.in;
import org.transaction.account.domain.DTO.DepositoAccountRequest;
import org.transaction.account.domain.model.account;

import io.smallrye.mutiny.Uni;

public interface DepositoUseCase {
    Uni<account> execute(DepositoAccountRequest request);
}
