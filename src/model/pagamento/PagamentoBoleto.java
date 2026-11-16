package model.pagamento;

public class PagamentoBoleto implements FormaPagamento {
    @Override
    public String getNome() {
        return "BOLETO";
    }

    @Override
    public double calcularValorFinal(double valorTotal) {
        return valorTotal;
    }

    @Override
    public String getDescricao() {
        return "Boleto Bancário (vencimento em 3 dias úteis)";
    }
}
