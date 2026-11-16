package repository;

import model.Cliente;
import model.ClienteVip;
import java.io.*;
import java.util.*;

public class ClienteRepository {
    private final String caminhoArquivo;
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();

    public ClienteRepository(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
        carregarDoArquivo();
    }

    public List<Cliente> listarTodos() {
        return new ArrayList<>(clientes.values());
    }

    public Cliente buscarPorId(String id) {
        return clientes.get(id);
    }

    public void salvar(Cliente cliente) {
        clientes.put(cliente.getId(), cliente);
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

    public void salvarNoArquivo() {
        File file = new File(caminhoArquivo);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("id;nome;email;pontos;saldoCashback;vip");
            writer.newLine();
            for (Cliente c : clientes.values()) {
                writer.write(String.format(Locale.US, "%s;%s;%s;%d;%.2f;%b",
                        c.getId(), c.getNome(), c.getEmail(),
                        c.getPontosFidelidade(), c.getSaldoCashback(), c.isVip()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar clientes: " + e.getMessage());
        }
    }
}
