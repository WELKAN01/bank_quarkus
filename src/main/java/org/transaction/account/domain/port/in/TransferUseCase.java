package org.transaction.account.domain.port.in;

import org.transaction.account.domain.DTO.TransferAccountRequest;
import org.transaction.account.domain.model.TransactionHistory;

import io.smallrye.mutiny.Uni;

public interface TransferUseCase {
    Uni<TransactionHistory> execute(TransferAccountRequest request);
}
