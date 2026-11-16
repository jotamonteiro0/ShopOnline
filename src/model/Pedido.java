package model;

import model.pagamento.FormaPagamento;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private String id;
    private Cliente cliente;
    private List<ItemCarrinho> itens;
    private double valorSubtotal;
    private double descontoTotal;
    private double valorFinal;
    private FormaPagamento formaPagamento;
    private StatusPedido status;
    private LocalDateTime dataCriacao;
    private double cashbackGerado;

    public Pedido(String id, Cliente cliente, List<ItemCarrinho> itens, double valorSubtotal,
                  double descontoTotal, double valorFinal, FormaPagamento formaPagamento, double cashbackGerado) {
        this.id = id;
        this.cliente = cliente;
        this.itens = new ArrayList<>(itens);
        this.valorSubtotal = valorSubtotal;
        this.descontoTotal = descontoTotal;
        this.valorFinal = valorFinal;
        this.formaPagamento = formaPagamento;
        this.status = StatusPedido.EM_PROCESSAMENTO;
        this.dataCriacao = LocalDateTime.now();
        this.cashbackGerado = cashbackGerado;
    }

    public String getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public double getValorSubtotal() {
        return valorSubtotal;
    }

    public double getDescontoTotal() {
        return descontoTotal;
    }

    public double getValorFinal() {
        return valorFinal;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public double getCashbackGerado() {
        return cashbackGerado;
    }

    public boolean cancelar() {
        if (this.status == StatusPedido.EM_PROCESSAMENTO) {
            this.status = StatusPedido.CANCELADO;
            for (ItemCarrinho item : itens) {
                item.getProduto().adicionarEstoque(item.getQuantidade());
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return String.format("Pedido #%s | Data: %s | Cliente: %s | Total: R$ %.2f | Status: %s | Pagamento: %s",
                id, dataCriacao.format(dtf), cliente.getNome(), valorFinal, status, formaPagamento.getDescricao());
    }
}
