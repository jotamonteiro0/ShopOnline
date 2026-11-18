package view;

import model.Cliente;
import model.ClienteVip;
import repository.ClienteRepository;

import java.io.UncheckedIOException;
import java.util.List;

public class MenuClientes {
    private final ClienteRepository clienteRepo;

    public MenuClientes(ClienteRepository clienteRepo) {
        this.clienteRepo = clienteRepo;
    }

    public void exibirMenu() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println("\n--- Gestão de clientes ---");
            System.out.println("1. Cadastrar cliente comum");
            System.out.println("2. Cadastrar cliente VIP");
            System.out.println("3. Listar clientes");
            System.out.println("0. Voltar");

            int opcao = Entrada.lerInteiro("Escolha uma opção: ", 0, 3);
            switch (opcao) {
                case 1 -> cadastrarCliente(false);
                case 2 -> cadastrarCliente(true);
                case 3 -> listarClientes();
                case 0 -> voltar = true;
            }
        }
    }

    private void cadastrarCliente(boolean vip) {
        System.out.println(vip
                ? "\n--- Cadastrar cliente VIP ---"
                : "\n--- Cadastrar cliente comum ---");

        String id = Entrada.lerId("ID do cliente: ");
        if (clienteRepo.buscarPorId(id) != null) {
            System.out.println("[Erro] Já existe um cliente com esse ID.");
            Entrada.aguardarEnter();
            return;
        }

        String nome = Entrada.lerTexto("Nome: ", true);
        String email = Entrada.lerTexto("E-mail: ", true);
        try {
            Cliente cliente = vip
                    ? new ClienteVip(id, nome, email)
                    : new Cliente(id, nome, email);

            clienteRepo.cadastrar(cliente);
            System.out.println("Cliente adicionado à lista.");
        } catch (IllegalArgumentException e) {
            System.out.println("[Erro] " + e.getMessage());
        } catch (UncheckedIOException e) {
            System.out.println("[Erro] Não foi possível salvar o cliente. "
                    + "Verifique o arquivo e tente novamente.");
        }
        Entrada.aguardarEnter();
    }

    private void listarClientes() {
        List<Cliente> clientes = clienteRepo.listarTodos();
        if (clientes.isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.");
        } else {
            for (Cliente cliente : clientes) {
                System.out.printf("[%s] %s%n", cliente.getId(), cliente);
            }
        }
        Entrada.aguardarEnter();
    }
}
