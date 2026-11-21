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
        this(codigo, tipo, valor, dataValidade, Set.of());
    }

    public Cupom(String codigo, TipoCupom tipo, double valor, LocalDate dataValidade, Set<String> clientesUtilizados) {
        if (codigo == null || !codigo.trim().matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Codigo do cupom invalido.");
        }
        if (tipo == null || !Double.isFinite(valor) || valor <= 0
                || (tipo == TipoCupom.PERCENTUAL && valor > 100)) {
            throw new IllegalArgumentException("Valor ou tipo de cupom invalido.");
        }
        this.codigo = codigo.trim().toUpperCase(java.util.Locale.ROOT);
        this.tipo = tipo;
        this.valor = java.math.BigDecimal.valueOf(valor).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
        if (this.valor <= 0) throw new IllegalArgumentException("Desconto deve ser de pelo menos 0,01.");
        this.dataValidade = dataValidade;
        this.clientesUtilizados = new HashSet<>();
        if (clientesUtilizados != null) {
            for (String id : clientesUtilizados) marcarComoUtilizado(id);
        }
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
        return java.util.Collections.unmodifiableSet(clientesUtilizados);
    }

    public boolean foiUtilizadoPor(String clienteId) {
        if (clienteId == null) return false;
        return clientesUtilizados.contains(clienteId);
    }

    public void marcarComoUtilizado(String clienteId) {
        if (clienteId != null && !clienteId.trim().isEmpty()) {
            if (clienteId.contains(";") || clienteId.contains(",") || clienteId.contains("\n") || clienteId.contains("\r")) {
                throw new IllegalArgumentException("ID do cliente invalido para o historico de cupons.");
            }
            clientesUtilizados.add(clienteId);
        }
    }

    @Deprecated
    public boolean isUtilizado() {
        return !clientesUtilizados.isEmpty();
    }

    @Deprecated
    public void marcarComoUtilizado() {
        clientesUtilizados.add("*");
    }

    public boolean isValido() {
        return !clientesUtilizados.contains("*") && (dataValidade == null || !LocalDate.now().isAfter(dataValidade));
    }

    public boolean isValidoParaCliente(String clienteId) {
        if (!isValido()) {
            return false;
        }
        if (clienteId == null || clienteId.isBlank() || foiUtilizadoPor(clienteId)) {
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
