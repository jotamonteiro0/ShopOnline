package model.pagamento;

public interface FormaPagamento {
    String getNome();
    double calcularValorFinal(double valorTotal);
    String getDescricao();
}
