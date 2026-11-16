package model;

public class Produto {
    private String id;
    private String nome;
    private String descricao;
    private double preco;
    private int quantidadeEstoque;
    private boolean promocaoVip;

    public Produto(String id, String nome, String descricao, double preco, int quantidadeEstoque, boolean promocaoVip) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.promocaoVip = promocaoVip;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(int quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public boolean isPromocaoVip() {
        return promocaoVip;
    }

    public void setPromocaoVip(boolean promocaoVip) {
        this.promocaoVip = promocaoVip;
    }

    public boolean precisaReposicao() {
        return this.quantidadeEstoque < 5;
    }

    public boolean reduzirEstoque(int quantidade) {
        if (quantidade > 0 && this.quantidadeEstoque >= quantidade) {
            this.quantidadeEstoque -= quantidade;
            return true;
        }
        return false;
    }

    public void adicionarEstoque(int quantidade) {
        if (quantidade > 0) {
            this.quantidadeEstoque += quantidade;
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - R$ %.2f (Estoque: %d%s%s)",
                id, nome, preco, quantidadeEstoque,
                precisaReposicao() ? " [ALERTA: REPOSIÇÃO NECESSÁRIA]" : "",
                promocaoVip ? " [PROMOÇÃO VIP]" : "");
    }
}
