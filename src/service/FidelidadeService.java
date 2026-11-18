package service;

import model.Cliente;
import model.NivelFidelidade;
import repository.ClienteRepository;

public class FidelidadeService {
    private final ClienteRepository clienteRepository;

    public FidelidadeService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public int calcularPontosGanhos(double valorCompra) {
        if (valorCompra > 100.0) {
            return (int) (valorCompra / 10.0);
        }
        return 0;
    }

    public double calcularCashback(Cliente cliente, double valorCompra) {
        NivelFidelidade nivel = cliente.getNivelFidelidade();
        return arredondarCentavos(valorCompra * nivel.getPercentualCashback());
    }

    public void processarFidelidadeECashback(Cliente cliente, double valorCompra) {
        int pontosGanhos = calcularPontosGanhos(valorCompra);
        double cashbackGanho = calcularCashback(cliente, valorCompra);

        cliente.adicionarPontos(pontosGanhos);
        cliente.creditarCashback(cashbackGanho);

        clienteRepository.salvar(cliente);
    }
    public double usarCashback(Cliente cliente, double valorCompra) {
        if (!Double.isFinite(valorCompra) || valorCompra < 0) {
            throw new IllegalArgumentException("Valor da compra inválido.");
        }

        double saldo = cliente.getSaldoCashback();
        if (!Double.isFinite(saldo) || saldo <= 0 || valorCompra == 0) {
            return 0.0;
        }

        double credito = arredondarCentavos(Math.min(saldo, valorCompra));
        if (credito <= 0 || !cliente.debitarCashback(Math.min(credito, saldo))) {
            return 0.0;
        }

        clienteRepository.salvar(cliente);
        return credito;
    }

    static double arredondarCentavos(double valor) {
        return java.math.BigDecimal.valueOf(valor)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
