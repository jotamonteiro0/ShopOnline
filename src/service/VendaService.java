package service;

import model.*;
import model.pagamento.FormaPagamento;
import repository.ClienteRepository;
import repository.ProdutoRepository;

import java.util.*;

public class VendaService {
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final EstoqueService estoqueService;
    private final FidelidadeService fidelidadeService;
    private final List<Pedido> pedidosRealizados = new ArrayList<>();

    public VendaService(ProdutoRepository produtoRepository,
                        ClienteRepository clienteRepository,
                        EstoqueService estoqueService,
                        FidelidadeService fidelidadeService) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.estoqueService = estoqueService;
        this.fidelidadeService = fidelidadeService;
    }

    public Pedido finalizarPedido(String pedidoId, Cliente cliente, Carrinho carrinho, FormaPagamento formaPagamento) {
        return finalizarPedido(pedidoId, cliente, carrinho, formaPagamento, false);
    }

    public Pedido finalizarPedido(String pedidoId, Cliente cliente, Carrinho carrinho, FormaPagamento formaPagamento, boolean usarCashback) {
        if (carrinho.getItens().isEmpty()) {
            throw new IllegalStateException("O carrinho está vazio.");
        }

        // Validação de estoque
        for (ItemCarrinho item : carrinho.getItens()) {
            Produto p = produtoRepository.buscarPorId(item.getProduto().getId());
            if (p == null || p.getQuantidadeEstoque() < item.getQuantidade()) {
                throw new IllegalStateException("Estoque insuficiente para o produto: " + item.getProduto().getNome());
            }
        }

        double subtotal = carrinho.getSubtotal();
        double valorAntesDoCashback = calcularValorAPagar(cliente, carrinho, formaPagamento);


        for (ItemCarrinho item : carrinho.getItens()) {
            estoqueService.darBaixaEstoque(item.getProduto().getId(), item.getQuantidade());
        }

        double creditoUsado = usarCashback ? fidelidadeService.usarCashback(cliente, valorAntesDoCashback) : 0.0;

        double valorFinal = FidelidadeService.arredondarCentavos(
                valorAntesDoCashback - creditoUsado
        );

        double descontoTotal = FidelidadeService.arredondarCentavos(
                Math.max(0.0, subtotal - valorFinal)
        );

        double cashbackGerado = fidelidadeService.calcularCashback(cliente, valorFinal);
        fidelidadeService.processarFidelidadeECashback(cliente, valorFinal);

        // Marcar cupom como utilizado
        if (carrinho.getCupomAplicado() != null) {
            carrinho.getCupomAplicado().marcarComoUtilizado();
        }

        Pedido pedido = new Pedido(
                pedidoId, cliente, carrinho.getItens(), subtotal,
                descontoTotal, valorFinal, formaPagamento, cashbackGerado
        );

        pedidosRealizados.add(pedido);
        carrinho.limpar();
        return pedido;
    }
    public double calcularValorAPagar(Cliente cliente, Carrinho carrinho, FormaPagamento formaPagamento) {
        double totalComDescontos = carrinho.calcularTotal(cliente);
        double valor = formaPagamento.calcularValorFinal(totalComDescontos);

        if (!Double.isFinite(valor) || valor < 0) {
            throw new IllegalArgumentException("Valor de pagamento inválido.");
        }

        return FidelidadeService.arredondarCentavos(valor);
    }

    public boolean cancelarPedido(String pedidoId) {
        for (Pedido p : pedidosRealizados) {
            if (p.getId().equals(pedidoId)) {
                return p.cancelar();
            }
        }
        return false;
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
        int pedidosValidos = 0;
        Map<String, Long> quantidadePorProduto = new LinkedHashMap<>();
        Map<String, String> nomePorProduto = new LinkedHashMap<>();

        for (Pedido p : pedidosRealizados) {
            if (p.getStatus() != StatusPedido.CANCELADO) {
                receitaTotal += p.getValorFinal();
                for (ItemCarrinho item : p.getItens()) {
                    totalItensVendidos += item.getQuantidade();
                    Produto produto = item.getProduto();
                    String id = produto.getId();

                    quantidadePorProduto.merge(id, (long) item.getQuantidade(), Long::sum);
                    nomePorProduto.putIfAbsent(id, produto.getNome());
                }
                pedidosValidos++;
            }
            System.out.println(p);
        }
        double ticketMedio = pedidosValidos == 0 ? 0.0 : receitaTotal / pedidosValidos;

        System.out.println("--------------------------------------------------");
        System.out.printf("Total de Pedidos Realizados: %d%n", pedidosRealizados.size());
        System.out.printf("Total de Produtos Vendidos: %d unidades%n", totalItensVendidos);
        System.out.printf("Receita Total Acumulada: R$ %.2f%n", receitaTotal);
        System.out.printf("Pedidos válidos: %d%n", pedidosValidos);
        System.out.printf("Ticket médio: R$ %.2f%n", ticketMedio);
        System.out.println("Quantidade vendida por produto:");
        if (quantidadePorProduto.isEmpty()) {
            System.out.println("  Nenhum produto vendido.");
        }
        quantidadePorProduto.forEach((id, quantidade) ->
                System.out.printf("  [%s] %s: %d unidade(s)%n", id, nomePorProduto.get(id), quantidade));
        System.out.println("==================================================");
    }
}
