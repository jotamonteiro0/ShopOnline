package model;

public class Cliente {
    private String id;
    private String nome;
    private String email;
    private int pontosFidelidade;
    private double saldoCashback;

    public Cliente(String id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.pontosFidelidade = 0;
        this.saldoCashback = 0.0;
    }

    public Cliente(String id, String nome, String email, int pontosFidelidade, double saldoCashback) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.pontosFidelidade = pontosFidelidade;
        this.saldoCashback = saldoCashback;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getPontosFidelidade() {
        return pontosFidelidade;
    }

    public void adicionarPontos(int pontos) {
        if (pontos > 0) {
            this.pontosFidelidade += pontos;
        }
    }

    public double getSaldoCashback() {
        return saldoCashback;
    }

    public void creditarCashback(double valor) {
        if (valor > 0) {
            this.saldoCashback += valor;
        }
    }

    public boolean debitarCashback(double valor) {
        if (valor > 0 && this.saldoCashback >= valor) {
            this.saldoCashback -= valor;
            return true;
        }
        return false;
    }

    public NivelFidelidade getNivelFidelidade() {
        return NivelFidelidade.obterPorPontos(this.pontosFidelidade);
    }

    public boolean isVip() {
        return false;
    }

    public double calcularDescontoVip(Produto produto) {
        return 0.0;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - Nível: %s | Pontos: %d | Saldo Cashback: R$ %.2f",
                isVip() ? "VIP" : "COMUM", nome, email, getNivelFidelidade(), pontosFidelidade, saldoCashback);
    }
}
