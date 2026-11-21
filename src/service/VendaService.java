package service;

import model.*;
import model.pagamento.FormaPagamento;
import repository.ClienteRepository;
import repository.CupomRepository;
import repository.PedidoRepository;
import repository.ProdutoRepository;

import java.util.*;

public class VendaService {
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final EstoqueService estoqueService;
    private final FidelidadeService fidelidadeService;
    private final PedidoService pedidoService;
    private final CupomRepository cupomRepository;
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
        this(produtoRepository, clienteRepository, estoqueService, fidelidadeService, pedidoService,
                new CupomRepository(clienteRepository.getCaminhoArquivo().resolveSibling("cupons.csv").toString()));
    }

    public VendaService(ProdutoRepository produtoRepository,
                        ClienteRepository clienteRepository,
                        EstoqueService estoqueService,
                        FidelidadeService fidelidadeService,
                        PedidoService pedidoService,
                        CupomRepository cupomRepository) {
        this.produtoRepository = Objects.requireNonNull(produtoRepository, "Repositorio de produtos obrigatorio.");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "Repositorio de clientes obrigatorio.");
        this.estoqueService = Objects.requireNonNull(estoqueService, "Servico de estoque obrigatorio.");
        this.fidelidadeService = Objects.requireNonNull(fidelidadeService, "Servico de fidelidade obrigatorio.");
        this.pedidoService = Objects.requireNonNull(pedidoService, "Servico de pedidos obrigatorio.");
        this.cupomRepository = Objects.requireNonNull(cupomRepository, "Repositorio de cupons obrigatorio.");
        pedidosRealizados.addAll(pedidoService.listarPedidos());
    }

    public Pedido finalizarPedido(String pedidoId, Cliente cliente, Carrinho carrinho, FormaPagamento formaPagamento) {
        return finalizarPedido(pedidoId, cliente, carrinho, formaPagamento, false);
    }

    public Pedido finalizarPedido(String pedidoId, Cliente cliente, Carrinho carrinho,
                                 FormaPagamento formaPagamento, boolean usarCashback) {
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

        Cupom aplicado = carrinho.getCupomAplicado();
        Cupom anterior = aplicado == null ? null : cupomRepository.buscarPorCodigo(aplicado.getCodigo());
        if (aplicado != null && (!aplicado.isValidoParaCliente(cliente.getId())
                || (anterior != null && !anterior.isValidoParaCliente(cliente.getId())))) {
            throw new IllegalStateException("Cupom expirado ou ja utilizado por este cliente.");
        }
        if (anterior != null && (aplicado.getTipo() != anterior.getTipo()
                || Double.compare(aplicado.getValor(), anterior.getValor()) != 0
                || !Objects.equals(aplicado.getDataValidade(), anterior.getDataValidade()))) {
            throw new IllegalArgumentException("Cupom diferente do cadastro. Aplique novamente pelo codigo.");
        }
        double subtotal = carrinho.getSubtotal();
        double valorAntesDoCashback = calcularValorAPagar(cliente, carrinho, formaPagamento);
        double creditoUsado = usarCashback
                ? fidelidadeService.calcularResgateCashback(cliente, valorAntesDoCashback) : 0;
        double valorFinal = FidelidadeService.arredondarCentavos(valorAntesDoCashback - creditoUsado);
        double descontoTotal = FidelidadeService.arredondarCentavos(Math.max(0, subtotal - valorFinal));
        double cashbackGerado = fidelidadeService.calcularCashback(cliente, valorFinal);

        Pedido pedido = new Pedido(pedidoId, cliente, carrinho.getItens(), subtotal,
                descontoTotal, valorFinal, formaPagamento, cashbackGerado);

        // Se a baixa ou o registro falhar, tenta recompor as unidades ja baixadas.
        List<ItemCarrinho> baixados = new ArrayList<>();
        int pontosAnteriores = cliente.getPontosFidelidade();
        double saldoAnterior = cliente.getSaldoCashback();
        boolean clienteAlterado = false;
        boolean cupomSalvo = false;
        try {
            for (ItemCarrinho item : carrinho.getItens()) {
                if (!estoqueService.darBaixaEstoque(item.getProduto().getId(), item.getQuantidade())) {
                    throw new IllegalStateException("Falha na baixa do estoque: " + item.getProduto().getId());
                }
                baixados.add(item);
            }
            clienteAlterado = true;
            if (usarCashback) fidelidadeService.usarCashback(cliente, valorAntesDoCashback);
            fidelidadeService.processarFidelidadeECashback(cliente, valorFinal);
            if (aplicado != null) {
                Cupom utilizado = new Cupom(aplicado.getCodigo(), aplicado.getTipo(), aplicado.getValor(),
                        aplicado.getDataValidade(), anterior == null ? aplicado.getClientesUtilizados() : anterior.getClientesUtilizados());
                utilizado.marcarComoUtilizado(cliente.getId());
                cupomRepository.salvar(utilizado);
                cupomSalvo = true;
            }
            pedidoService.registrarPedido(pedido);
        } catch (RuntimeException falha) {
            if (cupomSalvo) {
                try {
                    if (anterior == null) cupomRepository.removerPorId(aplicado.getCodigo());
                    else cupomRepository.salvar(anterior);
                } catch (RuntimeException erro) { falha.addSuppressed(erro); }
            }
            if (clienteAlterado) {
                cliente.atualizarFidelidade(pontosAnteriores, saldoAnterior);
                try { clienteRepository.salvar(cliente); }
                catch (RuntimeException erro) { falha.addSuppressed(erro); }
            }
            for (int i = baixados.size() - 1; i >= 0; i--) {
                ItemCarrinho item = baixados.get(i);
                try {
                    if (!estoqueService.reporEstoque(item.getProduto().getId(), item.getQuantidade())) {
                        falha.addSuppressed(new IllegalStateException("Falha na recomposicao do estoque."));
                    }
                } catch (RuntimeException erro) { falha.addSuppressed(erro); }
            }
            throw falha;
        }

        if (aplicado != null) aplicado.marcarComoUtilizado(cliente.getId());
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

    public CupomRepository getCupomRepository() { return cupomRepository; }

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
        int pedidosValidos = 0;
        Map<String, Long> quantidadePorProduto = new LinkedHashMap<>();
        Map<String, String> nomePorProduto = new LinkedHashMap<>();

        List<Pedido> historico = listarPedidos();
        for (Pedido p : historico) {
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
        System.out.printf("Total de Pedidos Realizados: %d%n", historico.size());
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
