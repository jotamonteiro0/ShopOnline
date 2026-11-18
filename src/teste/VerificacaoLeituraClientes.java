package teste;

import repository.ClienteRepository;
import model.Cliente;
import model.ClienteVip;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VerificacaoLeituraClientes {
    private static final String CABECALHO = "id;nome;email;pontos;saldoCashback;vip\n";

    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("shoponline-leitura-clientes-");
        Path arquivo = pasta.resolve("clientes.csv");
        PrintStream erroOriginal = System.err;
        ByteArrayOutputStream avisos = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(avisos, true, StandardCharsets.UTF_8)) {
            System.setErr(captura);
            String valido = "C01;João Monteiro;joao@email.com;600;25.00;false\n";
            String vip = "C02;Vitória Salgado;vitoria@email.com;2296;678.14;true\n";
            String[] invalidos = {
                    "CX;;teste@email.com;0;0.00;false",
                    "CX;Teste;invalido;0;0.00;false",
                    "CX;Teste;teste@email.com;abc;0.00;false",
                    "CX;Teste;teste@email.com;-1;0.00;false",
                    "CX;Teste;teste@email.com;0;-1.00;false",
                    "CX;Teste;teste@email.com;0;NaN;false",
                    "CX;Teste;teste@email.com;0;Infinity;false",
                    "CX;Teste;teste@email.com;0;0.00;talvez",
                    "CX;Teste;teste@email.com;0;0.00;",
                    "CX;Teste;teste@email.com;0;0.00;false;extra",
                    "CX;Teste",
                    "C01;Outro;outro@email.com;0;0.00;true"
            };
            for (String invalido : invalidos) {
                avisos.reset();
                String conteudo = CABECALHO + valido + invalido + "\n\n" + vip;
                Files.writeString(arquivo, conteudo);
                ClienteRepository repo = new ClienteRepository(arquivo.toString());
                verificar(repo.listarTodos().size() == 2, "Linha invalida nao deve impedir leitura das seguintes.");
                verificar(repo.buscarPorId("C01").getNome().equals("João Monteiro"), "ID repetido nao deve substituir cliente.");
                verificar(repo.buscarPorId("C02") instanceof ClienteVip, "VIP posterior deve ser reconstruido.");
                verificar(avisos.toString(StandardCharsets.UTF_8).contains("Linha 3"), "Aviso deve identificar linha invalida.");
                try {
                    repo.cadastrar(new Cliente("C03", "Lucas Silva", "lucas@email.com"));
                    throw new AssertionError("CSV com erros nao deve ser sobrescrito.");
                } catch (UncheckedIOException esperado) {
                    verificar(repo.buscarPorId("C03") == null, "Cadastro bloqueado nao deve permanecer no mapa.");
                }
                verificar(Files.readString(arquivo).equals(conteudo), "CSV original deve ser preservado.");
                Files.writeString(arquivo, CABECALHO + vip);
                repo.carregarDoArquivo();
                verificar(repo.buscarPorId("C01") == null, "Recarga nao deve manter clientes removidos do CSV.");
                repo.cadastrar(new Cliente("C03", "Lucas Silva", "lucas@email.com"));
                verificar(new ClienteRepository(arquivo.toString()).listarTodos().size() == 2,
                        "Gravacao deve ser liberada apos corrigir e recarregar o CSV.");
            }
            Files.writeString(arquivo, "cabecalho errado\n" + valido);
            ClienteRepository repo = new ClienteRepository(arquivo.toString());
            try {
                repo.salvarNoArquivo();
                throw new AssertionError("Cabecalho invalido deve bloquear regravacao.");
            } catch (UncheckedIOException esperado) {
                verificar(Files.readString(arquivo).startsWith("cabecalho errado"), "Cabecalho original deve ser preservado.");
            }
        } finally {
            System.setErr(erroOriginal);
            Files.deleteIfExists(arquivo);
            Files.deleteIfExists(pasta);
        }
        System.out.println("PASSOU: leitura resiliente, validacoes CSV, preservacao e recarga de clientes.");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
