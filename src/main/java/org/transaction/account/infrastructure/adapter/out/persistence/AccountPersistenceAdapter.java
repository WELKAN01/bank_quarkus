package org.transaction.account.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.Optional;

import org.transaction.account.domain.DTO.AccountPatch;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.model.account;
import org.transaction.account.domain.port.out.accountRepositoryPort;
import org.transaction.account.infrastructure.adapter.out.entity.AccountEntity;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

@ApplicationScoped 
public class AccountPersistenceAdapter implements accountRepositoryPort {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Override
    public Uni<Optional<account>> buscarCuentaParaRetiro(WithdrawAccountRequest request) {
        if (request == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("La solicitud de retiro es obligatoria"));
        }
        return buscarCuenta(request.numero_cuenta(), request.cuenta_interbancaria());
    }

    @Override
    public Uni<Optional<account>> actualizarCuenta(UpdateAccountRequest request) {
        if (request == null || request.cambios() == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("Los cambios de la cuenta son obligatorios"));
        }

        return sessionFactory.withTransaction(session ->
            buscarCuenta(session, request.numero_cuenta(), request.cuenta_interbancaria(), true)
                .onItem().transform(entity -> {
                    if (entity == null) {
                        return Optional.empty();
                    }

                    account cuenta = toDomain(entity);
                    aplicarCambios(cuenta, request.cambios());
                    entity.setNombre_dueino(cuenta.getNombre_dueino());
                    entity.setMoneda(cuenta.getMoneda());
                    return Optional.of(cuenta);
                })
        );
    }

    @Override
    public Uni<account> save(account account) {
        return sessionFactory.withTransaction(session -> {
            AccountEntity entity = new AccountEntity();
            entity.setNombre_dueino(account.getNombre_dueino());
            entity.setNumero_cuenta(account.getNumero_cuenta());
            entity.setNumero_interbancario(account.getNumero_interbancario());
            entity.setBalance(account.getBalance());
            entity.setMoneda(account.getMoneda());

            if (entity.getId() == null) {
                return session.persist(entity)
                        .replaceWith(entity)
                        .map(this::toDomain);
            }
            return session.merge(entity).map(this::toDomain);
        });
    }

    @Override
    public Uni<Optional<account>> extraermontocuenta(
            WithdrawAccountRequest request,
            BigDecimal montoEnMonedaDeCuenta) {
        if (request == null || montoEnMonedaDeCuenta == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("La solicitud y el monto son obligatorios"));
        }
        return sessionFactory.withTransaction(session ->
            buscarCuenta(session, request.numero_cuenta(), request.cuenta_interbancaria(), true)
                .onItem().transform(entity -> {
                    if (entity == null) {
                        return Optional.empty();
                    }

                    account cuenta = toDomain(entity);
                    cuenta.retiro(montoEnMonedaDeCuenta);
                    entity.setBalance(cuenta.getBalance());
                    return Optional.of(cuenta);
                })
        );
    }

    private Uni<AccountEntity> buscarCuenta(
            Mutiny.Session session,
            String numeroCuenta,
            String cuentaInterbancaria,
            boolean bloquear) {
        String consulta;
        String numero;
        boolean tieneNumeroCuenta = numeroCuenta != null
                && !numeroCuenta.isBlank();
        boolean tieneCci = cuentaInterbancaria != null
                && !cuentaInterbancaria.isBlank();

        if (tieneNumeroCuenta == tieneCci) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                "Debe enviar exactamente numero_cuenta o cuenta_interbancaria"));
        }

        if (tieneNumeroCuenta) {
            consulta = """
                SELECT a FROM AccountEntity a
                WHERE a.numero_cuenta = :numero
                """;
            numero = numeroCuenta;
        } else {
            consulta = """
                SELECT a FROM AccountEntity a
                WHERE a.numero_interbancario = :numero
                """;
            numero = cuentaInterbancaria;
        }

        var query = session.createSelectionQuery(consulta, AccountEntity.class)
            .setParameter("numero", numero)
            ;

        if (bloquear) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        return query.getSingleResultOrNull();
    }

    private Uni<Optional<account>> buscarCuenta(
            String numeroCuenta,
            String cuentaInterbancaria) {
        return sessionFactory.withSession(session ->
            buscarCuenta(session, numeroCuenta, cuentaInterbancaria, false)
        ).map(entity -> Optional.ofNullable(entity).map(this::toDomain));
    }

    private account toDomain(AccountEntity entity) {
        return new account(
            entity.getBalance(),
            entity.getNombre_dueino(),
            entity.getNumero_cuenta(),
            entity.getNumero_interbancario(),
            entity.getMoneda()
        );
    }

    private void aplicarCambios(account cuenta, AccountPatch cambios) {
        if (cambios.nombre_dueino() != null) {
            cuenta.cambiarNombreDueino(cambios.nombre_dueino());
        }
        if (cambios.moneda() != null) {
            cuenta.cambiarMoneda(cambios.moneda());
        }
    }

}
