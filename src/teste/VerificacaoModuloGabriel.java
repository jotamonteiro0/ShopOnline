package teste;

import model.Cliente;
import model.ItemCarrinho;
import model.Pedido;
import model.Produto;
import model.StatusPedido;
import model.pagamento.FormaPagamento;
import model.pagamento.PagamentoBoleto;
import model.pagamento.PagamentoCartao;
import model.pagamento.PagamentoPix;
import repository.ClienteRepository;
import repository.PedidoRepository;
import repository.ProdutoRepository;
import service.EstoqueService;
import service.PedidoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Testes sem bibliotecas externas; nao alteram a pasta data/ do repositorio. */
public class VerificacaoModuloGabriel {
    private static int testes = 0;

    public static void main(String[] args) throws Exception {
        Path pastaTeste = Files.createTempDirectory("shoponline-gabriel-");
        try {
            verificarPagamentos();
            Path caminhoProdutos = pastaTeste.resolve("produtos.csv");
            Path caminhoClientes = pastaTeste.resolve("clientes.csv");
            Path caminhoPedidos = pastaTeste.resolve("pedidos.csv");

            ClienteRepository clientes = new ClienteRepository(caminhoClientes.toString());
            ProdutoRepository produtos = new ProdutoRepository(caminhoProdutos.toString());
            Cliente cliente = new Cliente("C01", "Cliente Teste", "teste@email.com", 0, 0);
            clientes.salvar(cliente);
            Produto produto = new Produto("P01", "Produto Teste", "", 100, 10, false);
            produtos.salvar(produto);
            EstoqueService estoque = new EstoqueService(produtos);
            PedidoRepository repositorio = new PedidoRepository(caminhoPedidos.toString(), clientes, produtos);
            PedidoService pedidos = new PedidoService(repositorio, estoque);

            verificar(Files.exists(caminhoPedidos), "criar pedidos.csv ausente");
            verificar(Files.exists(pastaTeste.resolve("itens_pedidos.csv")), "criar arquivo de itens ausente");
            verificar(estoque.darBaixaEstoque("P01", 2), "efetuar baixa para checkout");
            Pedido pedido1 = criarPedido("PED-01", cliente, produto, 2, new PagamentoPix());
            pedidos.registrarPedido(pedido1);
            verificar(repositorio.existePorId("PED-01"), "registrar pedido");
            verificar(Files.readString(caminhoPedidos).contains("PED-01;C01;"), "gravar historico CSV");
            verificar(Files.readString(pastaTeste.resolve("itens_pedidos.csv")).contains("PED-01;P01;2;100.00"),
                    "gravar itens e quantidades para estorno");

            // Reinstancia os repositorios para simular o reinicio da aplicacao.
            ProdutoRepository produtosReabertos = new ProdutoRepository(caminhoProdutos.toString());
            PedidoRepository repositorioReaberto = new PedidoRepository(caminhoPedidos.toString(),
                    new ClienteRepository(caminhoClientes.toString()), produtosReabertos);
            PedidoService pedidosReabertos = new PedidoService(repositorioReaberto, new EstoqueService(produtosReabertos));
            verificar(pedidosReabertos.buscarPedido("PED-01").getItens().size() == 1,
                    "reconstruir pedido e seus itens apos reiniciar");
            verificar(produtosReabertos.buscarPorId("P01").getQuantidadeEstoque() == 8,
                    "manter baixa em disco antes do cancelamento");
            verificar(pedidosReabertos.cancelarPedido("PED-01"), "cancelar em processamento");
            verificar(produtosReabertos.buscarPorId("P01").getQuantidadeEstoque() == 10,
                    "devolver unidades ao estoque");
            verificar(!pedidosReabertos.cancelarPedido("PED-01"), "impedir cancelamento duplicado");
            verificar(new ProdutoRepository(caminhoProdutos.toString()).buscarPorId("P01").getQuantidadeEstoque() == 10,
                    "persistir estorno no CSV de produtos");
            verificar(new PedidoRepository(caminhoPedidos.toString(),
                    new ClienteRepository(caminhoClientes.toString()),
                    new ProdutoRepository(caminhoProdutos.toString()))
                    .buscarPorId("PED-01").getStatus() == StatusPedido.CANCELADO,
                    "persistir status CANCELADO no CSV");

            Pedido pedido2 = criarPedido("PED-02", cliente, produto, 1, new PagamentoCartao(10));
            pedidosReabertos.registrarPedido(pedido2);
            verificar(pedidosReabertos.avancarStatus("PED-02"), "avancar para ENVIADO");
            verificar(pedidosReabertos.buscarPedido("PED-02").getStatus() == StatusPedido.ENVIADO,
                    "status ENVIADO correto");
            verificar(!pedidosReabertos.cancelarPedido("PED-02"), "bloquear cancelamento de enviado");
            verificar(pedidosReabertos.avancarStatus("PED-02"), "avancar para ENTREGUE");
            verificar(!pedidosReabertos.avancarStatus("PED-02"), "impedir avancar pedido entregue");
            verificar(!pedidosReabertos.cancelarPedido("PED-02"), "bloquear cancelamento de entregue");
            verificar(!pedidosReabertos.avancarStatus("PED-01"), "impedir avancar pedido cancelado");
            verificar(!pedidosReabertos.cancelarPedido("ID-NAO-EXISTENTE"), "ID desconhecido");
            verificar(new PedidoRepository(caminhoPedidos.toString(),
                    new ClienteRepository(caminhoClientes.toString()),
                    new ProdutoRepository(caminhoProdutos.toString()))
                    .buscarPorId("PED-02").getStatus() == StatusPedido.ENTREGUE,
                    "persistir status ENTREGUE ao reiniciar");

            System.out.println("MODULO GABRIEL: " + testes + " verificacoes aprovadas.");
        } finally {
            // O sistema de arquivos de teste e isolado; nao toca os CSVs versionados da equipe.
            try (var arquivos = Files.walk(pastaTeste)) {
                arquivos.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(arquivo -> { try { Files.deleteIfExists(arquivo); } catch (Exception ignorado) { } });
            }
        }
    }

    private static void verificarPagamentos() {
        FormaPagamento pix = new PagamentoPix();
        FormaPagamento cartao = new PagamentoCartao(10);
        FormaPagamento boleto = new PagamentoBoleto();
        verificar(Math.abs(pix.calcularValorFinal(100) - 95) < 0.00001, "PIX 5% de desconto");
        verificar(Math.abs(cartao.calcularValorFinal(100) - 100) < 0.00001, "cartao sem juros");
        verificar(((PagamentoCartao) cartao).getParcelas() == 10, "cartao ate 10 parcelas");
        verificar(Math.abs(boleto.calcularValorFinal(100) - 100) < 0.00001, "boleto sem desconto");
    }

    private static Pedido criarPedido(String id, Cliente cliente, Produto produto, int quantidade,
                                     FormaPagamento pagamento) {
        double subtotal = produto.getPreco() * quantidade;
        double finalPago = pagamento.calcularValorFinal(subtotal);
        return new Pedido(id, cliente, List.of(new ItemCarrinho(produto, quantidade)),
                subtotal, subtotal - finalPago, finalPago, pagamento, 0);
    }

    private static void verificar(boolean condicao, String descricao) {
        if (!condicao) throw new AssertionError("FALHOU: " + descricao);
        testes++;
        System.out.println("[OK] " + descricao);
    }
}
