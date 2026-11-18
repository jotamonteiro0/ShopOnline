package teste;

import model.Cliente;
import model.ClienteVip;
import repository.ClienteRepository;

import java.nio.file.Files;
import java.nio.file.Path;

public class VerificacaoCadastroRepositorioCliente {
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("shoponline-cadastro-clientes-");
        Path arquivo = pasta.resolve("clientes.csv");
        try {
            ClienteRepository repositorio = new ClienteRepository(arquivo.toString());
            Cliente comum = new Cliente("C01", "Maria", "maria@email.com");
            ClienteVip vip = new ClienteVip("C02", "Ana", "ana@email.com");
            repositorio.cadastrar(comum);
            repositorio.cadastrar(vip);

            String antes = Files.readString(arquivo);
            rejeitar(() -> repositorio.cadastrar(new ClienteVip("C01", "Outra", "outra@email.com")));
            rejeitar(() -> repositorio.cadastrar(new Cliente("C02", "Outra", "outra@email.com")));
            rejeitar(() -> repositorio.cadastrar(null));
            verificar(repositorio.buscarPorId("C01") == comum, "Duplicidade deve preservar o cliente comum.");
            verificar(repositorio.buscarPorId("C02") == vip, "Duplicidade deve preservar o cliente VIP.");
            verificar(Files.readString(arquivo).equals(antes), "Cadastro rejeitado deve preservar o CSV.");

            ClienteRepository recarregado = new ClienteRepository(arquivo.toString());
            verificar(recarregado.listarTodos().size() == 2, "Dois clientes devem ser persistidos.");
            verificar(!recarregado.buscarPorId("C01").isVip(), "Cliente comum deve continuar comum.");
            verificar(recarregado.buscarPorId("C02") instanceof ClienteVip, "VIP deve ser reconstruido como ClienteVip.");

            comum.adicionarPontos(600);
            comum.creditarCashback(25.0);
            repositorio.salvar(comum);
            Cliente atualizado = new ClienteRepository(arquivo.toString()).buscarPorId("C01");
            verificar(atualizado.getPontosFidelidade() == 600, "Salvar deve continuar atualizando pontos.");
            verificar(atualizado.getSaldoCashback() == 25.0, "Salvar deve continuar atualizando cashback.");
            System.out.println("PASSOU: cadastro, duplicidade, persistencia comum/VIP e atualizacoes de clientes.");
        } finally {
            Files.deleteIfExists(arquivo);
            Files.deleteIfExists(pasta);
        }
    }

    private static void rejeitar(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Cadastro deveria ser rejeitado.");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
