package model;

import model.pagamento.FormaPagamento;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Representa um pedido e controla suas transicoes de estado. */
public class Pedido {
    private final String id;
    private final Cliente cliente;
    private final List<ItemCarrinho> itens;
    private final double valorSubtotal;
    private final double descontoTotal;
    private final double valorFinal;
    private final FormaPagamento formaPagamento;
    private StatusPedido status;
    private final LocalDateTime dataCriacao;
    private final double cashbackGerado;

    public Pedido(String id, Cliente cliente, List<ItemCarrinho> itens, double valorSubtotal,
                  double descontoTotal, double valorFinal, FormaPagamento formaPagamento, double cashbackGerado) {
        this(id, cliente, itens, valorSubtotal, descontoTotal, valorFinal, formaPagamento,
                cashbackGerado, StatusPedido.EM_PROCESSAMENTO, LocalDateTime.now());
    }

    /** Utilizado pelo repositorio para reconstruir um pedido persistido. */
    public Pedido(String id, Cliente cliente, List<ItemCarrinho> itens, double valorSubtotal,
                  double descontoTotal, double valorFinal, FormaPagamento formaPagamento,
                  double cashbackGerado, StatusPedido status, LocalDateTime dataCriacao) {
        if (id == null || id.isBlank() || id.contains(";") || id.contains("\n") || id.contains("\r")) {
            throw new IllegalArgumentException("ID do pedido invalido.");
        }
        this.id = id;
        this.cliente = Objects.requireNonNull(cliente, "Cliente obrigatorio.");
        Objects.requireNonNull(itens, "Lista de itens obrigatoria.");
        this.itens = new ArrayList<>();
        for (ItemCarrinho item : itens) {
            if (item == null || item.getProduto() == null || item.getQuantidade() <= 0) {
                throw new IllegalArgumentException("Item do pedido invalido.");
            }
            this.itens.add(copiarItem(item));
        }
        if (!Double.isFinite(valorSubtotal) || !Double.isFinite(descontoTotal)
                || !Double.isFinite(valorFinal) || !Double.isFinite(cashbackGerado)
                || valorSubtotal < 0 || descontoTotal < 0 || valorFinal < 0 || cashbackGerado < 0) {
            throw new IllegalArgumentException("Valores monetarios invalidos no pedido.");
        }
        this.valorSubtotal = valorSubtotal;
        this.descontoTotal = descontoTotal;
        this.valorFinal = valorFinal;
        this.formaPagamento = Objects.requireNonNull(formaPagamento, "Pagamento obrigatorio.");
        this.status = Objects.requireNonNull(status, "Status obrigatorio.");
        this.dataCriacao = Objects.requireNonNull(dataCriacao, "Data obrigatoria.");
        this.cashbackGerado = cashbackGerado;
    }

    public String getId() { return id; }
    public Cliente getCliente() { return cliente; }
    /** Protege quantidades e precos historicos contra alteracoes externas. */
    public List<ItemCarrinho> getItens() {
        List<ItemCarrinho> copia = new ArrayList<>();
        for (ItemCarrinho item : itens) copia.add(copiarItem(item));
        return Collections.unmodifiableList(copia);
    }

    private static ItemCarrinho copiarItem(ItemCarrinho item) {
        Produto p = item.getProduto();
        Produto fotografia = new Produto(p.getId(), p.getNome(), p.getDescricao(),
                p.getPreco(), 0, p.isPromocaoVip());
        return new ItemCarrinho(fotografia, item.getQuantidade());
    }
    public double getValorSubtotal() { return valorSubtotal; }
    public double getDescontoTotal() { return descontoTotal; }
    public double getValorFinal() { return valorFinal; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public StatusPedido getStatus() { return status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public double getCashbackGerado() { return cashbackGerado; }

    /** Impede pular etapas e alterar pedidos cancelados ou entregues. */
    public void setStatus(StatusPedido novoStatus) {
        Objects.requireNonNull(novoStatus, "Status obrigatorio.");
        if ((status == StatusPedido.EM_PROCESSAMENTO && novoStatus == StatusPedido.ENVIADO)
                || (status == StatusPedido.ENVIADO && novoStatus == StatusPedido.ENTREGUE)) {
            status = novoStatus;
            return;
        }
        throw new IllegalStateException("Transicao invalida: " + status + " -> " + novoStatus);
    }

    public boolean avancarStatus() {
        if (status == StatusPedido.EM_PROCESSAMENTO) {
            setStatus(StatusPedido.ENVIADO);
            return true;
        }
        if (status == StatusPedido.ENVIADO) {
            setStatus(StatusPedido.ENTREGUE);
            return true;
        }
        return false;
    }

    /** Atualiza apenas o estado; o PedidoService executa o estorno no estoque e nos CSVs. */
    public boolean cancelar() {
        if (status != StatusPedido.EM_PROCESSAMENTO || itens.isEmpty()) {
            return false;
        }
        status = StatusPedido.CANCELADO;
        return true;
    }

    @Override
    public String toString() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return String.format("Pedido #%s | Data: %s | Cliente: %s | Total: R$ %.2f | Status: %s | Pagamento: %s",
                id, dataCriacao.format(formato), cliente.getNome(), valorFinal, status, formaPagamento.getDescricao());
    }
}
