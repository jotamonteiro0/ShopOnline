package teste;

import model.Cliente;
import model.ClienteVip;

public class VerificacaoCadastroCliente {
    public static void main(String[] args) {
        for (String invalido : new String[]{null, "", "   ", "texto;extra", "texto\nextra", "texto\rextra"}) {
            rejeitar(() -> new Cliente(invalido, "Vitória", "vitoria@email.com"));
            rejeitar(() -> new Cliente("C01", invalido, "vitoria@email.com"));
            rejeitar(() -> new ClienteVip("C01", invalido, "vitoria@email.com", 600, 25.0));
        }

        for (String email : new String[]{null, "", "   ", "sem-arroba", "@email.com",
                "vitoria@", "vitoria@@email.com", "vi toria@email.com", "vitoria@email.com;extra"}) {
            rejeitar(() -> new Cliente("C01", "Vitória", email));
            rejeitar(() -> new ClienteVip("C01", "Vitória", email));
            rejeitar(() -> new Cliente("C01", "Vitória", email, 600, 25.0));
        }

        Cliente cliente = new Cliente(" C01 ", " Vitória Salgado ", " vitoria@email.com ", 600, 25.0);
        verificar(cliente.getId().equals("C01"), "ID deve ser aparado.");
        verificar(cliente.getNome().equals("Vitória Salgado"), "Nome deve ser aparado.");
        verificar(cliente.getEmail().equals("vitoria@email.com"), "E-mail deve ser aparado.");
        verificar(cliente.getPontosFidelidade() == 600 && cliente.getSaldoCashback() == 25.0,
                "Construtor deve preservar pontos e saldo.");

        rejeitar(() -> cliente.setNome("   "));
        rejeitar(() -> cliente.setEmail("invalido"));
        verificar(cliente.getNome().equals("Vitória Salgado"), "Nome anterior deve ser preservado.");
        verificar(cliente.getEmail().equals("vitoria@email.com"), "E-mail anterior deve ser preservado.");
        cliente.setNome(" Maria Vitória ");
        cliente.setEmail(" maria@email.com ");
        verificar(cliente.getNome().equals("Maria Vitória"), "Setter deve aceitar nome válido.");
        verificar(cliente.getEmail().equals("maria@email.com"), "Setter deve aceitar e-mail válido.");

        ClienteVip vip = new ClienteVip("C02", "Vitória", "vitoria@email.com");
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
