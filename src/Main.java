import model.*;
import model.pagamento.*;
import repository.ClienteRepository;
import repository.ProdutoRepository;
import service.EstoqueService;
import service.FidelidadeService;
import service.VendaService;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     SISTEMA DE E-COMMERCE - SHOPONLINE (P2)      ");
        System.out.println("==================================================");

        String caminhoProdutos = "data/produtos.csv";
        String caminhoClientes = "data/clientes.csv";

        ProdutoRepository produtoRepo = new ProdutoRepository(caminhoProdutos);
        ClienteRepository clienteRepo = new ClienteRepository(caminhoClientes);
        EstoqueService estoqueService = new EstoqueService(produtoRepo);
        FidelidadeService fidelidadeService = new FidelidadeService(clienteRepo);
        VendaService vendaService = new VendaService(produtoRepo, clienteRepo, estoqueService, fidelidadeService);

        // Se o banco CSV estiver vazio, popula com dados de teste
        if (produtoRepo.listarTodos().isEmpty()) {
            produtoRepo.salvar(new Produto("P01", "Notebook Gamer", "Notebook Ryzen 7 16GB SSD 512GB", 4500.00, 6, true));
            produtoRepo.salvar(new Produto("P02", "Mouse Sem Fio", "Mouse Óptico 1600 DPI", 80.00, 4, false)); // < 5 gera alerta
            produtoRepo.salvar(new Produto("P03", "Teclado Mecânico", "Teclado RGB Switch Blue", 250.00, 15, true));
        }

        if (clienteRepo.listarTodos().isEmpty()) {
            clienteRepo.salvar(new Cliente("C01", "João Monteiro", "joao@email.com", 600, 25.0)); // Nível Prata
            clienteRepo.salvar(new ClienteVip("C02", "Vitória Salgado", "vitoria@email.com", 1600, 120.0)); // VIP + Ouro
            clienteRepo.salvar(new Cliente("C03", "Lucas Silva", "lucas@email.com", 100, 0.0)); // Nível Bronze
        }

        System.out.println("\n--- Clientes Cadastrados ---");
        for (Cliente c : clienteRepo.listarTodos()) {
            System.out.println(c);
        }

        System.out.println("\n--- Catálogo de Produtos ---");
        for (Produto p : produtoRepo.listarTodos()) {
            System.out.println(p);
        }

        System.out.println("\n--- Simulação de Compra ---");
        Cliente clienteVip = clienteRepo.buscarPorId("C02");
        Carrinho carrinho = new Carrinho();

        carrinho.adicionarItem(produtoRepo.buscarPorId("P01"), 1);
        carrinho.adicionarItem(produtoRepo.buscarPorId("P02"), 1);

        Cupom cupom = new Cupom("BEMVINDO10", TipoCupom.PERCENTUAL, 10.0, LocalDate.now().plusDays(30));
        carrinho.aplicarCupom(cupom);

        System.out.println("Cliente: " + clienteVip.getNome() + " (VIP)");
        System.out.printf("Subtotal: R$ %.2f%n", carrinho.getSubtotal());
        System.out.printf("Desconto VIP: R$ %.2f%n", carrinho.calcularDescontoVip(clienteVip));
        System.out.printf("Desconto Cupom: R$ %.2f%n", carrinho.calcularDescontoCupom());

        FormaPagamento pagamentoPix = new PagamentoPix();
        Pedido pedido = vendaService.finalizarPedido("PED-1001", clienteVip, carrinho, pagamentoPix);

        System.out.println("\n--- Pedido Finalizado ---");
        System.out.println(pedido);

        System.out.println("\n--- Relatório de Vendas ---");
        vendaService.exibirRelatorioVendas();

        System.out.println("\n--- Cliente após Cashback e Pontos ---");
        System.out.println(clienteRepo.buscarPorId("C02"));
    }
}
