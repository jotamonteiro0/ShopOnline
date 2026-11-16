package teste;

import model.Produto;
import repository.ProdutoRepository;
import service.EstoqueService;

import java.io.File;
import java.util.List;

/**
 * Classe de verificacao do modulo do desenvolvedor Joao Guilherme Monteiro (dev_joao).
 * Executar diretamente no IntelliJ IDEA para validar todas as regras antes de subir ao Git.
 */
public class VerificacaoModuloJoao {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     CHECKLIST DE VERIFICACAO - MODULO DO JOAO    ");
        System.out.println("==================================================");

        int totalTestes = 6;
        int testesAprovados = 0;

        String arquivoTeste = "data/produtos_teste.csv";
        new File(arquivoTeste).delete(); // Limpa arquivo de teste anterior

        ProdutoRepository repo = new ProdutoRepository(arquivoTeste);
        EstoqueService estoqueService = new EstoqueService(repo);

        // TESTE 1: Cadastro e Busca
        System.out.print("[1/6] Testando cadastro e busca de produtos... ");
        Produto p1 = new Produto("T01", "Monitor 144Hz", "Monitor Gamer 24 polegadas", 1200.0, 10, true);
        Produto p2 = new Produto("T02", "Mouse Pad", "Mouse Pad Speed Extra Grande", 50.0, 4, false);
        repo.salvar(p1);
        repo.salvar(p2);

        Produto pBuscado = repo.buscarPorId("T01");
        if (pBuscado != null && pBuscado.getNome().equals("Monitor 144Hz") && pBuscado.getPreco() == 1200.0) {
            System.out.println("PASSOU");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 2: Baixa de estoque com sucesso
        System.out.print("[2/6] Testando baixa de estoque com quantidade valida... ");
        boolean baixou = estoqueService.darBaixaEstoque("T01", 3);
        Produto pAposBaixa = repo.buscarPorId("T01");
        if (baixou && pAposBaixa.getQuantidadeEstoque() == 7) {
            System.out.println("PASSOU");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 3: Disparo de Alerta de Reposicao (< 5 unidades)
        System.out.print("[3/6] Testando deteccao de alerta de estoque baixo (< 5 unidades)... ");
        estoqueService.darBaixaEstoque("T01", 3); // Estoque cai para 4
        List<Produto> alertas = estoqueService.listarProdutosComAlertaReposicao();
        boolean alertaDetectado = alertas.stream().anyMatch(p -> p.getId().equals("T01"));
        if (alertaDetectado) {
            System.out.println("PASSOU (Estoque restante = 4, Alerta ativo)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 4: Bloqueio de venda com estoque insuficiente
        System.out.print("[4/6] Testando bloqueio de venda sem estoque suficiente... ");
        boolean tentativaInvalida = estoqueService.darBaixaEstoque("T01", 100);
        Produto pSemAlteracao = repo.buscarPorId("T01");
        if (!tentativaInvalida && pSemAlteracao.getQuantidadeEstoque() == 4) {
            System.out.println("PASSOU (Estoque negativo prevenido)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 5: Persistencia e Recarga do CSV
        System.out.print("[5/6] Testando persistencia em disco e recarga CSV... ");
        ProdutoRepository repoRecarregado = new ProdutoRepository(arquivoTeste);
        Produto pRecarregado = repoRecarregado.buscarPorId("T01");
        if (pRecarregado != null && pRecarregado.getQuantidadeEstoque() == 4 && pRecarregado.isPromocaoVip()) {
            System.out.println("PASSOU (Dados lidos com fidelidade do arquivo)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 6: Resiliencia a Arquivo Ausente
        System.out.print("[6/6] Testando resiliencia a arquivo ausente/corrompido... ");
        try {
            ProdutoRepository repoInexistente = new ProdutoRepository("data/arquivo_que_nao_existe.csv");
            repoInexistente.salvar(new Produto("T99", "Item Fantasma", "Desc", 10.0, 1, false));
            System.out.println("PASSOU (Arquivo criado automaticamente sem travar)");
            testesAprovados++;
            new File("data/arquivo_que_nao_existe.csv").delete();
        } catch (Exception e) {
            System.out.println("FALHOU com excecao: " + e.getMessage());
        }

        // Limpeza
        new File(arquivoTeste).delete();

        System.out.println("==================================================");
        System.out.printf("RESULTADO FINAL: %d de %d testes aprovados!%n", testesAprovados, totalTestes);
        if (testesAprovados == totalTestes) {
            System.out.println("STATUS: Modulo dev_joao validado e pronto para commit/push!");
        } else {
            System.out.println("STATUS: Existem pendencias para correcao antes do commit.");
        }
        System.out.println("==================================================");
    }
}
