package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Carrinho {
    private List<ItemCarrinho> itens;
    private Cupom cupomAplicado;

    public Carrinho() {
        this.itens = new ArrayList<>();
    }

    public void adicionarItem(Produto produto, int quantidade) {
        if (produto == null) {
            throw new IllegalArgumentException("O produto nao pode ser nulo.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade adicionada deve ser maior que zero.");
        }
        for (ItemCarrinho item : itens) {
            if (item.getProduto().getId().equalsIgnoreCase(produto.getId())) {
                item.setQuantidade(item.getQuantidade() + quantidade);
                return;
            }
        }
        itens.add(new ItemCarrinho(produto, quantidade));
    }

    public boolean removerItem(String produtoId) {
        if (produtoId == null) return false;
        return itens.removeIf(item -> item.getProduto().getId().equalsIgnoreCase(produtoId.trim()));
    }

    public void limpar() {
        itens.clear();
        cupomAplicado = null;
    }

    public List<ItemCarrinho> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public double getSubtotal() {
        double subtotal = 0;
        for (ItemCarrinho item : itens) {
            subtotal += item.getSubtotal();
        }
        return subtotal;
    }

    public double calcularDescontoVip(Cliente cliente) {
        if (cliente == null || !cliente.isVip()) {
            return 0.0;
        }
        double desconto = 0;
        for (ItemCarrinho item : itens) {
            if (item.getProduto().isPromocaoVip()) {
                desconto += cliente.calcularDescontoVip(item.getProduto()) * item.getQuantidade();
            }
        }
        return desconto;
    }

    public boolean aplicarCupom(Cupom cupom) {
        if (cupom != null && cupom.isValido()) {
            this.cupomAplicado = cupom;
            return true;
        }
        return false;
    }

    public Cupom getCupomAplicado() {
        return cupomAplicado;
    }

    public double calcularDescontoCupom() {
        if (cupomAplicado == null) {
            return 0.0;
        }
        return cupomAplicado.calcularDesconto(getSubtotal());
    }

    public double calcularTotal(Cliente cliente) {
        double total = getSubtotal() - calcularDescontoVip(cliente) - calcularDescontoCupom();
        return Math.max(0.0, total);
    }
}
