package org.transaction.account.domain.DTO;

import java.util.List;

public record CreateAccountsRequest(List<CreateAccountRequest> accounts) {
}
