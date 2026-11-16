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
        return valorCompra * nivel.getPercentualCashback();
    }

    public void processarFidelidadeECashback(Cliente cliente, double valorCompra) {
        int pontosGanhos = calcularPontosGanhos(valorCompra);
        double cashbackGanho = calcularCashback(cliente, valorCompra);

        cliente.adicionarPontos(pontosGanhos);
        cliente.creditarCashback(cashbackGanho);

        clienteRepository.salvar(cliente);
    }
}
