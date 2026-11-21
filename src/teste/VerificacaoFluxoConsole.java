package teste;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/** Executa o Main em processos separados, com CSVs exclusivos do teste. */
public class VerificacaoFluxoConsole {
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("shoponline-console-");
        try {
            String compra = "3\n5\nC02\n\n6\n2\nTESTE10\n1\nInfinity\n10\n2030-01-01\n\n"
                    + "2\nP03\n1\n\n6\n3\nTESTE10\n\n7\n1\nS\n\n0\n0\n";
            String saida = executar(pasta, compra);
            if (!saida.contains("COMPRA FINALIZADA COM SUCESSO")) {
                throw new AssertionError("Checkout pelo console nao concluiu: " + saida);
            }
            Path arquivo = pasta.resolve("data/pedidos.csv");
            var pedidos = Files.readAllLines(arquivo);
            if (pedidos.size() != 2) throw new AssertionError("Pedido nao persistido");
            String[] pedido = pedidos.get(1).split(";");
            if (Double.parseDouble(pedido[4]) != 70.0 || !pedido[5].equals("PIX")) {
                throw new AssertionError("VIP, cupom, PIX ou cashback incorretos: " + pedidos.get(1));
            }
            String consulta = executar(pasta, "5\n\n0\n");
            if (!consulta.contains(pedido[0])) throw new AssertionError("Relatorio perdeu venda apos reinicio");
            executar(pasta, "4\n3\n" + pedido[0] + "\n\n0\n0\n");
            if (!Files.readString(arquivo).contains(";CANCELADO;")) {
                throw new AssertionError("Cancelamento pelo menu nao persistido");
            }
            String produto = Files.readAllLines(pasta.resolve("data/produtos.csv")).stream()
                    .filter(linha -> linha.startsWith("P03;")).findFirst().orElseThrow();
            if (Integer.parseInt(produto.split(";")[4]) != 15) {
                throw new AssertionError("Cancelamento nao devolveu estoque");
            }
            System.out.println("PASSOU: console, selecao de cliente, emissao e aplicacao de cupom, VIP, PIX, cashback, reinicio e cancelamento.");
        } finally {
            try (var arquivos = Files.walk(pasta)) {
                for (Path arquivo : arquivos.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(arquivo);
            }
        }
    }

    private static String executar(Path pasta, String entrada) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = Path.of(VerificacaoFluxoConsole.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).toString();
        Path log = pasta.resolve("console.log");
        Process processo = new ProcessBuilder(java, "-cp", classpath, "Main")
                .directory(pasta.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            try (var stdin = processo.getOutputStream()) { stdin.write(entrada.getBytes(StandardCharsets.UTF_8)); }
            if (!processo.waitFor(10, TimeUnit.SECONDS)) throw new AssertionError("Console nao encerrou");
            String saida = Files.readString(log);
            if (processo.exitValue() != 0) throw new AssertionError("Main falhou: " + saida);
            return saida;
        } finally {
            if (processo.isAlive()) processo.destroyForcibly().waitFor();
        }
    }
}
