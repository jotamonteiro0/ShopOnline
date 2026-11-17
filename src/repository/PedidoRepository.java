package repository;

import model.Cliente;
import model.ItemCarrinho;
import model.Pedido;
import model.Produto;
import model.StatusPedido;
import model.pagamento.FormaPagamento;
import model.pagamento.PagamentoBoleto;
import model.pagamento.PagamentoCartao;
import model.pagamento.PagamentoPix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Persiste o historico dos pedidos, preservando o formato CSV acordado pela equipe. */
public class PedidoRepository implements Repositorio<Pedido, String> {
    private static final String CABECALHO_PEDIDOS =
            "id;clienteId;subtotal;desconto;totalFinal;formaPagamento;status;dataCriacao;cashbackGerado";
    private static final String CABECALHO_ITENS = "pedidoId;produtoId;quantidade;precoUnitario";

    private final Path arquivoPedidos;
    private final Path arquivoItens;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final Map<String, Pedido> pedidos = new LinkedHashMap<>();

    public PedidoRepository(String caminhoPedidos, ClienteRepository clienteRepository,
                            ProdutoRepository produtoRepository) {
        if (caminhoPedidos == null || caminhoPedidos.isBlank()) {
            throw new IllegalArgumentException("Caminho do CSV obrigatorio.");
        }
        if (clienteRepository == null || produtoRepository == null) {
            throw new IllegalArgumentException("Repositorios de clientes e produtos obrigatorios.");
        }
        this.arquivoPedidos = Path.of(caminhoPedidos);
        this.arquivoItens = arquivoPedidos.resolveSibling("itens_pedidos.csv");
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        carregarDoArquivo();
    }

