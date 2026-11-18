package repository;

import model.Cliente;
import model.ClienteVip;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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

    public void cadastrar(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não pode ser nulo.");
        }
        if (clientes.containsKey(cliente.getId())) {
            throw new IllegalArgumentException("Já existe um cliente com esse ID.");
        }
        salvar(cliente);
    }

    public void salvar(Cliente cliente) {
        Cliente anterior = clientes.put(cliente.getId(), cliente);
        try {
            salvarNoArquivo();
        } catch (UncheckedIOException e) {
            if (anterior == null) {
                clientes.remove(cliente.getId());
            } else {
                clientes.put(cliente.getId(), anterior);
            }
            throw e;
        }
    }

    public void carregarDoArquivo() {
        File file = new File(caminhoArquivo);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
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
        Path destino = Path.of(caminhoArquivo).toAbsolutePath();
        Path temporario = null;
        try {
            Files.createDirectories(destino.getParent());
            temporario = Files.createTempFile(destino.getParent(), "clientes-", ".csv.tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                writer.write("id;nome;email;pontos;saldoCashback;vip");
                writer.newLine();
                for (Cliente c : clientes.values()) {
                    writer.write(String.format(Locale.US, "%s;%s;%s;%d;%.2f;%b",
                            c.getId(), c.getNome(), c.getEmail(),
                            c.getPontosFidelidade(), c.getSaldoCashback(), c.isVip()));
                    writer.newLine();
                }
            }
            try {
                Files.move(temporario, destino,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar os clientes.", e);
        } finally {
            if (temporario != null) {
                try {
                    Files.deleteIfExists(temporario);
                } catch (IOException e) {
                    System.err.println("Não foi possível remover o arquivo temporário: " + e.getMessage());
                }
            }
        }
    }
}
