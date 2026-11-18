package teste;

import model.Cliente;
import model.ClienteVip;
import repository.ClienteRepository;
import view.MenuClientes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;

public class VerificacaoFalhaGravacaoClientes {
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("shoponline-falha-clientes-");
        try {
            Path bloqueio = Files.writeString(pasta.resolve("bloqueio"), "arquivo");
            ClienteRepository indisponivel = new ClienteRepository(bloqueio.resolve("clientes.csv").toString());
            Cliente joao = new Cliente("C01", "João Monteiro", "joao@email.com");
            falhar(() -> indisponivel.cadastrar(joao));
            verificar(indisponivel.listarTodos().isEmpty(), "Cadastro sem gravacao deve ser desfeito na memoria.");

            Path arquivo = pasta.resolve("clientes.csv");
            ClienteRepository repositorio = new ClienteRepository(arquivo.toString());
            repositorio.cadastrar(joao);
            String original = Files.readString(arquivo);
            Files.delete(arquivo);
            Files.createDirectory(arquivo);
            Path marcador = Files.writeString(arquivo.resolve("preservar.txt"), original);

            Cliente vip = new ClienteVip("C02", "Vitória Salgado", "vitoria@email.com");
            falhar(() -> repositorio.cadastrar(vip));
            verificar(repositorio.buscarPorId("C02") == null, "Falha na substituicao do CSV deve desfazer novo cadastro.");
            falhar(() -> repositorio.salvar(new Cliente("C01", "João Atualizado", "joao.novo@email.com")));
            verificar(repositorio.buscarPorId("C01") == joao, "Falha deve restaurar o cliente anterior no mapa.");
            verificar(Files.readString(marcador).equals(original), "Falha nao deve sobrescrever o destino existente.");
            try (var arquivos = Files.list(pasta)) {
                verificar(arquivos.noneMatch(p -> p.getFileName().toString().endsWith(".csv.tmp")),
                        "Arquivos temporarios devem ser removidos apos falha.");
            }

            Files.delete(marcador);
            Files.delete(arquivo);
            Files.writeString(arquivo, original);
            repositorio.cadastrar(vip);
            verificar(new ClienteRepository(arquivo.toString()).listarTodos().size() == 2,
                    "Cadastro deve funcionar novamente apos corrigir o destino.");
            verificarMenu(indisponivel);
            System.out.println("PASSOU: falhas de gravacao, restauracao do mapa, limpeza, recuperacao e menu.");
        } finally {
            try (var arquivos = Files.walk(pasta)) {
                for (Path arquivo : arquivos.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(arquivo);
                }
            }
        }
    }

    private static void verificarMenu(ClienteRepository repositorio) {
        InputStream entradaOriginal = System.in;
        PrintStream saidaOriginal = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        String entradas = String.join("\n", "1", "C01", "João Monteiro", "joao@email.com", "", "3", "", "0", "");
        try (PrintStream captura = new PrintStream(saida, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(entradas.getBytes(StandardCharsets.UTF_8)));
            System.setOut(captura);
            new MenuClientes(repositorio).exibirMenu();
        } finally {
            System.setIn(entradaOriginal);
            System.setOut(saidaOriginal);
        }
        String texto = saida.toString(StandardCharsets.UTF_8);
        verificar(texto.contains("Não foi possível salvar o cliente"), "Menu deve informar falha de gravacao.");
        verificar(!texto.contains("Cliente adicionado à lista."), "Menu nao deve anunciar sucesso apos falha.");
        verificar(texto.contains("Nenhum cliente cadastrado."), "Menu deve continuar navegavel sem cadastro fantasma.");
    }

    private static void falhar(Runnable acao) {
        try {
            acao.run();
        } catch (UncheckedIOException e) {
            return;
        }
        throw new AssertionError("Falha de gravacao deveria lancar UncheckedIOException.");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
