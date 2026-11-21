package teste;

import model.*;
import model.pagamento.PagamentoBoleto;
import model.pagamento.PagamentoPix;
import repository.*;
import service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;

/** Reproduz falhas de integracao sem tocar nos CSVs reais. */
public class VerificacaoCorrecoesEntrega {
    public static void main(String[] args) throws Exception {
        int falhas = 0;
        for (String caso : new String[]{"descontos", "cashback", "cupom", "relatorio", "estoque", "quantidade",
                "falhaCheckout", "falhaCupom", "csvLegado", "cupomAlterado", "clienteIdBooleano", "precisaoCupom"}) {
            Path pasta = Files.createTempDirectory("shoponline-correcao-");
            try {
                verificarCaso(caso, pasta);
                System.out.println("PASSOU: " + caso);
            } catch (Exception | AssertionError erro) {
                falhas++;
                System.out.println("FALHOU: " + caso + " - " + erro);
            } finally {
                try (var arquivos = Files.walk(pasta)) {
                    for (Path arquivo : arquivos.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(arquivo);
                    }
                }
            }
        }
        if (falhas > 0) throw new AssertionError(falhas + " casos falharam.");
    }

    private static void verificarCaso(String caso, Path pasta) throws Exception {
        ClienteRepository clientes = new ClienteRepository(pasta.resolve("clientes.csv").toString());
        ProdutoRepository produtos = new ProdutoRepository(pasta.resolve("produtos.csv").toString());
        Cliente cliente = new ClienteVip("C01", "Cliente", "cliente@teste.com", 0, 20);
        clientes.salvar(cliente);
        Produto produto = new Produto("P01", "Produto", "", 100, 10, true);
        produtos.salvar(produto);
        EstoqueService estoque = new EstoqueService(produtos);
        PedidoService pedidos = new PedidoService(new PedidoRepository(
                pasta.resolve("pedidos.csv").toString(), clientes, produtos), estoque);
        VendaService vendas = new VendaService(produtos, clientes, estoque, new FidelidadeService(clientes), pedidos);
        switch (caso) {
            case "descontos" -> {
                Carrinho carrinho = carrinho(produto);
                carrinho.aplicarCupom(cupom(95));
                Pedido pedido = vendas.finalizarPedido("PED1", cliente, carrinho, new PagamentoPix());
                valor(pedido.getValorFinal(), 0, "Total nao pode ser negativo");
                valor(pedido.getDescontoTotal(), 100, "Desconto limitado ao subtotal");
            }
            case "cashback" -> {
                Pedido pedido = vendas.finalizarPedido("PED1", cliente, carrinho(produto), new PagamentoPix(), true);
                valor(pedido.getValorFinal(), 65.50, "VIP, PIX e resgate de R$ 20");
                valor(cliente.getSaldoCashback(), 1.31, "Novo cashback sobre o valor pago");
                valor(new ClienteRepository(pasta.resolve("clientes.csv").toString())
                        .buscarPorId("C01").getSaldoCashback(), 1.31, "Saldo persistido");
                try {
                    vendas.finalizarPedido("PED1", cliente, carrinho(produto), new PagamentoPix(), true);
                    throw new AssertionError("ID duplicado aceito");
                } catch (IllegalArgumentException esperado) { }
                valor(cliente.getSaldoCashback(), 1.31, "Falha nao debita cashback");

                cliente.atualizarFidelidade(499, 20);
                clientes.salvar(cliente);
                produto.setPreco(200);
                produtos.salvar(produto);
                Pedido promocao = vendas.finalizarPedido("PED2", cliente, carrinho(produto), new PagamentoPix(), true);
                valor(promocao.getValorFinal(), 151, "Resgate antes da pontuacao");
                valor(promocao.getCashbackGerado(), 3.02, "Cashback usa nivel anterior a compra");
                valor(cliente.getSaldoCashback(), 3.02, "Saldo apos resgate e novo credito");
                if (cliente.getPontosFidelidade() != 514 || cliente.getNivelFidelidade() != NivelFidelidade.PRATA) {
                    throw new AssertionError("Compra nao promoveu o cliente de Bronze para Prata");
                }
                cliente.atualizarFidelidade(Integer.MAX_VALUE, 20);
                clientes.salvar(cliente);
                int estoqueAnterior = produto.getQuantidadeEstoque();
                try {
                    vendas.finalizarPedido("PED3", cliente, carrinho(produto), new PagamentoPix(), true);
                    throw new AssertionError("Estouro dos pontos aceito");
                } catch (ArithmeticException esperado) { }
                if (cliente.getPontosFidelidade() != Integer.MAX_VALUE
                        || produto.getQuantidadeEstoque() != estoqueAnterior || vendas.buscarPedido("PED3") != null) {
                    throw new AssertionError("Falha de pontuacao deixou alteracoes parciais");
                }
                valor(cliente.getSaldoCashback(), 20, "Falha de pontuacao restaura saldo");
            }
            case "cupom" -> {
                Carrinho primeiro = carrinho(produto);
                primeiro.aplicarCupom(cupom(10));
                vendas.finalizarPedido("PED1", cliente, primeiro, new PagamentoBoleto());
                VendaService reabertas = new VendaService(produtos, clientes, estoque, new FidelidadeService(clientes), pedidos);
                Carrinho repetido = carrinho(produto);
                repetido.aplicarCupom(cupom(10));
                try {
                    reabertas.finalizarPedido("PED2", cliente, repetido, new PagamentoBoleto());
                    throw new AssertionError("Mesmo codigo reutilizado pelo mesmo cliente");
                } catch (IllegalArgumentException | IllegalStateException esperado) { }
                Cliente outro = new Cliente("C02", "Outro", "outro@teste.com");
                clientes.salvar(outro);
                Carrinho permitido = carrinho(produto);
                permitido.aplicarCupom(cupom(10));
                valor(reabertas.finalizarPedido("PED3", outro, permitido, new PagamentoBoleto())
                        .getValorFinal(), 90, "Outro cliente pode usar o cupom");
            }
            case "relatorio" -> {
                vendas.finalizarPedido("PED1", cliente, carrinho(produto), new PagamentoBoleto());
                PedidoService recarregados = new PedidoService(new PedidoRepository(
                        pasta.resolve("pedidos.csv").toString(), clientes, produtos), estoque);
                VendaService reabertas = new VendaService(produtos, clientes, estoque,
                        new FidelidadeService(clientes), recarregados);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                PrintStream original = System.out;
                Locale locale = Locale.getDefault();
                try {
                    Locale.setDefault(Locale.US);
                    System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
                    reabertas.exibirRelatorioVendas();
                } finally {
                    System.setOut(original);
                    Locale.setDefault(locale);
                }
                String texto = bytes.toString(StandardCharsets.UTF_8);
                if (!texto.contains("PED1") || !texto.contains("Receita Total Acumulada: R$ 90.00")) {
                    throw new AssertionError("Relatorio perdeu historico apos reinicio: " + texto);
                }
            }
            case "estoque" -> {
                Path arquivo = pasta.resolve("produtos.csv");
                Files.delete(arquivo);
                Files.createDirectory(arquivo);
                Files.writeString(arquivo.resolve("preservar"), "original");
                try {
                    estoque.darBaixaEstoque("P01", 1);
                    throw new AssertionError("Baixa confirmou gravacao que falhou");
                } catch (java.io.UncheckedIOException esperado) { }
                if (produto.getQuantidadeEstoque() != 10) throw new AssertionError("Estoque em memoria alterado");
                if (!Files.readString(arquivo.resolve("preservar")).equals("original")) {
                    throw new AssertionError("Destino alterado");
                }
            }
            case "quantidade" -> {
                Carrinho carrinho = carrinho(produto);
                try {
                    carrinho.adicionarItem(produto, -1);
                    throw new AssertionError("Quantidade negativa aceita");
                } catch (IllegalArgumentException esperado) { }
                if (carrinho.getItens().get(0).getQuantidade() != 1) throw new AssertionError("Carrinho alterado");
            }
            case "falhaCheckout", "falhaCupom" -> {
                Path destino = pasta.resolve(caso.equals("falhaCheckout") ? "pedidos.csv" : "cupons.csv");
                Files.delete(destino);
                Files.createDirectory(destino);
                Files.writeString(destino.resolve("preservar"), "original");
                String itensAntes = Files.readString(pasta.resolve("itens_pedidos.csv"));
                Carrinho carrinho = carrinho(produto);
                Cupom cupom = cupom(10);
                carrinho.aplicarCupom(cupom);
                try {
                    vendas.finalizarPedido("PED1", cliente, carrinho, new PagamentoPix(), true);
                    throw new AssertionError("Checkout confirmou falha de gravacao");
                } catch (java.io.UncheckedIOException esperado) { }
                valor(cliente.getSaldoCashback(), 20, "Saldo restaurado");
                if (cliente.getPontosFidelidade() != 0 || produto.getQuantidadeEstoque() != 10
                        || cupom.foiUtilizadoPor("C01") || carrinho.getItens().isEmpty()
                        || vendas.buscarPedido("PED1") != null) {
                    throw new AssertionError("Checkout deixou alteracoes parciais");
                }
                valor(new ClienteRepository(pasta.resolve("clientes.csv").toString())
                        .buscarPorId("C01").getSaldoCashback(), 20, "Saldo em disco restaurado");
                if (new ProdutoRepository(pasta.resolve("produtos.csv").toString())
                        .buscarPorId("P01").getQuantidadeEstoque() != 10) {
                    throw new AssertionError("Estoque em disco nao restaurado");
                }
                if (!Files.readString(pasta.resolve("itens_pedidos.csv")).equals(itensAntes)) {
                    throw new AssertionError("Itens de pedido inexistente ficaram gravados");
                }
            }
            case "cupomAlterado" -> {
                vendas.getCupomRepository().salvar(cupom(10));
                Carrinho carrinho = carrinho(produto);
                carrinho.aplicarCupom(cupom(90));
                try {
                    vendas.finalizarPedido("PED1", cliente, carrinho, new PagamentoBoleto());
                    throw new AssertionError("Checkout aceitou valor diferente do cupom cadastrado");
                } catch (IllegalArgumentException | IllegalStateException esperado) { }
                if (produto.getQuantidadeEstoque() != 10) throw new AssertionError("Estoque alterado");
            }
            case "clienteIdBooleano" -> {
                Cupom utilizado = cupom(10);
                utilizado.marcarComoUtilizado("TRUE");
                utilizado.marcarComoUtilizado("FALSE");
                vendas.getCupomRepository().salvar(utilizado);
                Cupom relido = new CupomRepository(pasta.resolve("cupons.csv").toString()).buscarPorCodigo("UNICO");
                if (relido.isValidoParaCliente("TRUE") || relido.isValidoParaCliente("FALSE")) {
                    throw new AssertionError("IDs TRUE/FALSE perderam o historico de utilizacao");
                }
            }
            case "precisaoCupom" -> {
                for (TipoCupom tipo : TipoCupom.values()) {
                    try {
                        new Cupom("MINIMO", tipo, 0.001, LocalDate.of(2030, 1, 1));
                        throw new AssertionError("Valor que vira zero no CSV foi aceito: " + tipo);
                    } catch (IllegalArgumentException esperado) { }
                }
                Cupom centavos = new Cupom("CENTAVOS", TipoCupom.VALOR_FIXO, 0.015, LocalDate.of(2030, 1, 1));
                valor(centavos.getValor(), 0.02, "Precisao do desconto antes de persistir");
                vendas.getCupomRepository().salvar(centavos);
                valor(new CupomRepository(pasta.resolve("cupons.csv").toString()).buscarPorCodigo("CENTAVOS")
                        .getValor(), 0.02, "Precisao preservada ao reiniciar");
            }
            case "csvLegado" -> {
                Path arquivo = pasta.resolve("cupons.csv");
                Files.writeString(arquivo, "codigo;tipo;valor;dataValidade;utilizado\n"
                        + "LIVRE;PERCENTUAL;10;2030-01-01;false\nUSADO;PERCENTUAL;10;2030-01-01;true\n");
                CupomRepository repositorio = new CupomRepository(arquivo.toString());
                if (!repositorio.buscarPorCodigo("LIVRE").isValidoParaCliente("C01")
                        || repositorio.buscarPorCodigo("USADO").isValidoParaCliente("C01")) {
                    throw new AssertionError("Leitura liberou cupom legado utilizado");
                }
                repositorio.salvarNoArquivo();
                if (new CupomRepository(arquivo.toString()).buscarPorCodigo("USADO").isValido()) {
                    throw new AssertionError("Migracao perdeu bloqueio legado");
                }
            }
        }
    }

    private static Cupom cupom(double valor) {
        return new Cupom("UNICO", TipoCupom.VALOR_FIXO, valor, LocalDate.now().plusDays(1));
    }

    private static Carrinho carrinho(Produto produto) {
        Carrinho carrinho = new Carrinho();
        carrinho.adicionarItem(produto, 1);
        return carrinho;
    }

    private static void valor(double obtido, double esperado, String descricao) {
        if (Math.abs(obtido - esperado) > 0.00001) {
            throw new AssertionError(descricao + ": esperado " + esperado + ", obtido " + obtido);
        }
    }
}