    @Override
    public List<Pedido> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(pedidos.values()));
    }

    @Override
    public Pedido buscarPorId(String id) { return pedidos.get(id); }

    @Override
    public boolean existePorId(String id) { return pedidos.containsKey(id); }

    @Override
    public void salvar(Pedido pedido) {
        if (pedido == null) throw new IllegalArgumentException("Pedido nao pode ser nulo.");
        Pedido anterior = pedidos.put(pedido.getId(), pedido);
        try {
            salvarNoArquivo();
        } catch (RuntimeException erro) {
            if (anterior == null) pedidos.remove(pedido.getId());
            else pedidos.put(pedido.getId(), anterior);
            throw erro;
        }
    }

    @Override
    public boolean removerPorId(String id) {
        Pedido removido = pedidos.remove(id);
        if (removido == null) return false;
        try {
            salvarNoArquivo();
        } catch (RuntimeException erro) {
            pedidos.put(id, removido);
            throw erro;
        }
        return true;
    }

    @Override
    public void carregarDoArquivo() {
        pedidos.clear();
        criarSeAusente(arquivoPedidos, CABECALHO_PEDIDOS);
        criarSeAusente(arquivoItens, CABECALHO_ITENS);
        Map<String, List<ItemCarrinho>> itensPorPedido = carregarItens();

        try (BufferedReader leitor = Files.newBufferedReader(arquivoPedidos, StandardCharsets.UTF_8)) {
            leitor.readLine(); // Cabecalho
            String linha;
            int numeroLinha = 1;
            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) continue;
                String[] campos = linha.split(";", -1);
                if (campos.length != 9) {
                    System.err.println("[AVISO CSV] Pedido ignorado na linha " + numeroLinha + ": numero de colunas incorreto.");
                    continue;
                }
                try {
                    Cliente cliente = clienteRepository.buscarPorId(campos[1]);
                    if (cliente == null) {
                        throw new IllegalArgumentException("Cliente inexistente: " + campos[1]);
                    }
                    Pedido pedido = new Pedido(campos[0], cliente,
                            itensPorPedido.getOrDefault(campos[0], List.of()),
                            Double.parseDouble(campos[2]), Double.parseDouble(campos[3]),
                            Double.parseDouble(campos[4]), pagamentoPorNome(campos[5]),
                            Double.parseDouble(campos[8]), StatusPedido.valueOf(campos[6]),
                            LocalDateTime.parse(campos[7]));
                    pedidos.put(pedido.getId(), pedido);
                } catch (IllegalArgumentException erro) {
                    System.err.printf("[AVISO CSV] Pedido ignorado na linha %d: %s%n", numeroLinha, erro.getMessage());
                }
            }
        } catch (IOException erro) {
            throw new UncheckedIOException("Falha ao carregar pedidos.", erro);
        }
    }

    private Map<String, List<ItemCarrinho>> carregarItens() {
        Map<String, List<ItemCarrinho>> itens = new LinkedHashMap<>();
        try (BufferedReader leitor = Files.newBufferedReader(arquivoItens, StandardCharsets.UTF_8)) {
            leitor.readLine();
            String linha;
            int numeroLinha = 1;
            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) continue;
                String[] campos = linha.split(";", -1);
                if (campos.length != 4) {
                    System.err.println("[AVISO CSV] Item ignorado na linha " + numeroLinha + ": colunas invalidas.");
                    continue;
                }
                try {
                    int quantidade = Integer.parseInt(campos[2]);
                    double precoVenda = Double.parseDouble(campos[3]);
                    Produto produto = produtoRepository.buscarPorId(campos[1]);
                    if (produto == null) {
                        throw new IllegalArgumentException("Produto inexistente: " + campos[1]);
                    }
                    if (quantidade <= 0 || !Double.isFinite(precoVenda) || precoVenda < 0) {
                        throw new IllegalArgumentException("Quantidade ou preco invalido.");
                    }
                    // Copia o preco de venda para o historico, sem alterar o catalogo atual.
                    Produto fotoProduto = new Produto(produto.getId(), produto.getNome(), produto.getDescricao(),
                            precoVenda, 0, produto.isPromocaoVip());
                    itens.computeIfAbsent(campos[0], chave -> new ArrayList<>())
                            .add(new ItemCarrinho(fotoProduto, quantidade));
                } catch (IllegalArgumentException erro) {
                    System.err.printf("[AVISO CSV] Item ignorado na linha %d: %s%n", numeroLinha, erro.getMessage());
                }
            }
        } catch (IOException erro) {
            throw new UncheckedIOException("Falha ao carregar itens dos pedidos.", erro);
        }
        return itens;
    }

    private static FormaPagamento pagamentoPorNome(String nome) {
        return switch (nome) {
            case "PIX" -> new PagamentoPix();
            case "CARTAO_CREDITO" -> new PagamentoCartao(1);
            case "BOLETO" -> new PagamentoBoleto();
            default -> throw new IllegalArgumentException("Forma de pagamento desconhecida: " + nome);
        };
    }

    @Override
    public void salvarNoArquivo() {
        List<String> linhasPedidos = new ArrayList<>();
        List<String> linhasItens = new ArrayList<>();
        linhasPedidos.add(CABECALHO_PEDIDOS);
        linhasItens.add(CABECALHO_ITENS);
        for (Pedido pedido : pedidos.values()) {
            linhasPedidos.add(String.format(Locale.US, "%s;%s;%.2f;%.2f;%.2f;%s;%s;%s;%.2f",
                    pedido.getId(), pedido.getCliente().getId(), pedido.getValorSubtotal(),
                    pedido.getDescontoTotal(), pedido.getValorFinal(), pedido.getFormaPagamento().getNome(),
                    pedido.getStatus(), pedido.getDataCriacao(), pedido.getCashbackGerado()));
            for (ItemCarrinho item : pedido.getItens()) {
                linhasItens.add(String.format(Locale.US, "%s;%s;%d;%.2f", pedido.getId(),
                        item.getProduto().getId(), item.getQuantidade(), item.getProduto().getPreco()));
            }
        }
        // Gravacao por arquivo temporario reduz o risco de truncar o CSV por falha de escrita.
        escreverArquivo(arquivoItens, linhasItens);
        escreverArquivo(arquivoPedidos, linhasPedidos);
    }

    private static void criarSeAusente(Path destino, String cabecalho) {
        if (Files.exists(destino)) return;
        escreverArquivo(destino, List.of(cabecalho));
    }

    private static void escreverArquivo(Path destino, List<String> linhas) {
        Path temporario = null;
        try {
            Path diretorio = destino.toAbsolutePath().getParent();
            Files.createDirectories(diretorio);
            temporario = Files.createTempFile(diretorio, "shoponline-", ".csv.tmp");
            try (BufferedWriter escritor = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                for (String linha : linhas) {
                    escritor.write(linha);
                    escritor.newLine();
                }
            }
            try {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException erro) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException erro) {
            throw new UncheckedIOException("Falha ao gravar CSV: " + destino, erro);
        } finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); } catch (IOException ignorado) { }
            }
        }
    }
}
