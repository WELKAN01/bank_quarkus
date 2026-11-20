package org.transaction.account.infrastructure.adapter.out.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Account")
public class AccountEntity {
    @Id 
    @GeneratedValue 
    private Long id;
    private BigDecimal balance;
    private String nombre_dueino;
    @Column(unique = true, nullable = false)
    private String numero_cuenta;
    private String moneda;
    @Column(unique = true, nullable = false)
    private String numero_interbancario;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public BigDecimal getBalance() {
        return balance;
    }
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    public String getNombre_dueino() {
        return nombre_dueino;
    }
    public void setNombre_dueino(String nombre_dueino) {
        this.nombre_dueino = nombre_dueino;
    }
    public String getNumero_cuenta() {
        return numero_cuenta;
    }
    public void setNumero_cuenta(String numero_cuenta) {
        this.numero_cuenta = numero_cuenta;
    }
    public String getNumero_interbancario() {
        return numero_interbancario;
    }
    public void setNumero_interbancario(String numero_interbancario) {
        this.numero_interbancario = numero_interbancario;
    }
    public String getMoneda() {
        return moneda;
    }
    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }


    
}
