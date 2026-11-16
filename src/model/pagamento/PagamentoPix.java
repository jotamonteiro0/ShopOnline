package model.pagamento;

public class PagamentoPix implements FormaPagamento {
    private static final double DESCONTO_PIX = 0.05; // 5% de desconto

    @Override
    public String getNome() {
        return "PIX";
    }

    @Override
    public double calcularValorFinal(double valorTotal) {
        return valorTotal * (1.0 - DESCONTO_PIX);
    }

    @Override
    public String getDescricao() {
        return "Pagamento via PIX (5% de desconto aplicado)";
    }
}
