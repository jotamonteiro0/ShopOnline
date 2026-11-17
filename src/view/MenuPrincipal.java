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

/**
 * Menu principal interativo do sistema ShopOnline.
 * Coordena os submenus e integra as frentes da equipe.
 */
public class MenuPrincipal {
    private final EstoqueService estoqueService;
    private final ClienteRepository clienteRepo;
    private final ProdutoRepository produtoRepo;
    private final FidelidadeService fidelidadeService;
    private final VendaService vendaService;
    private final CupomRepository cupomRepo;
    private final MenuProdutos menuProdutos;
    private final MenuCarrinho menuCarrinho;

    public MenuPrincipal(ProdutoRepository produtoRepo,
                         ClienteRepository clienteRepo,
                         EstoqueService estoqueService,
                         FidelidadeService fidelidadeService,
                         VendaService vendaService) {
        this.produtoRepo = produtoRepo;
        this.clienteRepo = clienteRepo;
        this.estoqueService = estoqueService;
        this.fidelidadeService = fidelidadeService;
        this.vendaService = vendaService;
        this.cupomRepo = new CupomRepository("data/cupons.csv");
        this.menuProdutos = new MenuProdutos(estoqueService);
        this.menuCarrinho = new MenuCarrinho(produtoRepo, clienteRepo, cupomRepo, estoqueService, vendaService);
    }

    public void iniciar() {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n==================================================");
            System.out.println("            SHOPONLINE - SISTEMA DE VENDAS        ");
            System.out.println("   iCEV - Engenharia de Software | Avaliacao P2   ");
            System.out.println("==================================================");
            System.out.println("1. Gestao de Produtos e Estoque (dev_joao)");
            System.out.println("2. Gestao de Clientes e VIP (dev_vitoria)");
            System.out.println("3. Carrinho, Cupons e Promocoes (dev_lucas)");
            System.out.println("4. Pagamentos e Pedidos (dev_gabriel)");
            System.out.println("5. Programa de Fidelidade e Relatorios (dev_lavynne)");
            System.out.println("6. Executar Demonstracao Rapida do Fluxo Completo");
            System.out.println("0. Sair");
            System.out.println("--------------------------------------------------");

            int opcao = Entrada.lerInteiro("Selecione uma opcao: ", 0, 6);
            switch (opcao) {
                case 1 -> menuProdutos.exibirMenu();
                case 2 -> menuClientesInfo();
                case 3 -> menuCarrinho.exibirMenu();
                case 4 -> menuPedidosInfo();
                case 5 -> menuFidelidadeInfo();
                case 6 -> executarDemonstracao();
                case 0 -> {
                    System.out.println("\nSalvando dados e encerrando o ShopOnline. Ate logo!");
                    rodando = false;
                }
            }
        }
    }

    private void menuClientesInfo() {
        System.out.println("\n==================================================");
        System.out.println("         MODULO: CLIENTES E ESPECIALIZACAO VIP    ");
        System.out.println("             Responsavel: Maria Vitoria (dev_vitoria) ");
        System.out.println("==================================================");
        System.out.println("Clientes atualmente carregados do arquivo data/clientes.csv:");
        for (Cliente c : clienteRepo.listarTodos()) {
            System.out.println("  * " + c);
        }
        System.out.println("\n[Informacao] Os submenus interativos de cadastro e validacao");
        System.out.println("estao em desenvolvimento na branch dev_vitoria.");
        Entrada.aguardarEnter();
    }

    private void menuCarrinhoInfo() {
        System.out.println("\n==================================================");
        System.out.println("        MODULO: CARRINHO E CUPONS DE DESCONTO     ");
        System.out.println("             Responsavel: Lucas Silveira (dev_lucas) ");
        System.out.println("==================================================");
        System.out.println("[Informacao] O gerenciador de carrinho interativo e validacao");
        System.out.println("de cupons por cliente estao em desenvolvimento na branch dev_lucas.");
        Entrada.aguardarEnter();
    }

    private void menuPedidosInfo() {
        System.out.println("\n==================================================");
        System.out.println("      MODULO: PAGAMENTOS E CICLO DE PEDIDOS       ");
        System.out.println("            Responsavel: Gabriel Buhatem (dev_gabriel)");
        System.out.println("==================================================");
        System.out.println("[Informacao] As rotinas de avanco logistico e cancelamento");
        System.out.println("restrito com estorno estao em desenvolvimento na branch dev_gabriel.");
        Entrada.aguardarEnter();
    }

    private void menuFidelidadeInfo() {
        System.out.println("\n==================================================");
        System.out.println("     MODULO: PROGRAMA DE FIDELIDADE E RELATORIOS  ");
        System.out.println("           Responsavel: Lavynne Marianne (dev_lavynne)");
        System.out.println("==================================================");
        System.out.println("[Informacao] A carteira de cashback e extrato analitico");
        System.out.println("estao em desenvolvimento na branch dev_lavynne.");
        Entrada.aguardarEnter();
    }

    private void executarDemonstracao() {
        System.out.println("\n==================================================");
        System.out.println("        DEMONSTRACAO DO FLUXO COMPLETO DE COMPRA   ");
        System.out.println("==================================================");

        Cliente clienteVip = clienteRepo.buscarPorId("C02");
        if (clienteVip == null) {
            clienteVip = new ClienteVip("C02", "Vitoria Salgado", "vitoria@email.com", 1600, 120.0);
            clienteRepo.salvar(clienteVip);
        }

        Produto pGamer = produtoRepo.buscarPorId("P01");
        Produto pMouse = produtoRepo.buscarPorId("P02");

        if (pGamer == null || pMouse == null) {
            System.out.println("[Aviso] Populando produtos padrao para a demonstracao...");
            pGamer = new Produto("P01", "Notebook Gamer", "Notebook Ryzen 7", 4500.0, 6, true);
            pMouse = new Produto("P02", "Mouse Sem Fio", "Mouse Optico 1600 DPI", 80.0, 4, false);
            produtoRepo.salvar(pGamer);
            produtoRepo.salvar(pMouse);
        }

        Carrinho carrinho = new Carrinho();
        carrinho.adicionarItem(pGamer, 1);
        carrinho.adicionarItem(pMouse, 1);

        Cupom cupom = new Cupom("BEMVINDO10", TipoCupom.PERCENTUAL, 10.0, LocalDate.now().plusDays(30));
        carrinho.aplicarCupom(cupom);

        System.out.println("Cliente: " + clienteVip.getNome() + " (VIP)");
        System.out.printf("Subtotal: R$ %.2f%n", carrinho.getSubtotal());
        System.out.printf("Desconto VIP: R$ %.2f%n", carrinho.calcularDescontoVip(clienteVip));
        System.out.printf("Desconto Cupom: R$ %.2f%n", carrinho.calcularDescontoCupom());

        FormaPagamento pagamentoPix = new PagamentoPix();
        Pedido pedido = vendaService.finalizarPedido("PED-" + System.currentTimeMillis() % 10000, clienteVip, carrinho, pagamentoPix);

        System.out.println("\nPedido Finalizado:");
        System.out.println("  " + pedido);

        System.out.println("\nRelatorio de Vendas:");
        vendaService.exibirRelatorioVendas();

        Entrada.aguardarEnter();
    }
}
