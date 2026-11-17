package view;

import model.*;
import model.pagamento.*;
import repository.ClienteRepository;
import repository.CupomRepository;
import repository.ProdutoRepository;
import service.EstoqueService;
import service.FidelidadeService;
import service.VendaService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MenuCarrinho {
    private final ProdutoRepository produtoRepo;
    private final ClienteRepository clienteRepo;
    private final CupomRepository cupomRepo;
    private final EstoqueService estoqueService;
    private final VendaService vendaService;
    private final Carrinho carrinho;
    private Cliente clienteAtual;

    public MenuCarrinho(ProdutoRepository produtoRepo,
                        ClienteRepository clienteRepo,
                        CupomRepository cupomRepo,
                        EstoqueService estoqueService,
                        VendaService vendaService) {
        this.produtoRepo = produtoRepo;
        this.clienteRepo = clienteRepo;
        this.cupomRepo = cupomRepo;
        this.estoqueService = estoqueService;
        this.vendaService = vendaService;
        this.carrinho = new Carrinho();
        
        // Selecionar um cliente padrão se houver
        List<Cliente> clientes = clienteRepo.listarTodos();
        if (!clientes.isEmpty()) {
            this.clienteAtual = clientes.get(0);
        }
    }

    public void exibirMenu() {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n==================================================");
            System.out.println("      MODULO: CARRINHO, CUPONS E PROMOCOES      ");
            System.out.println("            Responsavel: Lucas Silveira (dev_lucas)");
            System.out.println("==================================================");
            System.out.printf("Cliente Ativo: %s%n", clienteAtual != null ? clienteAtual.getNome() + " (ID: " + clienteAtual.getId() + (clienteAtual.isVip() ? " - VIP" : "") + ")" : "Nenhum selecionado");
            System.out.printf("Subtotal Atual: R$ %.2f | Total Final: R$ %.2f%n", carrinho.getSubtotal(), carrinho.calcularTotal(clienteAtual));
            System.out.println("--------------------------------------------------");
            System.out.println("1. Exibir Carrinho e Totalizadores");
            System.out.println("2. Adicionar Produto ao Carrinho");
            System.out.println("3. Remover Produto do Carrinho");
            System.out.println("4. Limpar Carrinho");
            System.out.println("5. Selecionar Cliente Ativo");
            System.out.println("6. Gerenciar / Aplicar Cupom de Desconto");
            System.out.println("7. Finalizar Compra (Checkout)");
            System.out.println("0. Voltar ao Menu Principal");
            System.out.println("--------------------------------------------------");

            int opcao = Entrada.lerInteiro("Selecione uma opcao: ", 0, 7);
            switch (opcao) {
                case 1 -> exibirDetalhesCarrinho();
                case 2 -> adicionarProdutoAoCarrinho();
                case 3 -> removerProdutoDoCarrinho();
                case 4 -> {
                    carrinho.limpar();
                    System.out.println("[Sucesso] Carrinho limpo. Todos os valores retornaram a zero.");
                    Entrada.aguardarEnter();
                }
                case 5 -> selecionarClienteAtivo();
                case 6 -> gerenciarCupons();
                case 7 -> finalizarCompra();
                case 0 -> rodando = false;
            }
        }
    }

    private void exibirDetalhesCarrinho() {
        System.out.println("\n==================================================");
        System.out.println("                ITENS NO CARRINHO                 ");
        System.out.println("==================================================");
        List<ItemCarrinho> itens = carrinho.getItens();
        if (itens.isEmpty()) {
            System.out.println("O carrinho esta vazio.");
        } else {
            for (int i = 0; i < itens.size(); i++) {
                ItemCarrinho item = itens.get(i);
                System.out.printf("[%d] %s (Estoque disp: %d) — Qtd: %d — Preco Unit: R$ %.2f — Subtotal: R$ %.2f%n",
                        i + 1, item.getProduto().getNome(), item.getProduto().getQuantidadeEstoque(),
                        item.getQuantidade(), item.getProduto().getPreco(), item.getSubtotal());
            }
        }
        System.out.println("--------------------------------------------------");
        double subtotal = carrinho.getSubtotal();
        double descontoVip = carrinho.calcularDescontoVip(clienteAtual);
        double descontoCupom = carrinho.calcularDescontoCupom();
        double total = carrinho.calcularTotal(clienteAtual);

        System.out.printf("Subtotal:       R$ %.2f%n", subtotal);
        System.out.printf("Desconto VIP:   R$ %.2f%n", descontoVip);
        Cupom cupom = carrinho.getCupomAplicado();
        String cupomInfo = cupom != null ? String.format(" [%s]", cupom.getCodigo()) : "";
        System.out.printf("Desconto Cupom: R$ %.2f%s%n", descontoCupom, cupomInfo);
        System.out.printf("TOTAL FINAL:    R$ %.2f%n", total);
        System.out.println("==================================================");
        Entrada.aguardarEnter();
    }

    private void adicionarProdutoAoCarrinho() {
        System.out.println("\n--- Adicionar Produto ao Carrinho ---");
        List<Produto> produtos = produtoRepo.listarTodos();
        if (produtos.isEmpty()) {
            System.out.println("[Aviso] Nenhum produto cadastrado no catalogo.");
            return;
        }

        for (Produto p : produtos) {
            System.out.printf("ID: %s | Nome: %s | Preco: R$ %.2f | Estoque: %d%s%n",
                    p.getId(), p.getNome(), p.getPreco(), p.getQuantidadeEstoque(),
                    p.isPromocaoVip() ? " [PROMOCAO VIP]" : "");
        }

        String id = Entrada.lerTexto("\nDigite o ID do produto: ", true);
        Produto produto = produtoRepo.buscarPorId(id);
        if (produto == null) {
            System.out.println("[Erro] Produto nao encontrado.");
            return;
        }

        int quantidade = Entrada.lerInteiro("Digite a quantidade desejada: ", 1, 10000);
        
        int qtdExistenteNoCarrinho = 0;
        for (ItemCarrinho item : carrinho.getItens()) {
            if (item.getProduto().getId().equalsIgnoreCase(produto.getId())) {
                qtdExistenteNoCarrinho = item.getQuantidade();
                break;
            }
        }

        int qtdTotalDesejada = qtdExistenteNoCarrinho + quantidade;
        if (produto.getQuantidadeEstoque() < qtdTotalDesejada) {
            System.out.printf("[Erro] Estoque insuficiente! Ja existem %d unidades no carrinho. Disponivel em estoque: %d unidades.%n",
                    qtdExistenteNoCarrinho, produto.getQuantidadeEstoque());
            Entrada.aguardarEnter();
            return;
        }

        carrinho.adicionarItem(produto, quantidade);
        System.out.printf("[Sucesso] %d unidade(s) de '%s' adicionada(s) ao carrinho!%n", quantidade, produto.getNome());
        System.out.printf("[Atualizacao] Subtotal atual do carrinho: R$ %.2f | Total Final: R$ %.2f%n",
                carrinho.getSubtotal(), carrinho.calcularTotal(clienteAtual));
        Entrada.aguardarEnter();
    }

    private void removerProdutoDoCarrinho() {
        if (carrinho.getItens().isEmpty()) {
            System.out.println("[Aviso] O carrinho esta vazio.");
            Entrada.aguardarEnter();
            return;
        }
        System.out.println("\n--- Remover Produto do Carrinho ---");
        for (ItemCarrinho item : carrinho.getItens()) {
            System.out.printf("ID: %s | Nome: %s | Qtd no carrinho: %d — Subtotal: R$ %.2f%n",
                    item.getProduto().getId(), item.getProduto().getNome(), item.getQuantidade(), item.getSubtotal());
        }
        String id = Entrada.lerTexto("Digite o ID do produto que deseja remover: ", true);
        
        ItemCarrinho itemAlvo = null;
        for (ItemCarrinho item : carrinho.getItens()) {
            if (item.getProduto().getId().equalsIgnoreCase(id)) {
                itemAlvo = item;
                break;
            }
        }

        if (itemAlvo == null) {
            System.out.println("[Aviso] Produto nao encontrado no carrinho.");
            Entrada.aguardarEnter();
            return;
        }

        int qtdAtual = itemAlvo.getQuantidade();
        int qtdRemover = Entrada.lerInteiro(String.format("Quantas unidades deseja remover? (Disponiveis: %d): ", qtdAtual), 1, qtdAtual);

        boolean removido = carrinho.removerQuantidadeItem(id, qtdRemover);
        if (removido) {
            System.out.printf("[Sucesso] %d unidade(s) removida(s) do carrinho!%n", qtdRemover);
            System.out.printf("[Atualizacao] Subtotal atual do carrinho: R$ %.2f | Total Final: R$ %.2f%n",
                    carrinho.getSubtotal(), carrinho.calcularTotal(clienteAtual));
        } else {
            System.out.println("[Erro] Nao foi possivel remover a quantidade solicitada.");
        }
        Entrada.aguardarEnter();
    }

    private void selecionarClienteAtivo() {
        System.out.println("\n--- Selecionar Cliente Ativo ---");
        List<Cliente> clientes = clienteRepo.listarTodos();
        if (clientes.isEmpty()) {
            System.out.println("[Aviso] Nenhum cliente cadastrado.");
            return;
        }
        for (Cliente c : clientes) {
            System.out.printf("ID: %s | Nome: %s | VIP: %b | Cashback: R$ %.2f%n",
                    c.getId(), c.getNome(), c.isVip(), c.getSaldoCashback());
        }
        String id = Entrada.lerTexto("Digite o ID do cliente: ", true);
        Cliente cliente = clienteRepo.buscarPorId(id);
        if (cliente != null) {
            this.clienteAtual = cliente;
            System.out.printf("[Sucesso] Cliente ativo alterado para: %s%n", cliente.getNome());
            System.out.printf("[Atualizacao] Total Final recalculatedo para o cliente: R$ %.2f%n", carrinho.calcularTotal(clienteAtual));
        } else {
            System.out.println("[Erro] Cliente nao encontrado.");
        }
        Entrada.aguardarEnter();
    }

    private void gerenciarCupons() {
        System.out.println("\n--- Gerenciar e Aplicar Cupons ---");
        System.out.println("1. Listar Cupons Disponiveis");
        System.out.println("2. Cadastrar Novo Cupom");
        System.out.println("3. Aplicar Cupom ao Carrinho");
        System.out.println("4. Remover Cupom do Carrinho");
        System.out.println("0. Voltar");

        int op = Entrada.lerInteiro("Escolha uma opcao: ", 0, 4);
        switch (op) {
            case 1 -> {
                List<Cupom> cupons = cupomRepo.listarTodos();
                if (cupons.isEmpty()) {
                    System.out.println("Nenhum cupom cadastrado.");
                } else {
                    for (Cupom c : cupons) {
                        boolean validoCliente = clienteAtual != null && c.isValidoParaCliente(clienteAtual.getId());
                        System.out.printf("Codigo: %s | Tipo: %s | Valor: %.2f | Validade: %s | Valido: %b%n",
                                c.getCodigo(), c.getTipo(), c.getValor(), c.getDataValidade(), validoCliente);
                    }
                }
                Entrada.aguardarEnter();
            }
            case 2 -> cadastrarCupom();
            case 3 -> aplicarCupom();
            case 4 -> {
                carrinho.aplicarCupom(null, null);
                System.out.println("[Sucesso] Cupom removido do carrinho!");
                System.out.printf("[Atualizacao] Subtotal: R$ %.2f | Total Final recalculado: R$ %.2f%n",
                        carrinho.getSubtotal(), carrinho.calcularTotal(clienteAtual));
                Entrada.aguardarEnter();
            }
        }
    }

    private void cadastrarCupom() {
        System.out.println("\n--- Cadastrar Novo Cupom ---");
        String codigo = Entrada.lerTexto("Codigo do cupom (ex: PROMO10): ", true);
        if (cupomRepo.existePorId(codigo)) {
            System.out.println("[Erro] Ja existe um cupom com esse codigo.");
            return;
        }

        System.out.println("Tipos de Cupom: 1. PERCENTUAL, 2. VALOR_FIXO");
        int tipoOp = Entrada.lerInteiro("Escolha o tipo: ", 1, 2);
        TipoCupom tipo = tipoOp == 1 ? TipoCupom.PERCENTUAL : TipoCupom.VALOR_FIXO;

        double valor = Entrada.lerDouble("Valor do desconto (ex: 15.0 para 15% ou R$ 15): ", 0.0);
        String dataStr = Entrada.lerTexto("Data de validade (AAAA-MM-DD) ou deixe em branco para sem validade: ", false);
        LocalDate dataValidade = null;
        if (!dataStr.trim().isEmpty()) {
            try {
                dataValidade = LocalDate.parse(dataStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                System.out.println("[Aviso] Data invalida. Cupom criado sem validade temporal.");
            }
        }

        Cupom cupom = new Cupom(codigo, tipo, valor, dataValidade);
        cupomRepo.salvar(cupom);
        System.out.printf("[Sucesso] Cupom '%s' cadastrado com sucesso!%n", cupom.getCodigo());
        Entrada.aguardarEnter();
    }

    private void aplicarCupom() {
        if (clienteAtual == null) {
            System.out.println("[Erro] Selecione um cliente ativo antes de aplicar um cupom.");
            return;
        }
        String codigo = Entrada.lerTexto("Digite o codigo do cupom: ", true);
        Cupom cupom = cupomRepo.buscarPorCodigo(codigo);
        if (cupom == null) {
            System.out.println("[Erro] Cupom nao encontrado.");
            return;
        }

        if (!cupom.isValido()) {
            System.out.println("[Erro] Este cupom esta expirado.");
            return;
        }

        if (cupom.foiUtilizadoPor(clienteAtual.getId())) {
            System.out.println("[Erro] Voce (cliente " + clienteAtual.getNome() + ") ja utilizou este cupom anteriormente. Uso unico por cliente!");
            return;
        }

        boolean aplicado = carrinho.aplicarCupom(cupom, clienteAtual);
        if (aplicado) {
            System.out.printf("[Sucesso] Cupom '%s' aplicado com sucesso! Desconto: R$ %.2f%n",
                    cupom.getCodigo(), carrinho.calcularDescontoCupom());
            System.out.printf("[Atualizacao] Subtotal: R$ %.2f | Total Final recalculado: R$ %.2f%n",
                    carrinho.getSubtotal(), carrinho.calcularTotal(clienteAtual));
        } else {
            System.out.println("[Erro] Nao foi possivel aplicar o cupom.");
        }
        Entrada.aguardarEnter();
    }

    private void finalizarCompra() {
        if (carrinho.getItens().isEmpty()) {
            System.out.println("[Erro] O carrinho esta vazio.");
            return;
        }
        if (clienteAtual == null) {
            System.out.println("[Erro] Nenhum cliente ativo selecionado.");
            return;
        }

        System.out.println("\n--- Escolha a Forma de Pagamento ---");
        System.out.println("1. PIX (5% de desconto)");
        System.out.println("2. Cartao de Credito (Parcelado sem juros)");
        System.out.println("3. Boleto Bancario");
        int op = Entrada.lerInteiro("Opcao: ", 1, 3);

        FormaPagamento formaPagamento = switch (op) {
            case 1 -> new PagamentoPix();
            case 2 -> {
                int parcelas = Entrada.lerInteiro("Numero de parcelas (1 a 10): ", 1, 10);
                yield new PagamentoCartao(parcelas);
            }
            default -> new PagamentoBoleto();
        };

        try {
            String pedidoId = "PED-" + (System.currentTimeMillis() % 100000);
            
            // Marcar cupom como utilizado pelo cliente antes de finalizar
            Cupom cupomAplicado = carrinho.getCupomAplicado();
            if (cupomAplicado != null) {
                cupomAplicado.marcarComoUtilizado(clienteAtual.getId());
                cupomRepo.salvar(cupomAplicado);
            }

            Pedido pedido = vendaService.finalizarPedido(pedidoId, clienteAtual, carrinho, formaPagamento);
            System.out.println("\n==================================================");
            System.out.println("         COMPRA FINALIZADA COM SUCESSO!           ");
            System.out.println("==================================================");
            System.out.println("  " + pedido);
            System.out.printf("  Cashback gerado: R$ %.2f%n", pedido.getCashbackGerado());
            System.out.println("==================================================");
        } catch (Exception e) {
            System.out.println("[Erro ao finalizar compra] " + e.getMessage());
        }
        Entrada.aguardarEnter();
    }
}
