package teste;

import model.Carrinho;
import model.Cliente;
import model.Pedido;
import model.Produto;
import model.StatusPedido;
import model.pagamento.PagamentoBoleto;
import model.pagamento.PagamentoCartao;
import model.pagamento.PagamentoPix;
import repository.ClienteRepository;
import repository.PedidoRepository;
import repository.ProdutoRepository;
import service.EstoqueService;
import service.FidelidadeService;
import service.PedidoService;
import service.VendaService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Regressao do cancelamento pelo VendaService, com dados isolados em pasta temporaria. */
public class VerificacaoIntegracaoGabriel {
    private static int verificacoes = 0;

    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("shoponline-integracao-gabriel-");
        try {
            Path caminhoClientes = pasta.resolve("clientes.csv");
            Path caminhoProdutos = pasta.resolve("produtos.csv");
            Path caminhoPedidos = pasta.resolve("pedidos.csv");
            ClienteRepository clientes = new ClienteRepository(caminhoClientes.toString());
            ProdutoRepository produtos = new ProdutoRepository(caminhoProdutos.toString());
            Cliente cliente = new Cliente("C01", "Cliente", "cliente@teste.com", 0, 0);
            Produto produto = new Produto("P01", "Produto", "", 100, 10, false);
            clientes.salvar(cliente);
            produtos.salvar(produto);

            EstoqueService estoque = new EstoqueService(produtos);
            PedidoService pedidos = new PedidoService(
                    new PedidoRepository(caminhoPedidos.toString(), clientes, produtos), estoque);
            VendaService vendas = new VendaService(produtos, clientes, estoque,
                    new FidelidadeService(clientes), pedidos);

            Pedido primeiro = vendas.finalizarPedido("PED-01", cliente, carrinho(produto, 2), new PagamentoPix());
            validar(estoque.buscarProduto("P01").getQuantidadeEstoque() == 8, "checkout reduz o estoque");
            validar(vendas.buscarPedido("PED-01") != null, "checkout persiste o pedido");
            validar(Files.readString(caminhoPedidos).contains("PED-01;C01;"), "pedido registrado no CSV");
            validar(Files.readString(pasta.resolve("itens_pedidos.csv")).contains("PED-01;P01;2;100.00"),
                    "quantidades persistidas para posterior estorno");
            validar(Math.abs(primeiro.getValorFinal() - 190.0) < 0.01, "regra do PIX preservada");

            validar(vendas.cancelarPedido("PED-01"), "VendaService cancela pedido em processamento");
            validar(estoque.buscarProduto("P01").getQuantidadeEstoque() == 10, "VendaService devolve estoque");
            validar(primeiro.getStatus() == StatusPedido.CANCELADO, "relatorio da sessao reflete cancelamento");
            validar(vendas.buscarPedido("PED-01").getStatus() == StatusPedido.CANCELADO, "status persistido");
            validar(!vendas.cancelarPedido("PED-01"), "segundo cancelamento recusado");
            validar(estoque.buscarProduto("P01").getQuantidadeEstoque() == 10, "sem estorno duplicado");
            validar(new ProdutoRepository(caminhoProdutos.toString()).buscarPorId("P01").getQuantidadeEstoque() == 10,
                    "devolucao persistida no CSV de produtos");

            vendas.finalizarPedido("PED-02", cliente, carrinho(produto, 1), new PagamentoCartao(10));
            validar(vendas.avancarStatusPedido("PED-02"), "avanca para enviado");
            validar(!vendas.cancelarPedido("PED-02"), "enviado nao pode ser cancelado");
            validar(estoque.buscarProduto("P01").getQuantidadeEstoque() == 9,
                    "cancelamento bloqueado nao altera estoque");
            validar(vendas.avancarStatusPedido("PED-02"), "avanca para entregue");
            validar(!vendas.cancelarPedido("PED-02"), "entregue nao pode ser cancelado");

            Carrinho duplicado = carrinho(produto, 1);
            boolean bloqueouDuplicado = false;
            try {
                vendas.finalizarPedido("PED-02", cliente, duplicado, new PagamentoBoleto());
            } catch (IllegalArgumentException esperado) {
                bloqueouDuplicado = true;
            }
            validar(bloqueouDuplicado, "bloqueia id duplicado");
            validar(estoque.buscarProduto("P01").getQuantidadeEstoque() == 9,
                    "id duplicado nao reduz estoque");
            validar(duplicado.getItens().size() == 1, "falha no checkout preserva carrinho");

            vendas.finalizarPedido("PED-03", cliente, carrinho(produto, 1), new PagamentoBoleto());
            validar(estoque.buscarProduto("P01").getQuantidadeEstoque() == 8, "segundo checkout reduz estoque");
            ProdutoRepository produtosReabertos = new ProdutoRepository(caminhoProdutos.toString());
            ClienteRepository clientesReabertos = new ClienteRepository(caminhoClientes.toString());
            EstoqueService estoqueReaberto = new EstoqueService(produtosReabertos);
            PedidoService pedidosReabertos = new PedidoService(new PedidoRepository(
                    caminhoPedidos.toString(), clientesReabertos, produtosReabertos), estoqueReaberto);
            VendaService vendasReabertas = new VendaService(produtosReabertos, clientesReabertos,
                    estoqueReaberto, new FidelidadeService(clientesReabertos), pedidosReabertos);
            validar(vendasReabertas.cancelarPedido("PED-03"), "cancelamento apos reinicializacao");
            validar(estoqueReaberto.buscarProduto("P01").getQuantidadeEstoque() == 9,
                    "restitui estoque apos reinicio");
            validar(!vendasReabertas.cancelarPedido("PED-03"), "evita estorno duplicado apos reinicio");
            validar(!vendasReabertas.cancelarPedido("INEXISTENTE"), "pedido inexistente nao cancela");
            System.out.println("INTEGRACAO GABRIEL: " + verificacoes + " verificacoes aprovadas.");
        } finally {
            try (var caminhos = Files.walk(pasta)) {
                for (Path arquivo : caminhos.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(arquivo);
                }
            }
        }
    }

    private static Carrinho carrinho(Produto produto, int quantidade) {
        Carrinho carrinho = new Carrinho();
        carrinho.adicionarItem(produto, quantidade);
        return carrinho;
    }

    private static void validar(boolean condicao, String descricao) {
        if (!condicao) throw new AssertionError("FALHOU: " + descricao);
        verificacoes++;
        System.out.println("[OK] " + descricao);
    }
}
