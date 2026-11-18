package teste;

import model.Cliente;
import model.ClienteVip;

public class VerificacaoCadastroCliente {
    public static void main(String[] args) {
        rejeitar(() -> new Cliente("C01", "João Monteiro", "joao@email.com", -1, 0.0));
        rejeitar(() -> new ClienteVip("C02", "Vitória Salgado", "vitoria@email.com", -1, 0.0));
        for (double saldoInvalido : new double[]{-0.01, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY}) {
            rejeitar(() -> new Cliente("C01", "João Monteiro", "joao@email.com", 0, saldoInvalido));
            rejeitar(() -> new ClienteVip("C02", "Vitória Salgado", "vitoria@email.com", 0, saldoInvalido));
        }

        for (String invalido : new String[]{null, "", "   ", "texto;extra", "texto\nextra", "texto\rextra"}) {
            rejeitar(() -> new Cliente(invalido, "João Monteiro", "joao@email.com"));
            rejeitar(() -> new Cliente("C01", invalido, "joao@email.com"));
            rejeitar(() -> new ClienteVip("C02", invalido, "vitoria@email.com", 1600, 120.0));
        }

        for (String email : new String[]{null, "", "   ", "sem-arroba", "@email.com",
                "vitoria@", "vitoria@@email.com", "vi toria@email.com", "vitoria@email.com;extra"}) {
            rejeitar(() -> new Cliente("C01", "João Monteiro", email));
            rejeitar(() -> new ClienteVip("C02", "Vitória Salgado", email));
            rejeitar(() -> new Cliente("C01", "João Monteiro", email, 600, 25.0));
        }

        Cliente cliente = new Cliente(" C01 ", " João Monteiro ", " joao@email.com ", 600, 25.0);
        verificar(cliente.getId().equals("C01"), "ID deve ser aparado.");
        verificar(cliente.getNome().equals("João Monteiro"), "Nome deve ser aparado.");
        verificar(cliente.getEmail().equals("joao@email.com"), "E-mail deve ser aparado.");
        verificar(cliente.getPontosFidelidade() == 600 && cliente.getSaldoCashback() == 25.0,
                "Construtor deve preservar pontos e saldo.");

        rejeitar(() -> cliente.setNome("   "));
        rejeitar(() -> cliente.setEmail("invalido"));
        verificar(cliente.getNome().equals("João Monteiro"), "Nome anterior deve ser preservado.");
        verificar(cliente.getEmail().equals("joao@email.com"), "E-mail anterior deve ser preservado.");
        cliente.setNome(" João Guilherme Monteiro ");
        cliente.setEmail(" joao.monteiro@email.com ");
        verificar(cliente.getNome().equals("João Guilherme Monteiro"), "Setter deve aceitar nome válido.");
        verificar(cliente.getEmail().equals("joao.monteiro@email.com"), "Setter deve aceitar e-mail válido.");

        ClienteVip vip = new ClienteVip("C02", "Vitória Salgado", "vitoria@email.com");
        verificar(vip.isVip(), "Cliente VIP deve manter sua especialização.");
        rejeitar(() -> vip.setNome(null));
        rejeitar(() -> vip.setEmail("invalido"));
        verificar(vip.getPontosFidelidade() == 0 && vip.getSaldoCashback() == 0.0,
                "Construtor simples deve iniciar pontos e saldo zerados.");
        System.out.println("PASSOU: validações cadastrais de clientes comuns e VIP.");
    }

    private static void rejeitar(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Cadastro inválido deveria lançar IllegalArgumentException.");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
