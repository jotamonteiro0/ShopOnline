package teste;

import model.Cliente;
import model.ClienteVip;
import model.Produto;

public class VerificacaoDescontoVip {
    public static void main(String[] args) {
        Cliente comum = new Cliente("C01", "João Monteiro", "joao@email.com");
        Cliente vip = new ClienteVip("C02", "Vitória Salgado", "vitoria@email.com");
        Produto promocional = new Produto("P01", "Notebook Gamer", "Notebook", 4500.0, 6, true);
        Produto semPromocao = new Produto("P02", "Mouse Sem Fio", "Mouse", 80.0, 4, false);
        Produto comCentavos = new Produto("P04", "Headset", "Headset", 199.90, 5, true);
        verificar(vip.isVip() && !comum.isVip(), "Tipo de cliente deve ser identificado por polimorfismo.");
        valor(vip.calcularDescontoVip(promocional), 450.0, "VIP deve receber 10% em produto promocional.");
        valor(vip.calcularDescontoVip(comCentavos), 19.99, "Desconto deve considerar centavos.");
        valor(vip.calcularDescontoVip(semPromocao), 0.0, "Produto sem promocao nao deve gerar desconto VIP.");
        valor(comum.calcularDescontoVip(promocional), 0.0, "Cliente comum nao deve receber desconto VIP.");
        valor(comum.calcularDescontoVip(semPromocao), 0.0, "Cliente comum nao deve receber desconto em produto normal.");
        valor(vip.calcularDescontoVip(null), 0.0, "Produto ausente nao deve gerar desconto.");
        promocional.setPromocaoVip(false);
        valor(vip.calcularDescontoVip(promocional), 0.0, "Remover promocao deve encerrar desconto VIP.");
        System.out.println("PASSOU: desconto VIP de 10%, centavos, produto sem promocao e cliente comum.");
    }

    private static void valor(double recebido, double esperado, String mensagem) {
        verificar(Math.abs(recebido - esperado) < 0.000001, mensagem);
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
