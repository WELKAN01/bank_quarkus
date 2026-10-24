package org.transaction.account.domain.port.in;

import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.model.account;

import io.smallrye.mutiny.Uni;

public interface UpdateAccountUseCase {
    Uni<account> execute(UpdateAccountRequest accountrequest);
}
