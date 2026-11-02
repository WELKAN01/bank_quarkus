package org.transaction.account.application.service;

import java.math.BigDecimal;
import java.util.List;

import org.transaction.account.domain.DTO.CreateAccountRequest;
import org.transaction.account.domain.DTO.CreateAccountsRequest;
import org.transaction.account.domain.DTO.DepositoAccountRequest;
import org.transaction.account.domain.DTO.ListMetadataAccountResponse;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.exception.AccountNotFoundException;
import org.transaction.account.domain.exception.InvalidAccountDataException;
import org.transaction.account.domain.exception.InvalidDepositException;
import org.transaction.account.domain.exception.InvalidWithdrawalException;
import org.transaction.account.domain.model.account;
import org.transaction.account.domain.port.in.CreateAccountUseCase;
import org.transaction.account.domain.port.in.CreateAccountsUseCase;
import org.transaction.account.domain.port.in.DepositoUseCase;
import org.transaction.account.domain.port.in.GetAccountUseCase;
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
    DepositoUseCase {

    
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
        return repositoryPort.save(toDomain(createAccountRequest));
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
            .map(tasa -> amount.multiply(tasa));
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

    @Override
    public Uni<ListMetadataAccountResponse> execute(String num_pagina,String pagina_actual) {
        return repositoryPort.ListAccountBank(num_pagina, pagina_actual);
    }

    

}
