package service;

import model.*;
import model.pagamento.FormaPagamento;
import repository.ClienteRepository;
import repository.PedidoRepository;
import repository.ProdutoRepository;

import java.util.*;

public class VendaService {
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final EstoqueService estoqueService;
    private final FidelidadeService fidelidadeService;
    private final PedidoService pedidoService;
    private final List<Pedido> pedidosRealizados = new ArrayList<>();

    /** Mantem a assinatura usada pelo Main e demais modulos da equipe. */
    public VendaService(ProdutoRepository produtoRepository,
                        ClienteRepository clienteRepository,
                        EstoqueService estoqueService,
                        FidelidadeService fidelidadeService) {
        this(produtoRepository, clienteRepository, estoqueService, fidelidadeService,
                new PedidoService(new PedidoRepository("data/pedidos.csv", clienteRepository, produtoRepository),
                        estoqueService));
    }

    /** Permite compartilhar a mesma instancia com os testes e com o menu interativo. */
    public VendaService(ProdutoRepository produtoRepository,
                        ClienteRepository clienteRepository,
                        EstoqueService estoqueService,
                        FidelidadeService fidelidadeService,
                        PedidoService pedidoService) {
        this.produtoRepository = Objects.requireNonNull(produtoRepository, "Repositorio de produtos obrigatorio.");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "Repositorio de clientes obrigatorio.");
        this.estoqueService = Objects.requireNonNull(estoqueService, "Servico de estoque obrigatorio.");
        this.fidelidadeService = Objects.requireNonNull(fidelidadeService, "Servico de fidelidade obrigatorio.");
        this.pedidoService = Objects.requireNonNull(pedidoService, "Servico de pedidos obrigatorio.");
    }

    public Pedido finalizarPedido(String pedidoId, Cliente cliente, Carrinho carrinho, FormaPagamento formaPagamento) {
        Objects.requireNonNull(cliente, "Cliente obrigatorio.");
        Objects.requireNonNull(carrinho, "Carrinho obrigatorio.");
        Objects.requireNonNull(formaPagamento, "Pagamento obrigatorio.");
        if (carrinho.getItens().isEmpty()) {
            throw new IllegalStateException("O carrinho esta vazio.");
        }
        // Validar antes de alterar estoque, cashback, cupom ou carrinho.
        if (pedidoService.buscarPedido(pedidoId) != null) {
            throw new IllegalArgumentException("Ja existe um pedido com ID " + pedidoId);
        }
        for (ItemCarrinho item : carrinho.getItens()) {
            Produto produto = produtoRepository.buscarPorId(item.getProduto().getId());
            if (produto == null || item.getQuantidade() <= 0
                    || produto.getQuantidadeEstoque() < item.getQuantidade()) {
                throw new IllegalStateException("Estoque insuficiente para o produto: " + item.getProduto().getNome());
            }
        }

        double subtotal = carrinho.getSubtotal();
        double descontoVip = carrinho.calcularDescontoVip(cliente);
        double descontoCupom = carrinho.calcularDescontoCupom();
        double totalIntermediario = Math.max(0.0, subtotal - descontoVip - descontoCupom);
        double valorFinal = Math.max(0.0, formaPagamento.calcularValorFinal(totalIntermediario));
        double descontoTotal = Math.max(0.0, subtotal - valorFinal);
        double cashbackGerado = fidelidadeService.calcularCashback(cliente, valorFinal);

        Pedido pedido = new Pedido(pedidoId, cliente, carrinho.getItens(), subtotal,
                descontoTotal, valorFinal, formaPagamento, cashbackGerado);

        // Se a baixa ou o registro falhar, tenta recompor as unidades ja baixadas.
        List<ItemCarrinho> baixados = new ArrayList<>();
        try {
            for (ItemCarrinho item : carrinho.getItens()) {
                if (!estoqueService.darBaixaEstoque(item.getProduto().getId(), item.getQuantidade())) {
                    throw new IllegalStateException("Falha na baixa do estoque: " + item.getProduto().getId());
                }
                baixados.add(item);
            }
            pedidoService.registrarPedido(pedido);
        } catch (RuntimeException falha) {
            for (int i = baixados.size() - 1; i >= 0; i--) {
                ItemCarrinho item = baixados.get(i);
                if (!estoqueService.reporEstoque(item.getProduto().getId(), item.getQuantidade())) {
                    falha.addSuppressed(new IllegalStateException(
                            "Falha na recomposicao do estoque: " + item.getProduto().getId()));
                }
            }
            throw falha;
        }

        fidelidadeService.processarFidelidadeECashback(cliente, valorFinal);
        if (carrinho.getCupomAplicado() != null) {
            carrinho.getCupomAplicado().marcarComoUtilizado();
        }
        pedidosRealizados.add(pedido);
        carrinho.limpar();
        return pedido;
    }

    /** O estorno deve ocorrer sempre pelo PedidoService, nunca diretamente pelo modelo. */
    public boolean cancelarPedido(String pedidoId) {
        boolean cancelado = pedidoService.cancelarPedido(pedidoId);
        if (cancelado) {
            // Sincroniza tambem o objeto mantido no relatorio de vendas da sessao.
            for (Pedido pedido : pedidosRealizados) {
                if (pedido.getId().equals(pedidoId)) pedido.cancelar();
            }
        }
        return cancelado;
    }

    public boolean avancarStatusPedido(String pedidoId) {
        if (!pedidoService.avancarStatus(pedidoId)) return false;
        Pedido atualizado = pedidoService.buscarPedido(pedidoId);
        for (Pedido pedido : pedidosRealizados) {
            if (pedido.getId().equals(pedidoId)) {
                while (pedido.getStatus() != atualizado.getStatus() && pedido.avancarStatus()) {
                    // Atualiza o status do relatorio em memoria sem duplicar a persistencia.
                }
            }
        }
        return true;
    }

    public Pedido buscarPedido(String pedidoId) { return pedidoService.buscarPedido(pedidoId); }

    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    public List<Pedido> getPedidosRealizados() {
        return Collections.unmodifiableList(pedidosRealizados);
    }

    public void exibirRelatorioVendas() {
        System.out.println("==================================================");
        System.out.println("            RELATÓRIO GERAL DE VENDAS             ");
        System.out.println("==================================================");
        double receitaTotal = 0;
        int totalItensVendidos = 0;

        for (Pedido p : pedidosRealizados) {
            if (p.getStatus() != StatusPedido.CANCELADO) {
                receitaTotal += p.getValorFinal();
                for (ItemCarrinho item : p.getItens()) {
                    totalItensVendidos += item.getQuantidade();
                }
            }
            System.out.println(p);
        }

        System.out.println("--------------------------------------------------");
        System.out.printf("Total de Pedidos Realizados: %d%n", pedidosRealizados.size());
        System.out.printf("Total de Produtos Vendidos: %d unidades%n", totalItensVendidos);
        System.out.printf("Receita Total Acumulada: R$ %.2f%n", receitaTotal);
        System.out.println("==================================================");
    }
}
