import model.*;
import model.pagamento.*;
import repository.ClienteRepository;
import repository.ProdutoRepository;
import service.EstoqueService;
import service.FidelidadeService;
import service.VendaService;
import view.MenuPrincipal;

import java.time.LocalDate;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        String caminhoProdutos = "data/produtos.csv";
        String caminhoClientes = "data/clientes.csv";

        ProdutoRepository produtoRepo = new ProdutoRepository(caminhoProdutos);
        ClienteRepository clienteRepo = new ClienteRepository(caminhoClientes);
        EstoqueService estoqueService = new EstoqueService(produtoRepo);
        FidelidadeService fidelidadeService = new FidelidadeService(clienteRepo);
        VendaService vendaService = new VendaService(produtoRepo, clienteRepo, estoqueService, fidelidadeService);

        // Se o banco CSV estiver vazio, popula com dados iniciais de teste
        if (produtoRepo.listarTodos().isEmpty()) {
            produtoRepo.salvar(new Produto("P01", "Notebook Gamer", "Ryzen 7 16GB SSD 512GB", 4500.00, 6, true));
            produtoRepo.salvar(new Produto("P02", "Mouse Sem Fio", "Mouse Optico 1600 DPI", 80.00, 4, false)); // < 5 alerta
            produtoRepo.salvar(new Produto("P03", "Teclado Mecanico", "Switch Blue RGB", 250.00, 15, true));
        }

        if (clienteRepo.listarTodos().isEmpty()) {
            clienteRepo.salvar(new Cliente("C01", "Joao Monteiro", "joao@email.com", 600, 25.0)); // Prata
            clienteRepo.salvar(new ClienteVip("C02", "Vitoria Salgado", "vitoria@email.com", 1600, 120.0)); // VIP Ouro
            clienteRepo.salvar(new Cliente("C03", "Lucas Silva", "lucas@email.com", 100, 0.0)); // Bronze
        }

        // Modo demonstracao direta se passado argumento --demo
        if (args.length > 0 && args[0].equalsIgnoreCase("--demo")) {
            executarDemonstracaoRapida(produtoRepo, clienteRepo, vendaService);
            return;
        }

        // Modo interativo padrao
        MenuPrincipal menu = new MenuPrincipal(produtoRepo, clienteRepo, estoqueService, fidelidadeService, vendaService);
        menu.iniciar();
    }

    private static void executarDemonstracaoRapida(ProdutoRepository produtoRepo, ClienteRepository clienteRepo, VendaService vendaService) {
        System.out.println("==================================================");
        System.out.println("     SISTEMA DE E-COMMERCE - SHOPONLINE (P2)      ");
        System.out.println("==================================================");

        Cliente clienteVip = clienteRepo.buscarPorId("C02");
        Carrinho carrinho = new Carrinho();

        carrinho.adicionarItem(produtoRepo.buscarPorId("P01"), 1);
        carrinho.adicionarItem(produtoRepo.buscarPorId("P02"), 1);

        Cupom cupom = new Cupom("BEMVINDO10", TipoCupom.PERCENTUAL, 10.0, LocalDate.now().plusDays(30));
        carrinho.aplicarCupom(cupom);

        FormaPagamento pagamentoPix = new PagamentoPix();
        Pedido pedido = vendaService.finalizarPedido("PED-" + UUID.randomUUID().toString().toUpperCase(), clienteVip, carrinho, pagamentoPix);

        System.out.println("\nPedido Finalizado:");
        System.out.println("  " + pedido);

        System.out.println("\nRelatorio de Vendas:");
        vendaService.exibirRelatorioVendas();
    }
}
