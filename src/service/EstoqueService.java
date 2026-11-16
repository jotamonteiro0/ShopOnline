package service;

import model.Produto;
import repository.ProdutoRepository;
import java.util.List;

public class EstoqueService {
    private final ProdutoRepository produtoRepository;

    public EstoqueService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public boolean darBaixaEstoque(String produtoId, int quantidade) {
        Produto produto = produtoRepository.buscarPorId(produtoId);
        if (produto != null && produto.reduzirEstoque(quantidade)) {
            produtoRepository.salvar(produto);
            if (produto.precisaReposicao()) {
                System.out.println(">>> [ALERTA DE REPOSIÇÃO] Produto '" + produto.getNome() + "' restam apenas " + produto.getQuantidadeEstoque() + " unidades!");
            }
            return true;
        }
        return false;
    }

    public void reporEstoque(String produtoId, int quantidade) {
        Produto produto = produtoRepository.buscarPorId(produtoId);
        if (produto != null) {
            produto.adicionarEstoque(quantidade);
            produtoRepository.salvar(produto);
        }
    }

    public List<Produto> listarProdutosComAlertaReposicao() {
        return produtoRepository.listarTodos().stream()
                .filter(Produto::precisaReposicao)
                .toList();
    }
}
