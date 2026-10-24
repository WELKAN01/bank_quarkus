package org.transaction.account.domain.port.in;


import org.transaction.account.domain.DTO.CreateAccountRequest;
import org.transaction.account.domain.model.account;

import io.smallrye.mutiny.Uni;

public interface CreateAccountUseCase {
        Uni<account> execute(CreateAccountRequest createAccountRequest);

}
