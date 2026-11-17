package service;

import model.Produto;
import repository.ProdutoRepository;

import java.util.List;

/**
 * Servico responsavel pelo controle e monitoramento do inventario e estoque da loja.
 * Aplica as regras de negocio de baixa automatica a cada venda, reposicao preventiva
 * e emissao de alertas quando o estoque restante for menor que 5 unidades.
 */
public class EstoqueService {
    private final ProdutoRepository produtoRepository;

    public EstoqueService(ProdutoRepository produtoRepository) {
        if (produtoRepository == null) {
            throw new IllegalArgumentException("ProdutoRepository nao pode ser nulo.");
        }
        this.produtoRepository = produtoRepository;
    }

    /**
     * Cadastra um novo produto no catalogo da loja com validacao de duplicidade de ID.
     */
    public boolean cadastrarProduto(String id, String nome, String descricao, double preco, int estoqueInicial, boolean promocaoVip) {
        if (produtoRepository.existePorId(id)) {
            return false;
        }
        Produto novoProduto = new Produto(id, nome, descricao, preco, estoqueInicial, promocaoVip);
        produtoRepository.salvar(novoProduto);
        return true;
    }

    /**
     * Executa a baixa automatica no estoque apos venda.
     * Dispara o alerta no console caso o estoque fique abaixo de 5 unidades.
     */
    public boolean darBaixaEstoque(String produtoId, int quantidade) {
        if (quantidade <= 0) {
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

    /**
     * Adiciona unidades ao estoque de um produto existente.
     */
    public boolean reporEstoque(String produtoId, int quantidade) {
        if (quantidade <= 0) {
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

    /**
     * Consulta se um produto possui saldo suficiente para atender uma quantidade solicitada.
     */
    public boolean temEstoqueSuficiente(String produtoId, int quantidade) {
        if (quantidade <= 0) return false;
        Produto produto = produtoRepository.buscarPorId(produtoId);
        return produto != null && produto.getQuantidadeEstoque() >= quantidade;
    }

    public Produto buscarProduto(String id) {
        return produtoRepository.buscarPorId(id);
    }

    public List<Produto> listarTodos() {
        return produtoRepository.listarTodos();
    }

    /**
     * Retorna a lista de produtos cujo estoque esta menor que 5 unidades.
     */
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
