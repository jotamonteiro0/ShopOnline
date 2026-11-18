package service;

import model.ItemCarrinho;
import model.Pedido;
import model.StatusPedido;
import repository.PedidoRepository;

import java.util.ArrayList;
import java.util.List;

/** Centraliza os eventos de pedido e a atualizacao do historico/estoque. */
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final EstoqueService estoqueService;

    public PedidoService(PedidoRepository pedidoRepository, EstoqueService estoqueService) {
        if (pedidoRepository == null || estoqueService == null) {
            throw new IllegalArgumentException("Dependencias de pedidos obrigatorias.");
        }
        this.pedidoRepository = pedidoRepository;
        this.estoqueService = estoqueService;
    }

    /** Integracao: chamado no checkout apos a baixa de estoque e a criacao do pedido. */
    public void registrarPedido(Pedido pedido) {
        if (pedido == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("Pedido precisa conter itens.");
        }
        if (pedido.getStatus() != StatusPedido.EM_PROCESSAMENTO) {
            throw new IllegalArgumentException("Um novo pedido deve estar em processamento.");
        }
        if (pedidoRepository.existePorId(pedido.getId())) {
            throw new IllegalArgumentException("Ja existe um pedido com ID " + pedido.getId());
        }
        pedidoRepository.salvar(pedido);
    }

    public Pedido buscarPedido(String id) {
        return pedidoRepository.buscarPorId(id); }

    public List<Pedido> listarPedidos(){
        return pedidoRepository.listarTodos();
    }

    /** Avanca exclusivamente EM_PROCESSAMENTO -> ENVIADO -> ENTREGUE. */
    public boolean avancarStatus(String id) {
        Pedido atual = pedidoRepository.buscarPorId(id);
        if (atual == null) return false;
        Pedido atualizado = copiarPedido(atual);
        if (!atualizado.avancarStatus()) return false;
        pedidoRepository.salvar(atualizado);
        return true;
    }

    /**
     * Cancela um pedido em processamento e devolve seus itens ao estoque persistido.
     * Se alguma operacao falhar, tenta desfazer os estornos ja realizados.
     * A operacao abrange dois CSVs, portanto nao constitui transacao atomica de banco de dados.
     */
    public boolean cancelarPedido(String id) {
        Pedido atual = pedidoRepository.buscarPorId(id);
        if (atual == null || atual.getStatus() != StatusPedido.EM_PROCESSAMENTO
                || atual.getItens().isEmpty()) return false;

        List<ItemCarrinho> itens = atual.getItens();
        for (ItemCarrinho item : itens) {
            if (estoqueService.buscarProduto(item.getProduto().getId()) == null) return false;
        }

        Pedido cancelado = copiarPedido(atual);
        if (!cancelado.cancelar()) return false;
        List<ItemCarrinho> devolvidos = new ArrayList<>();
        try {
            for (ItemCarrinho item : itens) {
                if (!estoqueService.reporEstoque(item.getProduto().getId(), item.getQuantidade())) {
                    throw new IllegalStateException("Nao foi possivel estornar " + item.getProduto().getId());
                }
                devolvidos.add(item);
            }
            pedidoRepository.salvar(cancelado);
            return true;
        } catch (RuntimeException falha) {
            // Compensacao para nao deixar um cancelamento parcial nas falhas detectaveis.
            for (int i = devolvidos.size() - 1; i >= 0; i--) {
                ItemCarrinho item = devolvidos.get(i);
                if (!estoqueService.darBaixaEstoque(item.getProduto().getId(), item.getQuantidade())) {
                    falha.addSuppressed(new IllegalStateException(
                            "Falha no rollback do estoque: " + item.getProduto().getId()));
                }
            }
            throw falha;
        }
    }

    /** Copia para evitar alterar o objeto anterior se a gravacao do CSV falhar. */
    private static Pedido copiarPedido(Pedido pedido) {
        return new Pedido(pedido.getId(), pedido.getCliente(), pedido.getItens(),
                pedido.getValorSubtotal(), pedido.getDescontoTotal(), pedido.getValorFinal(),
                pedido.getFormaPagamento(), pedido.getCashbackGerado(), pedido.getStatus(),
                pedido.getDataCriacao());
    }
}
