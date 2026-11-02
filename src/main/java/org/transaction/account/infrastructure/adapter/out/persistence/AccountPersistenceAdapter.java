package org.transaction.account.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.transaction.account.domain.DTO.AccountPatch;
import org.transaction.account.domain.DTO.DepositoAccountRequest;
import org.transaction.account.domain.DTO.ListMetadataAccountResponse;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.exception.InvalidAccountDataException;
import org.transaction.account.domain.model.account;
import org.transaction.account.domain.port.out.accountRepositoryPort;
import org.transaction.account.infrastructure.adapter.out.entity.AccountEntity;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.inject.Inject;

@ApplicationScoped 
public class AccountPersistenceAdapter implements accountRepositoryPort {
    
    @Inject
    AccountPanacheRepository accountRepository;
    
    @Override
    public Uni<Optional<account>> buscarCuentaParaDeposito(DepositoAccountRequest request) {
        if (request == null) {
            return Uni.createFrom().failure(
                new InvalidAccountDataException("La solicitud de depósito es obligatoria"));
        }
        return buscarCuenta(request.numero_cuenta(), request.cuenta_interbancaria());
    }
    
    @Override
    public Uni<Optional<account>> buscarCuentaParaRetiro(WithdrawAccountRequest request) {
        if (request == null) {
            return Uni.createFrom().failure(
                new InvalidAccountDataException("La solicitud de retiro es obligatoria"));
        }
        return buscarCuenta(request.numero_cuenta(), request.cuenta_interbancaria());
    }

    @Override
    public Uni<Optional<account>> actualizarCuenta(UpdateAccountRequest request) {
        if (request == null || request.cambios() == null) {
            return Uni.createFrom().failure(
                new InvalidAccountDataException("Los cambios de la cuenta son obligatorios"));
        }

        return Panache.withTransaction(() ->
            buscarCuenta(request.numero_cuenta(), request.cuenta_interbancaria(), true)
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
        return Panache.withTransaction(() -> {
            AccountEntity entity = new AccountEntity();
            entity.setNombre_dueino(account.getNombre_dueino());
            entity.setNumero_cuenta(account.getNumero_cuenta());
            entity.setNumero_interbancario(account.getNumero_interbancario());
            entity.setBalance(account.getBalance());
            entity.setMoneda(account.getMoneda());

            return accountRepository.persist(entity).map(this::toDomain);
        });
    }

    @Override
    public Uni<List<account>> saveAll(List<account> accounts) {
        return Panache.withTransaction(() ->
            Multi.createFrom().iterable(accounts)
                .onItem().transform(this::toEntity)
                .onItem().transformToUniAndConcatenate(entity ->
                    accountRepository.persist(entity).map(this::toDomain))
                .collect().asList()
        );
    }

    @Override
    public Uni<Optional<account>> extraermontocuenta(
            WithdrawAccountRequest request,
            BigDecimal montoEnMonedaDeCuenta) {
        if (request == null || montoEnMonedaDeCuenta == null) {
            return Uni.createFrom().failure(
                new InvalidAccountDataException("La solicitud y el monto son obligatorios"));
        }
        return Panache.withTransaction(() ->
            buscarCuenta(request.numero_cuenta(), request.cuenta_interbancaria(), true)
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
            String numeroCuenta,
            String cuentaInterbancaria,
            boolean bloquear) {
        String campo;
        String numero;
        boolean tieneNumeroCuenta = numeroCuenta != null
                && !numeroCuenta.isBlank();
        boolean tieneCci = cuentaInterbancaria != null
                && !cuentaInterbancaria.isBlank();

        if (tieneNumeroCuenta == tieneCci) {
            return Uni.createFrom().failure(new InvalidAccountDataException(
                "Debe enviar exactamente numero_cuenta o cuenta_interbancaria"));
        }

        if (tieneNumeroCuenta) {
            campo = "numero_cuenta";
            numero = numeroCuenta;
        } else {
            campo = "numero_interbancario";
            numero = cuentaInterbancaria;
        }

        if (bloquear) {
            return accountRepository.find(campo, numero)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResult();
        }
        return accountRepository.find(campo, numero).firstResult();
    }

    private Uni<Optional<account>> buscarCuenta(
            String numeroCuenta,
            String cuentaInterbancaria) {
        return Panache.withSession(() ->
            buscarCuenta(numeroCuenta, cuentaInterbancaria, false)
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

    private AccountEntity toEntity(account account) {
        AccountEntity entity = new AccountEntity();
        entity.setNombre_dueino(account.getNombre_dueino());
        entity.setNumero_cuenta(account.getNumero_cuenta());
        entity.setNumero_interbancario(account.getNumero_interbancario());
        entity.setBalance(account.getBalance());
        entity.setMoneda(account.getMoneda());
        return entity;
    }

    private void aplicarCambios(account cuenta, AccountPatch cambios) {
        if (cambios.nombre_dueino() != null) {
            cuenta.cambiarNombreDueino(cambios.nombre_dueino());
        }
        if (cambios.moneda() != null) {
            cuenta.cambiarMoneda(cambios.moneda());
        }
    }

    @Override
    public Uni<ListMetadataAccountResponse> ListAccountBank(String num_pagina, String pagina) {
        int index= Integer.valueOf(num_pagina);
        int tamanio_pagina =  Integer.valueOf(pagina);
        return Panache.withTransaction(()->{
            return accountRepository.findAll().
                page(Page.of(index, tamanio_pagina)).
                withLock(LockModeType.PESSIMISTIC_READ).
                list().onItem().transform(listaccount ->
                    new ListMetadataAccountResponse(
                            index,
                            tamanio_pagina,
                            listaccount.size()
                            ,listaccount.stream().map(this::toDomain).toList())
            );
        });
    }


    @Override
    public  Uni<Optional<account>> depositarMontocuenta(DepositoAccountRequest request, BigDecimal montoEnMonedaDeCuenta) {
            return Panache.withTransaction(() ->
            buscarCuenta(request.numero_cuenta(), request.cuenta_interbancaria(), true)
            .onItem().transform(entity ->{
                if (entity == null) {
                    return Optional.empty();
                }
                account cuenta = toDomain(entity);
                if(request.moneda()== null || request.moneda().isBlank()){
                    throw new InvalidAccountDataException("La moneda es obligatoria");
                }

                cuenta.depositar(montoEnMonedaDeCuenta);

                entity.setBalance(cuenta.getBalance());
                return Optional.of(cuenta);
            })
        );
    }

    // @Override
    // public Uni<Optional<account>> buscarCuentaParaDeposito(DepositoAccountRequest request) {
    //     if (request == null) {
    //         return Uni.createFrom().failure(
    //             new InvalidAccountDataException("La solicitud de depósito es obligatoria"));
    //     }
    //     return buscarCuenta(request.numero_cuenta(), request.cuenta_interbancaria());
    // }

    

}
