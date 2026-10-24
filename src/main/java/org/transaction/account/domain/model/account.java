package org.transaction.account.domain.model;

import java.math.BigDecimal;

public class account {
    private BigDecimal balance;
    private String nombre_dueino;
    private String numero_cuenta;
    private String numero_interbancario;
    private String moneda;

    public account() {
    }

    public account(
        BigDecimal balance,
        String nombreDueino,
        String numeroCuenta,
        String numeroInterbancario,
        String moneda) {
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        }
        this.balance = balance;
        this.nombre_dueino = nombreDueino;
        this.numero_cuenta = numeroCuenta;
        this.moneda = moneda;
        this.numero_interbancario = numeroInterbancario;
    }


    public BigDecimal getBalance() {
        return balance;
    }

    public String getMoneda() {
        return moneda;
    }

    public void retiro(BigDecimal amount){
        if(amount.signum() <= 0){
            throw new IllegalArgumentException(
                "monto no puede ser positivo"
            );
        }

        if(balance.compareTo(amount)<0){
            throw new IllegalArgumentException(
                "salario no puede ser menor de 0"
            );
        }

        balance = balance.subtract(amount);
    }

    public String getNombre_dueino() {
        return nombre_dueino;
    }

    public String getNumero_cuenta() {
        return numero_cuenta;
    }

    public String getNumero_interbancario() {
        return numero_interbancario;
    }

    public void cambiarNombreDueino(String nombreDueino) {
        if (nombreDueino == null || nombreDueino.isBlank()) {
            throw new IllegalArgumentException("El nombre del dueño es obligatorio");
        }
        this.nombre_dueino = nombreDueino.trim();
    }

    public void cambiarMoneda(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            throw new IllegalArgumentException("La moneda es obligatoria");
        }
        this.moneda = moneda.trim().toUpperCase();
    }


    


}
