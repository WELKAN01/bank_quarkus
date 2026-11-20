package org.transaction.account.infrastructure.adapter.out.persistence;

import org.transaction.account.infrastructure.adapter.out.entity.IdempotencyEntity;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdempotencyPanacheRepository implements PanacheRepositoryBase<IdempotencyEntity, Long> {
}
