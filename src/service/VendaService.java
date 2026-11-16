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
        double descontoVip = carrinho.calcularDescontoVip(cliente);
        double descontoCupom = carrinho.calcularDescontoCupom();
        double totalIntermediario = subtotal - descontoVip - descontoCupom;

        double valorFinal = formaPagamento.calcularValorFinal(totalIntermediario);
        double descontoPagamento = totalIntermediario - valorFinal;
        double descontoTotal = descontoVip + descontoCupom + Math.max(0, descontoPagamento);

        // Dar baixa no estoque
        for (ItemCarrinho item : carrinho.getItens()) {
            estoqueService.darBaixaEstoque(item.getProduto().getId(), item.getQuantidade());
        }

        // Calcular e processar fidelidade / cashback
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
