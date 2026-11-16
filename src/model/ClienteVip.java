package model;

public class ClienteVip extends Cliente {
    private static final double DESCONTO_PROMOCAO_VIP = 0.10; // 10% de desconto

    public ClienteVip(String id, String nome, String email) {
        super(id, nome, email);
    }

    public ClienteVip(String id, String nome, String email, int pontosFidelidade, double saldoCashback) {
        super(id, nome, email, pontosFidelidade, saldoCashback);
    }

    @Override
    public boolean isVip() {
        return true;
    }

    @Override
    public double calcularDescontoVip(Produto produto) {
        if (produto != null && produto.isPromocaoVip()) {
            return produto.getPreco() * DESCONTO_PROMOCAO_VIP;
        }
        return 0.0;
    }
}
