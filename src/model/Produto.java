package model;

import java.util.Objects;

/**
 * Representa um produto comercializado pela loja virtual ShopOnline.
 * Encapsula identificador, nome, descricao detalhada, preco de venda,
 * quantidade em estoque e participacao em promocao para clientes VIP.
 */
public class Produto {
    private String id;
    private String nome;
    private String descricao;
    private double preco;
    private int quantidadeEstoque;
    private boolean promocaoVip;

    public Produto(String id, String nome, String descricao, double preco, int quantidadeEstoque, boolean promocaoVip) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador do produto nao pode ser nulo ou vazio.");
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do produto nao pode ser nulo ou vazio.");
        }
        if (preco < 0.0) {
            throw new IllegalArgumentException("O preco do produto nao pode ser negativo.");
        }
        if (quantidadeEstoque < 0) {
            throw new IllegalArgumentException("A quantidade inicial em estoque nao pode ser negativa.");
        }

        this.id = id.trim().toUpperCase();
        this.nome = nome.trim();
        this.descricao = (descricao != null) ? descricao.trim() : "";
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
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do produto nao pode ser vazio.");
        }
        this.nome = nome.trim();
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = (descricao != null) ? descricao.trim() : "";
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        if (preco < 0.0) {
            throw new IllegalArgumentException("O preco nao pode ser negativo.");
        }
        this.preco = preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(int quantidadeEstoque) {
        if (quantidadeEstoque < 0) {
            throw new IllegalArgumentException("O estoque nao pode ser negativo.");
        }
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public boolean isPromocaoVip() {
        return promocaoVip;
    }

    public void setPromocaoVip(boolean promocaoVip) {
        this.promocaoVip = promocaoVip;
    }

    /**
     * Regra do enunciado: dispara alerta preventivo quando o estoque restante for menor que 5 unidades.
     */
    public boolean precisaReposicao() {
        return this.quantidadeEstoque < 5;
    }

    /**
     * Realiza a baixa automatica no estoque apos venda confirmada.
     * Impede qualquer tentativa de deixar o estoque negativo.
     *
     * @param quantidade Unidades a serem reduzidas
     * @return true se a baixa foi efetuada com sucesso; false se o saldo for insuficiente
     */
    public boolean reduzirEstoque(int quantidade) {
        if (quantidade > 0 && this.quantidadeEstoque >= quantidade) {
            this.quantidadeEstoque -= quantidade;
            return true;
        }
        return false;
    }

    /**
     * Adiciona unidades ao inventario do produto (reposicao de fornecedor ou cancelamento seguro).
     *
     * @param quantidade Unidades a serem adicionadas
     */
    public void adicionarEstoque(int quantidade) {
        if (quantidade > 0) {
            this.quantidadeEstoque += quantidade;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Produto produto = (Produto) o;
        return Objects.equals(id, produto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %-25s | R$ %8.2f | Estoque: %3d un. %s%s",
                id, nome, preco, quantidadeEstoque,
                precisaReposicao() ? "[ALERTA: REPOSICAO NECESSARIA]" : "[ESTOQUE OK]",
                promocaoVip ? " [PROMO VIP]" : "");
    }
}
