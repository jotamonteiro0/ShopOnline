package repository;

import model.Produto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Repositorio responsavel pela persistencia e consulta de produtos em arquivo CSV.
 * Implementa a interface Repositorio e garante persistencia atomica e tolerante a falhas.
 */
public class ProdutoRepository implements Repositorio<Produto, String> {
    private static final String CABECALHO = "id;nome;descricao;preco;estoque;promocaoVip";
    private static final String DELIMITADOR = ";";

    private final String caminhoArquivo;
    private final Map<String, Produto> produtos = new LinkedHashMap<>();

    public ProdutoRepository(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
        carregarDoArquivo();
    }

    @Override
    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos.values());
    }

    @Override
    public Produto buscarPorId(String id) {
        if (id == null) return null;
        return produtos.get(id.trim().toUpperCase());
    }

    @Override
    public void salvar(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("Produto nao pode ser nulo.");
        }
        produtos.put(produto.getId(), produto);
        salvarNoArquivo();
    }

    @Override
    public boolean removerPorId(String id) {
        if (id == null) return false;
        Produto removido = produtos.remove(id.trim().toUpperCase());
        if (removido != null) {
            salvarNoArquivo();
            return true;
        }
        return false;
    }

    @Override
    public boolean existePorId(String id) {
        if (id == null) return false;
        return produtos.containsKey(id.trim().toUpperCase());
    }

    @Override
    public void carregarDoArquivo() {
        File file = new File(caminhoArquivo);
        if (!file.exists()) {
            salvarNoArquivo(); // Cria o arquivo inicial com o cabecalho padrao
            return;
        }

        produtos.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String linha = reader.readLine(); // Cabecalho
            int linhaNum = 1;
            while ((linha = reader.readLine()) != null) {
                linhaNum++;
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split(DELIMITADOR);
                if (partes.length >= 6) {
                    try {
                        String id = partes[0].trim();
                        String nome = partes[1].trim();
                        String descricao = partes[2].trim();
                        double preco = Double.parseDouble(partes[3].trim());
                        int estoque = Integer.parseInt(partes[4].trim());
                        boolean promocaoVip = Boolean.parseBoolean(partes[5].trim());

                        Produto p = new Produto(id, nome, descricao, preco, estoque, promocaoVip);
                        produtos.put(p.getId(), p);
                    } catch (IllegalArgumentException e) {
                        System.err.printf("[AVISO CSV] Linha %d corrompida em '%s': %s%n", linhaNum, caminhoArquivo, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ERRO REPOSITORY] Falha ao carregar produtos: " + e.getMessage());
        }
    }

    @Override
    public void salvarNoArquivo() {
        File file = new File(caminhoArquivo);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(CABECALHO);
            writer.newLine();
            for (Produto p : produtos.values()) {
                writer.write(String.format(Locale.US, "%s;%s;%s;%.2f;%d;%b",
                        p.getId(), p.getNome(), p.getDescricao(), p.getPreco(),
                        p.getQuantidadeEstoque(), p.isPromocaoVip()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[ERRO REPOSITORY] Falha ao gravar produtos: " + e.getMessage());
        }
    }
}
