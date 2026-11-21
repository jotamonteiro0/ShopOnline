package view;

import model.Produto;
import service.EstoqueService;

import java.util.List;

public class MenuProdutos {
    private final EstoqueService estoqueService;

    public MenuProdutos(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    public void exibirMenu() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println("\n==================================================");
            System.out.println("          GESTAO DE PRODUTOS E ESTOQUE            ");
            System.out.println("==================================================");
            System.out.println("1. Cadastrar Novo Produto");
            System.out.println("2. Listar Todos os Produtos");
            System.out.println("3. Consultar Produto por ID");
            System.out.println("4. Repor Estoque de Produto");
            System.out.println("5. Relatorio de Produtos em Alerta (< 5 unidades)");
            System.out.println("6. Resumo Geral do Inventario");
            System.out.println("0. Voltar ao Menu Principal");
            System.out.println("--------------------------------------------------");

            int opcao = Entrada.lerInteiro("Escolha uma opcao: ", 0, 6);
            try {
                switch (opcao) {
                    case 1 -> cadastrarProduto();
                    case 2 -> listarProdutos();
                    case 3 -> consultarPorId();
                    case 4 -> reporEstoque();
                    case 5 -> listarAlertas();
                    case 6 -> exibirResumoInventario();
                    case 0 -> voltar = true;
                }
            } catch (RuntimeException erro) {
                System.out.println("[Erro] Nao foi possivel salvar a alteracao: " + erro.getMessage());
            }
        }
    }

    private void cadastrarProduto() {
        System.out.println("\n--- Cadastrar Novo Produto ---");
        String id = Entrada.lerId("ID/Codigo do Produto (ex: P04): ");

        if (estoqueService.buscarProduto(id) != null) {
            System.out.printf("[Erro] Ja existe um produto cadastrado com o ID '%s'.%n", id);
            Entrada.aguardarEnter();
            return;
        }

        String nome = Entrada.lerTexto("Nome do Produto: ", true);
        String descricao = Entrada.lerTexto("Descricao detalhada: ", false);
        double preco = Entrada.lerDouble("Preco unitario (R$): ", 0.01);
        int estoque = Entrada.lerInteiro("Quantidade inicial em estoque: ", 0, 10000);
        boolean vip = Entrada.lerConfirmacao("Este produto participa de promocao VIP (10% desconto)");

        boolean sucesso = estoqueService.cadastrarProduto(id, nome, descricao, preco, estoque, vip);
        if (sucesso) {
            System.out.println("\n[Sucesso] Produto cadastrado e persistido no arquivo data/produtos.csv!");
            if (estoque < 5) {
                System.out.println(">>> [ALERTA DE REPOSICAO] Atencao: produto cadastrado com menos de 5 unidades!");
            }
        } else {
            System.out.println("\n[Erro] Falha ao cadastrar produto.");
        }
        Entrada.aguardarEnter();
    }

    private void listarProdutos() {
        System.out.println("\n--- Catalogo Geral de Produtos ---");
        List<Produto> lista = estoqueService.listarTodos();
        if (lista.isEmpty()) {
            System.out.println("Nenhum produto cadastrado no catalogo.");
        } else {
            System.out.println("-----------------------------------------------------------------------------------------");
            for (Produto p : lista) {
                System.out.println(p);
            }
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("Total de produtos cadastrados: %d itens%n", lista.size());
        }
        Entrada.aguardarEnter();
    }

    private void consultarPorId() {
        System.out.println("\n--- Consultar Produto por ID ---");
        String id = Entrada.lerId("Digite o ID do produto: ");
        Produto p = estoqueService.buscarProduto(id);
        if (p == null) {
            System.out.printf("[Aviso] Produto com ID '%s' nao foi encontrado.%n", id);
        } else {
            System.out.println("\nDados do Produto:");
            System.out.println("  ID: " + p.getId());
            System.out.println("  Nome: " + p.getNome());
            System.out.println("  Descricao: " + (p.getDescricao().isEmpty() ? "(sem descricao)" : p.getDescricao()));
            System.out.printf("  Preco: R$ %.2f%n", p.getPreco());
            System.out.printf("  Estoque Atual: %d unidades%n", p.getQuantidadeEstoque());
            System.out.println("  Promocao VIP: " + (p.isPromocaoVip() ? "SIM (10% de desconto para clientes VIP)" : "NAO"));
            System.out.println("  Situacao: " + (p.precisaReposicao() ? "[ALERTA: REPOSICAO NECESSARIA]" : "[ESTOQUE REGULAR]"));
        }
        Entrada.aguardarEnter();
    }

    private void reporEstoque() {
        System.out.println("\n--- Reposicao de Estoque ---");
        String id = Entrada.lerId("ID do Produto a repor: ");
        Produto p = estoqueService.buscarProduto(id);
        if (p == null) {
            System.out.printf("[Erro] Produto com ID '%s' nao encontrado.%n", id);
            Entrada.aguardarEnter();
            return;
        }

        System.out.printf("Produto: %s | Estoque atual: %d unidades%n", p.getNome(), p.getQuantidadeEstoque());
        int quantidade = Entrada.lerInteiro("Quantidade a adicionar ao estoque: ", 1, 10000);

        boolean ok = estoqueService.reporEstoque(id, quantidade);
        if (ok) {
            System.out.printf("\n[Sucesso] Estoque atualizado! Novo saldo: %d unidades.%n", p.getQuantidadeEstoque());
        } else {
            System.out.println("\n[Erro] Falha ao atualizar o estoque.");
        }
        Entrada.aguardarEnter();
    }

    private void listarAlertas() {
        System.out.println("\n--- Produtos com Alerta de Reposicao (< 5 unidades) ---");
        List<Produto> alertas = estoqueService.listarProdutosComAlertaReposicao();
        if (alertas.isEmpty()) {
            System.out.println("Otimo! Todos os produtos estao com estoque igual ou superior a 5 unidades.");
        } else {
            System.out.println("ATENCAO: Os seguintes produtos necessitam de pedido de compra urgente:");
            System.out.println("-----------------------------------------------------------------------------------------");
            for (Produto p : alertas) {
                System.out.printf("  * [%s] %-25s | Saldo: %d unidade(s) | Preco: R$ %.2f%n",
                        p.getId(), p.getNome(), p.getQuantidadeEstoque(), p.getPreco());
            }
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("Total de produtos em estado critico: %d item(ns)%n", alertas.size());
        }
        Entrada.aguardarEnter();
    }

    private void exibirResumoInventario() {
        System.out.println("\n--- Resumo Geral do Inventario ---");
        List<Produto> produtos = estoqueService.listarTodos();
        int totalUnidades = estoqueService.calcularTotalUnidadesEmEstoque();
        double valorTotal = estoqueService.calcularValorTotalInventario();
        int itensEmAlerta = estoqueService.listarProdutosComAlertaReposicao().size();

        System.out.printf("Quantidade de Itens Cadastrados: %d%n", produtos.size());
        System.out.printf("Total de Unidades Fisicas em Estoque: %d unidades%n", totalUnidades);
        System.out.printf("Valor Financeiro Total do Estoque: R$ %.2f%n", valorTotal);
        System.out.printf("Produtos em Alerta de Reposicao: %d produto(s)%n", itensEmAlerta);
        Entrada.aguardarEnter();
    }
}
