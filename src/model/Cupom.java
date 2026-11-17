package model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class Cupom {
    private String codigo;
    private TipoCupom tipo;
    private double valor;
    private LocalDate dataValidade;
    private Set<String> clientesUtilizados;

    public Cupom(String codigo, TipoCupom tipo, double valor, LocalDate dataValidade) {
        this.codigo = codigo.trim().toUpperCase();
        this.tipo = tipo;
        this.valor = valor;
        this.dataValidade = dataValidade;
        this.clientesUtilizados = new HashSet<>();
    }

    public Cupom(String codigo, TipoCupom tipo, double valor, LocalDate dataValidade, Set<String> clientesUtilizados) {
        this.codigo = codigo.trim().toUpperCase();
        this.tipo = tipo;
        this.valor = valor;
        this.dataValidade = dataValidade;
        this.clientesUtilizados = clientesUtilizados != null ? new HashSet<>(clientesUtilizados) : new HashSet<>();
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoCupom getTipo() {
        return tipo;
    }

    public double getValor() {
        return valor;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public Set<String> getClientesUtilizados() {
        return clientesUtilizados;
    }

    public boolean foiUtilizadoPor(String clienteId) {
        if (clienteId == null) return false;
        return clientesUtilizados.contains(clienteId.trim().toUpperCase());
    }

    public void marcarComoUtilizado(String clienteId) {
        if (clienteId != null && !clienteId.trim().isEmpty()) {
            clientesUtilizados.add(clienteId.trim().toUpperCase());
        }
    }

    @Deprecated
    public boolean isUtilizado() {
        return !clientesUtilizados.isEmpty();
    }

    @Deprecated
    public void marcarComoUtilizado() {
        // Mantido para retrocompatibilidade, mas idealmente usa-se por clienteId
    }

    public boolean isValido() {
        return dataValidade == null || !LocalDate.now().isAfter(dataValidade);
    }

    public boolean isValidoParaCliente(String clienteId) {
        if (!isValido()) {
            return false;
        }
        if (clienteId != null && foiUtilizadoPor(clienteId)) {
            return false; // Uso único por cliente
        }
        return true;
    }

    public double calcularDesconto(double subtotal) {
        if (!isValido() || subtotal <= 0) {
            return 0.0;
        }
        if (tipo == TipoCupom.PERCENTUAL) {
            return subtotal * (valor / 100.0);
        } else {
            return Math.min(valor, subtotal);
        }
    }
}
