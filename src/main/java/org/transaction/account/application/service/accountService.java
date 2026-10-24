package org.transaction.account.application.service;

import java.math.BigDecimal;

import org.transaction.account.domain.DTO.CreateAccountRequest;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.model.account;
import org.transaction.account.domain.port.in.CreateAccountUseCase;
import org.transaction.account.domain.port.in.UpdateAccountUseCase;
import org.transaction.account.domain.port.in.withdrawUseCase;
import org.transaction.account.domain.port.out.ExchangeRatePort;
import org.transaction.account.domain.port.out.accountRepositoryPort;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped 
public class accountService implements  withdrawUseCase,CreateAccountUseCase,UpdateAccountUseCase{

    
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
                () -> new IllegalArgumentException("cuenta no fue encontrada")))
            .onItem().transformToUni(cuenta -> montoEnMonedaDeCuenta(request, cuenta)
                .onItem().transformToUni(montoConvertido ->
                    repositoryPort.extraermontocuenta(request, montoConvertido)))
            .onItem().transform(optional -> optional.orElseThrow(
                () -> new IllegalArgumentException("cuenta no fue encontrada")));
    }

    @Override
    public Uni<account> execute(CreateAccountRequest createAccountRequest) {
        account nuevaCuenta = new account(
            createAccountRequest.balance(),
            createAccountRequest.nombre_dueino(),
            createAccountRequest.numero_cuenta(),
            createAccountRequest.numero_interbbancario(),
            createAccountRequest.moneda()
        );
        return  repositoryPort.save(nuevaCuenta);
    }

    private Uni<BigDecimal> montoEnMonedaDeCuenta(
            WithdrawAccountRequest request, account cuenta) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("El monto debe ser positivo"));
        }

        String monedaOrigen = normalizarMoneda(request.moneda());
        String monedaDestino = normalizarMoneda(cuenta.getMoneda());

        if (monedaOrigen.equals(monedaDestino)) {
            return Uni.createFrom().item(request.amount());
        }

        return exchangeRatePort.getRate(monedaOrigen, monedaDestino)
            .map(tasa -> request.amount().multiply(tasa));
    }

    private String normalizarMoneda(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            throw new IllegalArgumentException("La moneda es obligatoria");
        }
        return moneda.trim().toUpperCase();
    }

    @Override
    public Uni<account> execute(UpdateAccountRequest request) {
        return repositoryPort.actualizarCuenta(request)
            .onItem().transform(optional -> optional.orElseThrow(
                () -> new IllegalArgumentException("Cuenta no se encontro")));
    }


}
