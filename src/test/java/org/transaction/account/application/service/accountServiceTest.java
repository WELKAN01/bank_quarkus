package org.transaction.account.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.transaction.account.domain.DTO.AccountPatch;
import org.transaction.account.domain.DTO.ListMetadataAccountResponse;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.model.account;
import org.transaction.account.domain.port.out.ExchangeRatePort;
import org.transaction.account.domain.port.out.accountRepositoryPort;

import io.smallrye.mutiny.Uni;

class accountServiceTest {

    @Test
    void buscaLaCuentaAntesDeExtraerElMonto() {
        account cuenta = cuentaConSaldo("100.00", "PEN");
        RepositorioFalso repositorio = new RepositorioFalso(cuenta);
        accountService servicio = new accountService(repositorio, new TipoCambioFalso(BigDecimal.ONE));
        WithdrawAccountRequest solicitud = new WithdrawAccountRequest("001", null, new BigDecimal("25.00"), "PEN");

        account resultado = servicio.execute(solicitud).await().indefinitely();

        assertEquals(1, repositorio.busquedasParaRetiro);
        assertEquals(solicitud, repositorio.solicitudBuscada);
        assertEquals(new BigDecimal("75.00"), resultado.getBalance());
    }

    @Test
    void extraeElMontoConvertidoALaMonedaDeLaCuenta() {
        account cuenta = cuentaConSaldo("100.00", "PEN");
        RepositorioFalso repositorio = new RepositorioFalso(cuenta);
        TipoCambioFalso tipoCambio = new TipoCambioFalso(new BigDecimal("3.50"));
        accountService servicio = new accountService(repositorio, tipoCambio);
        WithdrawAccountRequest solicitud = new WithdrawAccountRequest("001", null, new BigDecimal("10.00"), "usd");

        servicio.execute(solicitud).await().indefinitely();

        assertEquals("USD", tipoCambio.monedaOrigen);
        assertEquals("PEN", tipoCambio.monedaDestino);
        assertEquals(new BigDecimal("35.00"), repositorio.montoExtraido);
        assertEquals(new BigDecimal("65.00"), cuenta.getBalance());
    }

    @Test
    void listaCuentasConLosParametrosDePaginacionRecibidos() {
        RepositorioFalso repositorio = new RepositorioFalso(cuentaConSaldo("20.00", "PEN"));
        accountService servicio = new accountService(repositorio, new TipoCambioFalso(BigDecimal.ONE));

        ListMetadataAccountResponse resultado = servicio.execute("2", "10").await().indefinitely();

        assertEquals("2", repositorio.numeroPaginaRecibido);
        assertEquals("10", repositorio.tamanioPaginaRecibido);
        assertEquals(1, resultado.total_registro());
        assertEquals("001", resultado.accountList().get(0).getNumero_cuenta());
    }

    @Test
    void actualizaCuentaYDevuelveLaCuentaActualizada() {
        account cuenta = cuentaConSaldo("100.00", "PEN");
        RepositorioFalso repositorio = new RepositorioFalso(cuenta);
        accountService servicio = new accountService(repositorio, new TipoCambioFalso(BigDecimal.ONE));
        UpdateAccountRequest solicitud = new UpdateAccountRequest("001", null, new AccountPatch("Ana Torres", "usd"));

        account resultado = servicio.execute(solicitud).await().indefinitely();

        assertEquals(solicitud, repositorio.solicitudActualizada);
        assertEquals("Ana Torres", resultado.getNombre_dueino());
        assertEquals("USD", resultado.getMoneda());
    }

    private static account cuentaConSaldo(String saldo, String moneda) {
        return new account(new BigDecimal(saldo), "Cliente inicial", "001", "CCI-001", moneda);
    }

    private static final class TipoCambioFalso implements ExchangeRatePort {
        private final BigDecimal tasa;
        private String monedaOrigen;
        private String monedaDestino;

        private TipoCambioFalso(BigDecimal tasa) {
            this.tasa = tasa;
        }

        @Override
        public Uni<BigDecimal> getRate(String base, String quote) {
            monedaOrigen = base;
            monedaDestino = quote;
            return Uni.createFrom().item(tasa);
        }
    }

    private static final class RepositorioFalso implements accountRepositoryPort {
        private final account cuenta;
        private int busquedasParaRetiro;
        private WithdrawAccountRequest solicitudBuscada;
        private BigDecimal montoExtraido;
        private String numeroPaginaRecibido;
        private String tamanioPaginaRecibido;
        private UpdateAccountRequest solicitudActualizada;

        private RepositorioFalso(account cuenta) {
            this.cuenta = cuenta;
        }

        @Override
        public Uni<Optional<account>> buscarCuentaParaRetiro(WithdrawAccountRequest request) {
            busquedasParaRetiro++;
            solicitudBuscada = request;
            return Uni.createFrom().item(Optional.of(cuenta));
        }

        @Override
        public Uni<Optional<account>> extraermontocuenta(WithdrawAccountRequest request, BigDecimal monto) {
            montoExtraido = monto;
            cuenta.retiro(monto);
            return Uni.createFrom().item(Optional.of(cuenta));
        }

        @Override
        public Uni<ListMetadataAccountResponse> ListAccountBank(String numeroPagina, String tamanioPagina) {
            numeroPaginaRecibido = numeroPagina;
            tamanioPaginaRecibido = tamanioPagina;
            return Uni.createFrom().item(new ListMetadataAccountResponse(
                Integer.parseInt(numeroPagina), Integer.parseInt(tamanioPagina), 1, List.of(cuenta)));
        }

        @Override
        public Uni<Optional<account>> actualizarCuenta(UpdateAccountRequest request) {
            solicitudActualizada = request;
            AccountPatch cambios = request.cambios();
            if (cambios.nombre_dueino() != null) {
                cuenta.cambiarNombreDueino(cambios.nombre_dueino());
            }
            if (cambios.moneda() != null) {
                cuenta.cambiarMoneda(cambios.moneda());
            }
            return Uni.createFrom().item(Optional.of(cuenta));
        }

        @Override
        public Uni<account> save(account cuenta) {
            return Uni.createFrom().item(cuenta);
        }
    }
}
