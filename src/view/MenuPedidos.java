package view;

import model.Pedido;
import service.VendaService;

public class MenuPedidos {
    private final VendaService vendaService;

    public MenuPedidos(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    public void exibirMenu() {
        while (true) {
            System.out.println("\n--- Pagamentos e Pedidos ---");
            System.out.println("1. Listar pedidos");
            System.out.println("2. Avancar status do pedido");
            System.out.println("3. Cancelar pedido em processamento");
            System.out.println("0. Voltar");
            int opcao = Entrada.lerInteiro("Opcao: ", 0, 3);
            if (opcao == 0) return;
            try {
                if (opcao == 1) {
                    if (vendaService.listarPedidos().isEmpty()) System.out.println("Nenhum pedido registrado.");
                    for (Pedido pedido : vendaService.listarPedidos()) System.out.println(pedido);
                } else {
                    String id = Entrada.lerTexto("ID do pedido: ", true);
                    boolean realizado = opcao == 2 ? vendaService.avancarStatusPedido(id) : vendaService.cancelarPedido(id);
                    System.out.println(realizado ? "Pedido atualizado com sucesso."
                            : "Operacao recusada: confira o ID e o status do pedido.");
                }
            } catch (RuntimeException erro) {
                System.out.println("[Erro] Nao foi possivel atualizar o pedido: " + erro.getMessage());
            }
            Entrada.aguardarEnter();
        }
    }
}
