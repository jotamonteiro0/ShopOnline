package model.pagamento;

public class PagamentoCartao implements FormaPagamento {
    private int parcelas;

    public PagamentoCartao(int parcelas) {
        if (parcelas < 1) {
            this.parcelas = 1;
        } else if (parcelas > 10) {
            this.parcelas = 10;
        } else {
            this.parcelas = parcelas;
        }
    }

    public int getParcelas() {
        return parcelas;
    }

    @Override
    public String getNome() {
        return "CARTAO_CREDITO";
    }

    @Override
    public double calcularValorFinal(double valorTotal) {
        return valorTotal; // Sem juros em até 10x
    }

    @Override
    public String getDescricao() {
        return String.format("Cartão de Crédito em %dx sem juros", parcelas);
    }
}
