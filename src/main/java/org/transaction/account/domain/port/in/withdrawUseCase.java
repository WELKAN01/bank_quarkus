package org.transaction.account.domain.port.in;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.model.account;

import io.smallrye.mutiny.Uni;

public interface withdrawUseCase {
    Uni<account> execute(WithdrawAccountRequest request);
}
