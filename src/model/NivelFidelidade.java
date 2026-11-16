package model;

public enum NivelFidelidade {
    BRONZE(0.02, 0, 499),
    PRATA(0.05, 500, 1499),
    OURO(0.08, 1500, Integer.MAX_VALUE);

    private final double percentualCashback;
    private final int pontosMinimos;
    private final int pontosMaximos;

    NivelFidelidade(double percentualCashback, int pontosMinimos, int pontosMaximos) {
        this.percentualCashback = percentualCashback;
        this.pontosMinimos = pontosMinimos;
        this.pontosMaximos = pontosMaximos;
    }

    public double getPercentualCashback() {
        return percentualCashback;
    }

    public static NivelFidelidade obterPorPontos(int pontos) {
        if (pontos >= 1500) {
            return OURO;
        } else if (pontos >= 500) {
            return PRATA;
        } else {
            return BRONZE;
        }
    }
}
