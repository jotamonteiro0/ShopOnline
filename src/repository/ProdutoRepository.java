package repository;

import model.Produto;
import java.io.*;
import java.util.*;

public class ProdutoRepository {
    private final String caminhoArquivo;
    private final Map<String, Produto> produtos = new LinkedHashMap<>();

    public ProdutoRepository(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
        carregarDoArquivo();
    }

    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos.values());
    }

    public Produto buscarPorId(String id) {
        return produtos.get(id);
    }

    public void salvar(Produto produto) {
        produtos.put(produto.getId(), produto);
        salvarNoArquivo();
    }

    public void carregarDoArquivo() {
        File file = new File(caminhoArquivo);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linha = reader.readLine(); // Header
            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split(";");
                if (partes.length >= 6) {
                    String id = partes[0].trim();
                    String nome = partes[1].trim();
                    String descricao = partes[2].trim();
                    double preco = Double.parseDouble(partes[3].trim());
                    int estoque = Integer.parseInt(partes[4].trim());
                    boolean promocaoVip = Boolean.parseBoolean(partes[5].trim());

                    Produto p = new Produto(id, nome, descricao, preco, estoque, promocaoVip);
                    produtos.put(id, p);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Erro ao carregar produtos: " + e.getMessage());
        }
    }

    public void salvarNoArquivo() {
        File file = new File(caminhoArquivo);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("id;nome;descricao;preco;estoque;promocaoVip");
            writer.newLine();
            for (Produto p : produtos.values()) {
                writer.write(String.format(Locale.US, "%s;%s;%s;%.2f;%d;%b",
                        p.getId(), p.getNome(), p.getDescricao(), p.getPreco(),
                        p.getQuantidadeEstoque(), p.isPromocaoVip()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar produtos: " + e.getMessage());
        }
    }
}
