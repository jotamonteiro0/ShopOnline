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
        for (ItemCarrinho item : itens) {
            if (item.getProduto().getId().equals(produto.getId())) {
                item.setQuantidade(item.getQuantidade() + quantidade);
                return;
            }
        }
        itens.add(new ItemCarrinho(produto, quantidade));
    }

    public boolean removerItem(String produtoId) {
        return itens.removeIf(item -> item.getProduto().getId().equals(produtoId));
    }

    public boolean removerQuantidadeItem(String produtoId, int quantidadeParaRemover) {
        if (produtoId == null || quantidadeParaRemover <= 0) {
            return false;
        }
        for (int i = 0; i < itens.size(); i++) {
            ItemCarrinho item = itens.get(i);
            if (item.getProduto().getId().equalsIgnoreCase(produtoId.trim())) {
                if (quantidadeParaRemover > item.getQuantidade()) {
                    return false;
                }
                if (quantidadeParaRemover == item.getQuantidade()) {
                    itens.remove(i);
                } else {
                    item.setQuantidade(item.getQuantidade() - quantidadeParaRemover);
                }
                return true;
            }
        }
        return false;
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

    public boolean aplicarCupom(Cupom cupom, Cliente cliente) {
        if (cupom != null && cliente != null && cupom.isValidoParaCliente(cliente.getId())) {
            this.cupomAplicado = cupom;
            return true;
        }
        return aplicarCupom(cupom);
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
