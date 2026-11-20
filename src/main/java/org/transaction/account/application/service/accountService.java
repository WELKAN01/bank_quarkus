package org.transaction.account.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.transaction.account.domain.DTO.CreateAccountRequest;
import org.transaction.account.domain.DTO.CreateAccountsRequest;
import org.transaction.account.domain.DTO.DepositoAccountRequest;
import org.transaction.account.domain.DTO.ListMetadataAccountResponse;
import org.transaction.account.domain.DTO.TransferAccountRequest;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.exception.AccountNotFoundException;
import org.transaction.account.domain.exception.InvalidAccountDataException;
import org.transaction.account.domain.exception.InvalidDepositException;
import org.transaction.account.domain.exception.InvalidWithdrawalException;
import org.transaction.account.domain.model.TransactionHistory;
import org.transaction.account.domain.model.account;
import org.transaction.account.domain.port.in.CreateAccountUseCase;
import org.transaction.account.domain.port.in.CreateAccountsUseCase;
import org.transaction.account.domain.port.in.DepositoUseCase;
import org.transaction.account.domain.port.in.GetAccountUseCase;
import org.transaction.account.domain.port.in.TransferUseCase;
import org.transaction.account.domain.port.in.UpdateAccountUseCase;
import org.transaction.account.domain.port.in.withdrawUseCase;
import org.transaction.account.domain.port.out.ExchangeRatePort;
import org.transaction.account.domain.port.out.accountRepositoryPort;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped 
public class accountService implements 
    withdrawUseCase,
    CreateAccountUseCase,
    CreateAccountsUseCase,
    UpdateAccountUseCase,
    GetAccountUseCase,
    DepositoUseCase,
    TransferUseCase {

    
    private final accountRepositoryPort repositoryPort;
    private final ExchangeRatePort exchangeRatePort;

    @Inject 
    public accountService(accountRepositoryPort accountRepositoryPort,ExchangeRatePort exchangeRatePort){
        this.repositoryPort= accountRepositoryPort;
        this.exchangeRatePort= exchangeRatePort;
    }

    @Override
    public Uni<account> execute(WithdrawAccountRequest request) {
        return repositoryPort.buscarCuentaParaRetiro(request)
            .onItem().transform(optional -> optional.orElseThrow(
                AccountNotFoundException::new))
            .onItem().transformToUni(cuenta -> montoEnMonedaDeCuenta(request, cuenta)
                .onItem().transformToUni(montoConvertido ->
                    repositoryPort.extraermontocuenta(request, montoConvertido)))
            .onItem().transform(optional -> optional.orElseThrow(
                AccountNotFoundException::new));
    }

    @Override
    public Uni<account> execute(CreateAccountRequest createAccountRequest) {
        return execute(createAccountRequest, null);
    }

    @Override
    public Uni<account> execute(CreateAccountRequest createAccountRequest, String idempotencyKey) {
        if (createAccountRequest == null) {
            return Uni.createFrom().failure(
                new InvalidAccountDataException("Los datos de la cuenta son obligatorios"));
        }


        // Validar que el balance inicial sea positivo
        String payloadHash = generarPayloadHash(createAccountRequest);

        // Validar que el balance inicial sea positivo
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return repositoryPort.buscarHashPorIdempotencyKey(idempotencyKey)
                .flatMap(hashGuardado -> {
                    if (hashGuardado.isPresent()) {
                        if (!payloadHash.equals(hashGuardado.get())) {
                            return Uni.createFrom().failure(
                                new InvalidAccountDataException("La clave idempotente ya fue usada con otra solicitud"));
                        }
                        return repositoryPort.buscarCuentaPorIdempotencyKey(idempotencyKey)
                            .flatMap(cuentaExistente -> {
                                if (cuentaExistente.isPresent()) {
                                    return Uni.createFrom().item(cuentaExistente.get());
                                }
                                return Uni.createFrom().failure(new AccountNotFoundException());
                            });
                    }
                    return repositoryPort.buscarCuentaPorNumeroCuentaOInterbancario(
                            createAccountRequest.numero_cuenta(),
                            createAccountRequest.numero_interbbancario())
                        .flatMap(cuentaExistente -> {
                            if (cuentaExistente.isPresent()) {
                                return Uni.createFrom().item(cuentaExistente.get());
                            }
                            return repositoryPort.save(toDomain(createAccountRequest), idempotencyKey, payloadHash);
                        });
                });
        }

        return repositoryPort.buscarCuentaPorNumeroCuentaOInterbancario(
                createAccountRequest.numero_cuenta(),
                createAccountRequest.numero_interbbancario())
            .flatMap(cuentaExistente -> {
                if (cuentaExistente.isPresent()) {
                    return Uni.createFrom().item(cuentaExistente.get());
                }
                return repositoryPort.save(toDomain(createAccountRequest), null, payloadHash);
            });
    }

    private String generarPayloadHash(CreateAccountRequest request) {
        String payload = String.join("|",
            request.nombre_dueino() == null ? "" : request.nombre_dueino(),
            request.numero_cuenta() == null ? "" : request.numero_cuenta(),
            request.numero_interbbancario() == null ? "" : request.numero_interbbancario(),
            request.moneda() == null ? "" : request.moneda(),
            request.balance() == null ? "" : request.balance().toPlainString() );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo generar el hash de la cuenta", e);
        }
    }
    
    @Override
    public Uni<account> execute(DepositoAccountRequest request) {
        return repositoryPort.buscarCuentaParaDeposito(request)
            .onItem().transform(optional -> optional.orElseThrow(
                AccountNotFoundException::new))
            .onItem().transformToUni(cuenta -> montoEnMonedaDeCuenta(request, cuenta)
                .onItem().transformToUni(montoConvertido -> {
                    return repositoryPort.depositarMontocuenta(request, montoConvertido);
                }))
            .onItem().transform(optional -> optional.orElseThrow(
                AccountNotFoundException::new));
    }

    @Override
    public Uni<List<account>> execute(CreateAccountsRequest request) {
        if (request == null || request.accounts() == null || request.accounts().isEmpty()) {
            return Uni.createFrom().failure(
                new InvalidAccountDataException("Debe enviar al menos una cuenta"));
        }

        return repositoryPort.saveAll(request.accounts().stream()
            .map(this::toDomain)
            .toList());
    }

    private account toDomain(CreateAccountRequest request) {
        if (request == null) {
            throw new InvalidAccountDataException("Los datos de la cuenta son obligatorios");
        }

        return new account(
            request.balance(),
            request.nombre_dueino(),
            request.numero_cuenta(),
            request.numero_interbbancario(),
            request.moneda()
        );
    }

    private Uni<BigDecimal> montoEnMonedaDeCuenta(WithdrawAccountRequest request, account cuenta) {
        return montoEnMonedaDeCuenta(request.amount(), request.moneda(), cuenta);
    }

    private Uni<BigDecimal> montoEnMonedaDeCuenta(DepositoAccountRequest request, account cuenta) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            return Uni.createFrom().failure(
                new InvalidDepositException("El monto debe ser positivo"));
        }
        return montoEnMonedaDeCuenta(request.amount(), request.moneda(), cuenta);
    }

    private Uni<BigDecimal> montoEnMonedaDeCuenta(BigDecimal amount, String moneda, account cuenta) {
        if (amount == null || amount.signum() <= 0) {
            return Uni.createFrom().failure(
                new InvalidWithdrawalException("El monto debe ser positivo"));
        }

        String monedaOrigen = normalizarMoneda(moneda);
        String monedaDestino = normalizarMoneda(cuenta.getMoneda());

        if (monedaOrigen.equals(monedaDestino)) {
            return Uni.createFrom().item(amount);
        }

        return exchangeRatePort.getRate(monedaOrigen, monedaDestino)
            .map(tasa -> amount.multiply(tasa).setScale(2, RoundingMode.HALF_UP));
    }

    private String normalizarMoneda(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            throw new InvalidAccountDataException("La moneda es obligatoria");
        }
        return moneda.trim().toUpperCase();
    }

    @Override
    public Uni<account> execute(UpdateAccountRequest request) {
        return repositoryPort.actualizarCuenta(request)
            .onItem().transform(optional -> optional.orElseThrow(
                AccountNotFoundException::new));
    }

    public Uni<TransactionHistory> transferir(TransferAccountRequest request) {
        return execute(request);
    }

    @Override
    public Uni<TransactionHistory> execute(TransferAccountRequest request) {
        if (request == null) {
            return Uni.createFrom().failure(
                new InvalidAccountDataException("La transferencia es obligatoria"));
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            return Uni.createFrom().failure(
                new InvalidWithdrawalException("El monto debe ser positivo"));
        }

        return repositoryPort.buscarCuentaParaRetiro(new WithdrawAccountRequest(
                request.numero_cuenta_origen(),
                request.cuenta_interbancaria_origen(),
                request.amount(),
                request.moneda()))
            .onItem().transform(optional -> optional.orElseThrow(AccountNotFoundException::new))
            .onItem().transformToUni(origen ->
                repositoryPort.buscarCuentaParaDeposito(new DepositoAccountRequest(
                        request.numero_cuenta_destino(),
                        request.cuenta_interbancaria_destino(),
                        request.amount(),
                        request.moneda()))
                    .onItem().transform(optional -> optional.orElseThrow(AccountNotFoundException::new))
                    .onItem().transformToUni(destino -> {
                        Uni<BigDecimal> montoOrigen = montoEnMonedaDeCuenta(request.amount(), request.moneda(), origen);
                        Uni<BigDecimal> montoDestino = montoEnMonedaDeCuenta(request.amount(), request.moneda(), destino);
                        return Uni.combine().all().unis(montoOrigen, montoDestino)
                            .asTuple()
                            .flatMap(tuple -> repositoryPort.transferirEntreCuentas(
                                request,
                                tuple.getItem1(),
                                tuple.getItem2()));
                    })
            );
    }

    @Override
    public Uni<ListMetadataAccountResponse> execute(String num_pagina,String pagina_actual) {
        return repositoryPort.ListAccountBank(num_pagina, pagina_actual);
    }

    

}
