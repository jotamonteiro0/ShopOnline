package model;

public class Cliente {
    private String id;
    private String nome;
    private String email;
    private int pontosFidelidade;
    private double saldoCashback;

    public Cliente(String id, String nome, String email) {
        this(id, nome, email, 0, 0.0);
    }

    public Cliente(String id, String nome, String email, int pontosFidelidade, double saldoCashback) {
        this.id = validarTexto(id, "ID");
        setNome(nome);
        setEmail(email);
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
        this.nome = validarTexto(nome, "Nome");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        email = validarTexto(email, "E-mail");
        if (!email.matches("[^\\s@]+@[^\\s@]+")) {
            throw new IllegalArgumentException("E-mail deve conter usuário e domínio separados por @, sem espaços.");
        }
        this.email = email;
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio.");
        }
        // O CSV compartilhado não possui escape para separadores ou quebras de linha.
        if (valor.contains(";") || valor.contains("\n") || valor.contains("\r")) {
            throw new IllegalArgumentException(campo + " não pode conter ponto e vírgula ou quebra de linha.");
        }
        return valor.strip();
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
