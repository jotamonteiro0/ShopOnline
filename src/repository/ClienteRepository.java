package repository;

import model.Cliente;
import model.ClienteVip;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClienteRepository {
    private final String caminhoArquivo;
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();

    public ClienteRepository(String caminhoArquivo) {
        if (caminhoArquivo == null || caminhoArquivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho do arquivo de clientes nao pode ser vazio.");
        }
        this.caminhoArquivo = caminhoArquivo;
        carregarDoArquivo();
    }

    public List<Cliente> listarTodos() {
        return new ArrayList<>(clientes.values());
    }

    public Cliente buscarPorId(String id) {
        if (id == null) return null;
        String chave = id.trim();
        Cliente direto = clientes.get(chave);
        return direto != null ? direto : clientes.get(chave.toUpperCase());
    }

    public void salvar(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente nao pode ser nulo.");
        }
        clientes.put(cliente.getId().trim().toUpperCase(), cliente);
        salvarNoArquivo();
    }

    public final void carregarDoArquivo() {
        File file = new File(caminhoArquivo);
        if (!file.exists()) {
            return;
        }

        clientes.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String linha = reader.readLine(); // Header
            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split(";", -1);
                if (partes.length >= 6) {
                    String id = partes[0].trim().toUpperCase();
                    String nome = partes[1].trim();
                    String email = partes[2].trim();
                    int pontos = Integer.parseInt(partes[3].trim());
                    double saldo = Double.parseDouble(partes[4].trim());
                    boolean vip = Boolean.parseBoolean(partes[5].trim());

                    Cliente c;
                    if (vip) {
                        c = new ClienteVip(id, nome, email, pontos, saldo);
                    } else {
                        c = new Cliente(id, nome, email, pontos, saldo);
                    }
                    clientes.put(id, c);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Erro ao carregar clientes: " + e.getMessage());
        }
    }

    public final void salvarNoArquivo() {
        File file = new File(caminhoArquivo);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write("id;nome;email;pontos;saldoCashback;vip");
            writer.newLine();
            for (Cliente c : clientes.values()) {
                String nomeSeguro = c.getNome() != null ? c.getNome().replace(';', ',') : "";
                String emailSeguro = c.getEmail() != null ? c.getEmail().replace(';', ',') : "";
                writer.write(String.format(Locale.US, "%s;%s;%s;%d;%.2f;%b",
                        c.getId(), nomeSeguro, emailSeguro,
                        c.getPontosFidelidade(), c.getSaldoCashback(), c.isVip()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar clientes: " + e.getMessage());
        }
    }
}
