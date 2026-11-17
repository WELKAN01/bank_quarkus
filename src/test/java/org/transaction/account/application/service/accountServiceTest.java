package org.transaction.account.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.transaction.account.domain.DTO.AccountPatch;
import org.transaction.account.domain.DTO.CreateAccountRequest;
import org.transaction.account.domain.DTO.DepositoAccountRequest;
import org.transaction.account.domain.DTO.ListMetadataAccountResponse;
import org.transaction.account.domain.DTO.TransferAccountRequest;
import org.transaction.account.domain.DTO.UpdateAccountRequest;
import org.transaction.account.domain.DTO.WithdrawAccountRequest;
import org.transaction.account.domain.model.TransactionHistory;
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
    void transfiereMontoEntreCuentasYGuardaElHistorial() {
        account origen = cuentaConSaldo("200.00", "PEN");
        account destino = cuentaConSaldo("50.00", "PEN");
        RepositorioFalso repositorio = new RepositorioFalso(origen, destino);
        accountService servicio = new accountService(repositorio, new TipoCambioFalso(BigDecimal.ONE));
        TransferAccountRequest solicitud = new TransferAccountRequest("001", null, "002", null, new BigDecimal("25.00"), "PEN", "Transferencia entre cuentas");

        TransactionHistory resultado = servicio.transferir(solicitud).await().indefinitely();

        assertEquals(new BigDecimal("175.00"), origen.getBalance());
        assertEquals(new BigDecimal("75.00"), destino.getBalance());
        assertEquals("001", resultado.origen());
        assertEquals("002", resultado.destino());
        assertEquals(new BigDecimal("25.00"), resultado.monto());
        assertEquals("PEN", resultado.moneda());
        assertEquals("TRANSFERENCIA", resultado.tipo());
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

    @Test
    void creaCuentaIdempotenteCuandoSeRepiteLaMismaClave() {
        RepositorioFalso repositorio = new RepositorioFalso(null);
        accountService servicio = new accountService(repositorio, new TipoCambioFalso(BigDecimal.ONE));
        CreateAccountRequest solicitud = new CreateAccountRequest(
            new BigDecimal("100.00"),
            "PEN",
            "Ana Torres",
            "001",
            "CCI-001"
        );

        account primera = servicio.execute(solicitud, "idem-123").await().indefinitely();
        account repetida = servicio.execute(solicitud, "idem-123").await().indefinitely();

        assertEquals("001", primera.getNumero_cuenta());
        assertEquals("001", repetida.getNumero_cuenta());
        assertEquals(1, repositorio.cuentasGuardadas);
    }

    private static account cuentaConSaldo(String saldo, String moneda) {
        return new account(new BigDecimal(saldo), "Cliente inicial", "001", "CCI-001", moneda);
    }

    private static account cuentaConSaldo(String saldo, String moneda, String numeroCuenta) {
        return new account(new BigDecimal(saldo), "Cliente inicial", numeroCuenta, "CCI-" + numeroCuenta, moneda);
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
        private account cuenta;
        private final account cuentaDestino;
        private int busquedasParaRetiro;
        private WithdrawAccountRequest solicitudBuscada;
        private BigDecimal montoExtraido;
        private String numeroPaginaRecibido;
        private String tamanioPaginaRecibido;
        private UpdateAccountRequest solicitudActualizada;
        private TransactionHistory historialGuardado;
        private int cuentasGuardadas;
        private String ultimoIdempotencyKey;
        private String ultimoPayloadHash;

        private RepositorioFalso(account cuenta) {
            this(cuenta, null);
        }

        private RepositorioFalso(account cuenta, account cuentaDestino) {
            this.cuenta = cuenta;
            this.cuentaDestino = cuentaDestino;
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
            return save(cuenta, null, null);
        }

        @Override
        public Uni<account> save(account cuenta, String idempotencyKey, String payloadHash) {
            cuentasGuardadas++;
            ultimoIdempotencyKey = idempotencyKey;
            ultimoPayloadHash = payloadHash;
            if (this.cuenta == null) {
                this.cuenta = cuenta;
            }
            return Uni.createFrom().item(cuenta);
        }

        @Override
        public Uni<List<account>> saveAll(List<account> cuentas) {
            return Uni.createFrom().item(cuentas);
        }

        @Override
        public Uni<Optional<String>> buscarHashPorIdempotencyKey(String idempotencyKey) {
            if ("idem-123".equals(idempotencyKey) && ultimoPayloadHash != null) {
                return Uni.createFrom().item(Optional.of(ultimoPayloadHash));
            }
            return Uni.createFrom().item(Optional.empty());
        }

        @Override
        public Uni<Optional<account>> buscarCuentaPorIdempotencyKey(String idempotencyKey) {
            if ("idem-123".equals(idempotencyKey) && this.cuenta != null) {
                return Uni.createFrom().item(Optional.of(this.cuenta));
            }
            return Uni.createFrom().item(Optional.empty());
        }

        @Override
        public Uni<Optional<account>> buscarCuentaPorNumeroCuentaOInterbancario(String numeroCuenta, String numeroInterbancario) {
            if (this.cuenta == null) {
                return Uni.createFrom().item(Optional.empty());
            }
            if (numeroCuenta.equals(this.cuenta.getNumero_cuenta())
                || numeroInterbancario.equals(this.cuenta.getNumero_interbancario())) {
                return Uni.createFrom().item(Optional.of(this.cuenta));
            }
            return Uni.createFrom().item(Optional.empty());
        }

        @Override
        public Uni<Optional<account>> buscarCuentaParaDeposito(DepositoAccountRequest request) {
            return Uni.createFrom().item(Optional.ofNullable(cuentaDestino != null ? cuentaDestino : cuenta));
        }

        @Override
        public Uni<Optional<account>> depositarMontocuenta(DepositoAccountRequest request,
                BigDecimal montoEnMonedaDeCuenta) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'depositarMontocuenta'");
        }

        @Override
        public Uni<TransactionHistory> transferirEntreCuentas(TransferAccountRequest request, BigDecimal montoOrigen, BigDecimal montoDestino) {
            if (cuentaDestino == null) {
                throw new UnsupportedOperationException("Cuenta destino no configurada");
            }
            cuenta.retiro(montoOrigen);
            cuentaDestino.depositar(montoDestino);
            historialGuardado = new TransactionHistory(
                "001",
                "002",
                montoOrigen,
                request.moneda(),
                "TRANSFERENCIA",
                request.descripcion()
            );
            return Uni.createFrom().item(historialGuardado);
        }
    }
}
