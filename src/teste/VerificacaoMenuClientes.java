package teste;

import model.Cliente;
import model.ClienteVip;
import repository.ClienteRepository;
import view.MenuClientes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VerificacaoMenuClientes {
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("shoponline-menu-clientes-");
        Path arquivo = pasta.resolve("clientes.csv");
        InputStream entradaOriginal = System.in;
        PrintStream saidaOriginal = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        String entradas = String.join("\n",
                "3", "",
                "texto", "9",
                "1", "C01", "João Monteiro", "joao@email.com", "",
                "2", "C02", "Vitória Salgado", "vitoria@email.com", "",
                "1", "C01", "",
                "1", "C03", "Lucas Silva", "invalido", "",
                "3", "", "0", "");

        try (PrintStream captura = new PrintStream(saida, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(entradas.getBytes(StandardCharsets.UTF_8)));
            System.setOut(captura);
            ClienteRepository repositorio = new ClienteRepository(arquivo.toString());
            new MenuClientes(repositorio).exibirMenu();

            ClienteRepository recarregado = new ClienteRepository(arquivo.toString());
            verificar(recarregado.listarTodos().size() == 2,
                    "Somente os dois cadastros validos devem ser persistidos.");
            Cliente comum = recarregado.buscarPorId("C01");
            Cliente vip = recarregado.buscarPorId("C02");
            verificar(comum != null && !comum.isVip()
                            && comum.getNome().equals("João Monteiro")
                            && comum.getEmail().equals("joao@email.com"),
                    "Cadastro comum deve preservar identidade e rejeitar substituicao por ID repetido.");
            verificar(vip instanceof ClienteVip
                            && vip.getNome().equals("Vitória Salgado")
                            && vip.getEmail().equals("vitoria@email.com"),
                    "Cadastro VIP deve preservar identidade e especializacao.");
            verificar(comum.getPontosFidelidade() == 0 && comum.getSaldoCashback() == 0.0
                            && vip.getPontosFidelidade() == 0 && vip.getSaldoCashback() == 0.0,
                    "Novos clientes devem iniciar sem pontos nem cashback.");
            verificar(recarregado.buscarPorId("C03") == null,
                    "E-mail invalido nao deve resultar em cadastro.");
            String texto = saida.toString(StandardCharsets.UTF_8);
            verificar(texto.contains("Nenhum cliente cadastrado."), "Listagem vazia deve ser informada.");
            verificar(texto.contains("[C01]") && texto.contains("João Monteiro")
                            && texto.contains("[C02]") && texto.contains("Vitória Salgado"),
                    "Listagem deve exibir IDs e nomes dos clientes.");
            verificar(texto.contains("Já existe um cliente com esse ID."),
                    "ID repetido deve ser informado ao usuario.");
        } finally {
            System.setIn(entradaOriginal);
            System.setOut(saidaOriginal);
            Files.deleteIfExists(arquivo);
            Files.deleteIfExists(pasta);
        }
        System.out.println("PASSOU: menu de clientes, cadastros comum/VIP, validacoes, listagem e retorno.");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
