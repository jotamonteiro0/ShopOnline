package model;

import java.time.LocalDate;

public class Cupom {
    private String codigo;
    private TipoCupom tipo;
    private double valor;
    private LocalDate dataValidade;
    private boolean utilizado;

    public Cupom(String codigo, TipoCupom tipo, double valor, LocalDate dataValidade) {
        this.codigo = codigo.toUpperCase();
        this.tipo = tipo;
        this.valor = valor;
        this.dataValidade = dataValidade;
        this.utilizado = false;
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

    public boolean isUtilizado() {
        return utilizado;
    }

    public void marcarComoUtilizado() {
        this.utilizado = true;
    }

    public boolean isValido() {
        return !utilizado && (dataValidade == null || !LocalDate.now().isAfter(dataValidade));
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
