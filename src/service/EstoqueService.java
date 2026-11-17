package service;

import model.Produto;
import repository.ProdutoRepository;

import java.util.List;

public class EstoqueService {
    private final ProdutoRepository produtoRepository;

    public EstoqueService(ProdutoRepository produtoRepository) {
        if (produtoRepository == null) {
            throw new IllegalArgumentException("ProdutoRepository nao pode ser nulo.");
        }
        this.produtoRepository = produtoRepository;
    }

    public boolean cadastrarProduto(String id, String nome, String descricao, double preco, int estoqueInicial, boolean promocaoVip) {
        if (id == null || id.trim().isEmpty() || produtoRepository.existePorId(id)) {
            return false;
        }
        Produto novoProduto = new Produto(id, nome, descricao, preco, estoqueInicial, promocaoVip);
        produtoRepository.salvar(novoProduto);
        return true;
    }

    // Baixa automatica no estoque e alerta se menor que 5
    public boolean darBaixaEstoque(String produtoId, int quantidade) {
        if (produtoId == null || quantidade <= 0) {
            return false;
        }

        Produto produto = produtoRepository.buscarPorId(produtoId);
        if (produto != null && produto.reduzirEstoque(quantidade)) {
            produtoRepository.salvar(produto);
            if (produto.precisaReposicao()) {
                System.out.printf(">>> [ALERTA DE REPOSICAO] O produto '%s' (ID: %s) esta com estoque baixo: apenas %d unidades restantes!%n",
                        produto.getNome(), produto.getId(), produto.getQuantidadeEstoque());
            }
            return true;
        }
        return false;
    }

    // Reposicao manual de estoque
    public boolean reporEstoque(String produtoId, int quantidade) {
        if (produtoId == null || quantidade <= 0) {
            return false;
        }

        Produto produto = produtoRepository.buscarPorId(produtoId);
        if (produto != null) {
            produto.adicionarEstoque(quantidade);
            produtoRepository.salvar(produto);
            return true;
        }
        return false;
    }

    public boolean temEstoqueSuficiente(String produtoId, int quantidade) {
        if (produtoId == null || quantidade <= 0) return false;
        Produto produto = produtoRepository.buscarPorId(produtoId);
        return produto != null && produto.getQuantidadeEstoque() >= quantidade;
    }

    public Produto buscarProduto(String id) {
        return produtoRepository.buscarPorId(id);
    }

    public List<Produto> listarTodos() {
        return produtoRepository.listarTodos();
    }

    // Retorna produtos com estoque baixo (< 5 unidades)
    public List<Produto> listarProdutosComAlertaReposicao() {
        return produtoRepository.listarTodos().stream()
                .filter(Produto::precisaReposicao)
                .toList();
    }

    public int calcularTotalUnidadesEmEstoque() {
        return produtoRepository.listarTodos().stream()
                .mapToInt(Produto::getQuantidadeEstoque)
                .sum();
    }

    public double calcularValorTotalInventario() {
        return produtoRepository.listarTodos().stream()
                .mapToDouble(p -> p.getPreco() * p.getQuantidadeEstoque())
                .sum();
    }
}
