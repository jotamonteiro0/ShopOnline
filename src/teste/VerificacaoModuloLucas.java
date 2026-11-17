package teste;

import model.*;
import repository.CupomRepository;

import java.io.File;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class VerificacaoModuloLucas {
    private static int totalTestes = 0;
    private static int testesAprovados = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    SUITE DE VERIFICACAO - MODULO LUCAS (dev_lucas)");
        System.out.println("==================================================");

        testarAdicaoEProdutoCarrinho();
        testarRemocaoEAlteracaoQuantidade();
        testarSubtotaisECalculos();
        testarCupomPercentualEFixo();
        testarValidadeEExpiracaoCupom();
        testarUsoUnicoPorClienteEOutrosClientes();
        testarPersistenciaCupomCSV();
        testarCombinacaoVipCupomETotalNaoNegativo();

        System.out.println("==================================================");
        System.out.printf("RESULTADO FINAL: %d de %d testes aprovados.%n", testesAprovados, totalTestes);
        System.out.println("==================================================");

        if (testesAprovados < totalTestes) {
            System.exit(1);
        }
    }

    private static void assertTest(String nomeTeste, boolean condicao) {
        totalTestes++;
        if (condicao) {
            testesAprovados++;
            System.out.printf("[PASSOU] Teste %d: %s%n", totalTestes, nomeTeste);
        } else {
            System.out.printf("[FALHOU] Teste %d: %s%n", totalTestes, nomeTeste);
        }
    }

    private static void testarAdicaoEProdutoCarrinho() {
        Carrinho carrinho = new Carrinho();
        Produto p = new Produto("P99", "Produto Teste", "Descricao", 100.0, 10, false);
        carrinho.adicionarItem(p, 2);

        assertTest("Adicao de produto ao carrinho e contagem de itens", carrinho.getItens().size() == 1);
        assertTest("Verificacao de quantidade do item no carrinho", carrinho.getItens().get(0).getQuantidade() == 2);
    }

    private static void testarRemocaoEAlteracaoQuantidade() {
        Carrinho carrinho = new Carrinho();
        Produto p = new Produto("P99", "Produto Teste", "Descricao", 100.0, 10, false);
        carrinho.adicionarItem(p, 3); // 3 unidades
        
        boolean parcialOk = carrinho.removerQuantidadeItem("P99", 1);
        assertTest("Remocao parcial de 1 unidade (deve restar 2)", parcialOk && carrinho.getItens().get(0).getQuantidade() == 2);

        boolean excesoOk = carrinho.removerQuantidadeItem("P99", 5);
        assertTest("Rejeicao de tentativa de remover mais unidades do que o disponivel no carrinho", !excesoOk && carrinho.getItens().get(0).getQuantidade() == 2);

        boolean totalOk = carrinho.removerQuantidadeItem("P99", 2);
        assertTest("Remocao total das unidades restantes retira o produto do carrinho", totalOk && carrinho.getItens().isEmpty());
    }

    private static void testarSubtotaisECalculos() {
        Carrinho carrinho = new Carrinho();
        Produto p1 = new Produto("P1", "Item 1", "Desc", 50.0, 20, false);
        Produto p2 = new Produto("P2", "Item 2", "Desc", 100.0, 20, false);

        carrinho.adicionarItem(p1, 2); // 100.0
        carrinho.adicionarItem(p2, 1); // 100.0

        assertTest("Calculo correto do subtotal do carrinho", carrinho.getSubtotal() == 200.0);
    }

    private static void testarCupomPercentualEFixo() {
        Cupom cupomPerc = new Cupom("DESC20", TipoCupom.PERCENTUAL, 20.0, LocalDate.now().plusDays(10));
        assertTest("Calculo correto de desconto percentual (20% de 200 = 40)", cupomPerc.calcularDesconto(200.0) == 40.0);

        Cupom cupomFixo = new Cupom("VALOR50", TipoCupom.VALOR_FIXO, 50.0, LocalDate.now().plusDays(10));
        assertTest("Calculo correto de desconto fixo (R$ 50 de 200 = 50)", cupomFixo.calcularDesconto(200.0) == 50.0);

        Cupom cupomFixoMaior = new Cupom("VALOR300", TipoCupom.VALOR_FIXO, 300.0, LocalDate.now().plusDays(10));
        assertTest("Desconto fixo nao ultrapassa o subtotal da compra", cupomFixoMaior.calcularDesconto(200.0) == 200.0);
    }

    private static void testarValidadeEExpiracaoCupom() {
        Cupom cupomValido = new Cupom("VALIDO", TipoCupom.PERCENTUAL, 10.0, LocalDate.now().plusDays(5));
        assertTest("Cupom com data futura e valido", cupomValido.isValido());

        Cupom cupomExpirado = new Cupom("EXPIRADO", TipoCupom.PERCENTUAL, 10.0, LocalDate.now().minusDays(1));
        assertTest("Cupom com data passada e rejeitado (expirado)", !cupomExpirado.isValido());
    }

    private static void testarUsoUnicoPorClienteEOutrosClientes() {
        Cupom cupom = new Cupom("UNICO", TipoCupom.PERCENTUAL, 10.0, LocalDate.now().plusDays(5));
        String clienteA = "C01";
        String clienteB = "C02";

        assertTest("Cupom inicialmente valido para cliente A", cupom.isValidoParaCliente(clienteA));

        cupom.marcarComoUtilizado(clienteA);
        assertTest("Cupom e marcado como utilizado pelo cliente A e rejeitado para reuso", !cupom.isValidoParaCliente(clienteA));
        assertTest("Mesmo cupom continua valido para cliente B (clientes diferentes)", cupom.isValidoParaCliente(clienteB));

        cupom.marcarComoUtilizado(clienteB);
        assertTest("Cupom e marcado como utilizado pelo cliente B e rejeitado para ambos", !cupom.isValidoParaCliente(clienteB));
    }

    private static void testarPersistenciaCupomCSV() {
        String arquivoTeste = "data/test_cupons.csv";
        File f = new File(arquivoTeste);
        if (f.exists()) f.delete();

        CupomRepository repo = new CupomRepository(arquivoTeste);
        Set<String> usados = new HashSet<>();
        usados.add("C01");
        Cupom c = new Cupom("TESTCSV", TipoCupom.PERCENTUAL, 15.0, LocalDate.of(2030, 1, 1), usados);
        repo.salvar(c);

        // Recriar repositorio para testar leitura do arquivo
        CupomRepository repoReload = new CupomRepository(arquivoTeste);
        Cupom cCarregado = repoReload.buscarPorCodigo("TESTCSV");

        assertTest("Cupom carregado com sucesso do CSV", cCarregado != null);
        assertTest("Tipo do cupom carregado corretamente", cCarregado.getTipo() == TipoCupom.PERCENTUAL);
        assertTest("Valor do cupom carregado corretamente", cCarregado.getValor() == 15.0);
        assertTest("Historico de clientes utilizados persistido no CSV", cCarregado.foiUtilizadoPor("C01"));

        if (f.exists()) f.delete();
    }

    private static void testarCombinacaoVipCupomETotalNaoNegativo() {
        ClienteVip clienteVip = new ClienteVip("VIP01", "Maria VIP", "maria@email.com", 2000, 0.0);
        Produto pVip = new Produto("PVIP", "Item VIP", "Desc", 1000.0, 10, true); // 10% desconto VIP = 100.0

        Carrinho carrinho = new Carrinho();
        carrinho.adicionarItem(pVip, 1);

        Cupom cupomFixo = new Cupom("DESC900", TipoCupom.VALOR_FIXO, 900.0, LocalDate.now().plusDays(5));
        carrinho.aplicarCupom(cupomFixo, clienteVip);

        double subtotal = carrinho.getSubtotal(); // 1000.0
        double descVip = carrinho.calcularDescontoVip(clienteVip); // 100.0
        double descCupom = carrinho.calcularDescontoCupom(); // 900.0
        double total = carrinho.calcularTotal(clienteVip); // 1000 - 100 - 900 = 0.0

        assertTest("Calculo correto de desconto VIP (10% de 1000 = 100)", descVip == 100.0);
        assertTest("Calculo correto de desconto cupom (900)", descCupom == 900.0);
        assertTest("Total final nunca fica negativo (garantido >= 0)", total >= 0.0 && total == 0.0);
    }
}
