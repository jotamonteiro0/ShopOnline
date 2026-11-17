package teste;

import model.Produto;
import repository.ProdutoRepository;
import service.EstoqueService;

import java.io.File;
import java.util.List;

/**
 * Suite de verificacao e testes automatizados do modulo do desenvolvedor Joao Guilherme Monteiro (dev_joao).
 * Valida todas as regras de negocio de catalogo, controle de estoque, alertas preventivos e persistencia CSV.
 */
public class VerificacaoModuloJoao {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     CHECKLIST DE VERIFICACAO - MODULO DO JOAO    ");
        System.out.println("==================================================");

        int totalTestes = 8;
        int testesAprovados = 0;

        String arquivoTeste = "data/produtos_teste_joao.csv";
        new File(arquivoTeste).delete(); // Limpa arquivo de teste anterior se existir

        ProdutoRepository repo = new ProdutoRepository(arquivoTeste);
        EstoqueService estoqueService = new EstoqueService(repo);

        // TESTE 1: Cadastro e Busca
        System.out.print("[1/8] Testando cadastro e busca de produtos... ");
        boolean cadastrou1 = estoqueService.cadastrarProduto("T01", "Monitor 144Hz", "Monitor Gamer 24p", 1200.0, 10, true);
        boolean cadastrou2 = estoqueService.cadastrarProduto("T02", "Mouse Pad Speed", "Tamanho Extra Grande", 50.0, 4, false);

        Produto pBuscado = repo.buscarPorId("T01");
        if (cadastrou1 && cadastrou2 && pBuscado != null && pBuscado.getNome().equals("Monitor 144Hz") && pBuscado.getPreco() == 1200.0) {
            System.out.println("PASSOU");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 2: Baixa de estoque com quantidade valida
        System.out.print("[2/8] Testando baixa de estoque com quantidade valida... ");
        boolean baixou = estoqueService.darBaixaEstoque("T01", 3);
        Produto pAposBaixa = repo.buscarPorId("T01");
        if (baixou && pAposBaixa != null && pAposBaixa.getQuantidadeEstoque() == 7) {
            System.out.println("PASSOU (Estoque reduzido de 10 para 7)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 3: Disparo de Alerta de Reposicao (< 5 unidades)
        System.out.print("[3/8] Testando disparo de alerta preventivo (< 5 unidades)... ");
        estoqueService.darBaixaEstoque("T01", 3); // Estoque cai para 4 unidades (< 5)
        List<Produto> alertas = estoqueService.listarProdutosComAlertaReposicao();
        boolean alertaDetectado = alertas.stream().anyMatch(p -> p.getId().equals("T01"));
        if (alertaDetectado) {
            System.out.println("PASSOU (Estoque atual = 4 un., Alerta ativo)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 4: Bloqueio de venda com estoque insuficiente
        System.out.print("[4/8] Testando prevencao de estoque negativo... ");
        boolean tentativaInvalida = estoqueService.darBaixaEstoque("T01", 100);
        Produto pSemAlteracao = repo.buscarPorId("T01");
        if (!tentativaInvalida && pSemAlteracao.getQuantidadeEstoque() == 4) {
            System.out.println("PASSOU (Bloqueou baixa excessiva com sucesso)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 5: Reposicao de estoque manual
        System.out.print("[5/8] Testando reposicao de inventario... ");
        boolean reposto = estoqueService.reporEstoque("T01", 10); // Sobe para 14
        Produto pReposto = repo.buscarPorId("T01");
        if (reposto && pReposto != null && pReposto.getQuantidadeEstoque() == 14 && !pReposto.precisaReposicao()) {
            System.out.println("PASSOU (Estoque reposto para 14 un., Alerta normalizado)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 6: Bloqueio de cadastro com ID duplicado
        System.out.print("[6/8] Testando bloqueio de cadastro com ID duplicado... ");
        boolean duplicado = estoqueService.cadastrarProduto("T01", "Monitor Clone", "Desc", 999.0, 5, false);
        if (!duplicado) {
            System.out.println("PASSOU (ID repetido rejeitado)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 7: Persistencia e Recarga do CSV
        System.out.print("[7/8] Testando persistencia em disco e recarga do arquivo CSV... ");
        ProdutoRepository repoRecarregado = new ProdutoRepository(arquivoTeste);
        Produto pRecarregado = repoRecarregado.buscarPorId("T01");
        if (pRecarregado != null && pRecarregado.getQuantidadeEstoque() == 14 && pRecarregado.isPromocaoVip()) {
            System.out.println("PASSOU (Arquivo CSV lido com fidelidade)");
            testesAprovados++;
        } else {
            System.out.println("FALHOU");
        }

        // TESTE 8: Resiliencia a Arquivo Ausente
        System.out.print("[8/8] Testando resiliencia a arquivo ausente/inexistente... ");
        try {
            String arqFantasma = "data/produtos_fantasma.csv";
            ProdutoRepository repoInexistente = new ProdutoRepository(arqFantasma);
            repoInexistente.salvar(new Produto("T99", "Item Fantasma", "Descricao", 15.0, 2, false));
            System.out.println("PASSOU (Arquivo e diretorio criados sem falhas)");
            testesAprovados++;
            new File(arqFantasma).delete();
        } catch (Exception e) {
            System.out.println("FALHOU com excecao: " + e.getMessage());
        }

        // Limpeza dos arquivos temporarios de teste
        new File(arquivoTeste).delete();

        System.out.println("==================================================");
        System.out.printf("RESULTADO FINAL: %d de %d testes aprovados!%n", testesAprovados, totalTestes);
        if (testesAprovados == totalTestes) {
            System.out.println("STATUS: Modulo dev_joao 100% VALIDADO e pronto para producao!");
        } else {
            System.out.println("STATUS: Existem pendencias para correcao antes do commit.");
        }
        System.out.println("==================================================");
    }
}
