package repository;

import model.Cliente;
import model.ClienteVip;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ClienteRepository {
    private static final String CABECALHO = "id;nome;email;pontos;saldoCashback;vip";
    private final String caminhoArquivo;
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private boolean arquivoComErros;

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

        Map<String, Cliente> carregados = new LinkedHashMap<>();
        boolean encontrouErros = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String linha = reader.readLine();
            if (!CABECALHO.equals(linha)) {
                throw new IOException("Cabeçalho de clientes inválido.");
            }
            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) continue;
                try {
                    String[] partes = linha.split(";", -1);
                    if (partes.length != 6) {
                        throw new IllegalArgumentException("Esperadas exatamente seis colunas.");
                    }
                    String id = partes[0].trim();
                    String nome = partes[1].trim();
                    String email = partes[2].trim();
                    int pontos = Integer.parseInt(partes[3].trim());
                    double saldo = Double.parseDouble(partes[4].trim());
                    if (pontos < 0 || !Double.isFinite(saldo) || saldo < 0) {
                        throw new IllegalArgumentException("Pontos e cashback devem ser não negativos e finitos.");
                    }
                    if (!partes[5].trim().equalsIgnoreCase("true")
                            && !partes[5].trim().equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("VIP deve ser true ou false.");
                    }
                    boolean vip = Boolean.parseBoolean(partes[5].trim());
                    Cliente c = vip ? new ClienteVip(id, nome, email, pontos, saldo)
                            : new Cliente(id, nome, email, pontos, saldo);
                    if (carregados.containsKey(c.getId())) {
                        throw new IllegalArgumentException("ID de cliente repetido.");
                    }
                    carregados.put(c.getId(), c);
                } catch (IllegalArgumentException e) {
                    encontrouErros = true;
                    System.err.printf("[AVISO CSV] Linha %d inválida em '%s': %s%n",
                            numeroLinha, caminhoArquivo, e.getMessage());
                }
            }
            clientes.clear();
            clientes.putAll(carregados);
            arquivoComErros = encontrouErros;
        } catch (IOException e) {
            arquivoComErros = true;
            System.err.println("Erro ao carregar clientes: " + e.getMessage());
        }
    }

    public void salvarNoArquivo() {
        if (arquivoComErros) {
            throw new UncheckedIOException("Corrija e recarregue o CSV de clientes antes de salvar.",
                    new IOException("O arquivo de clientes contém erros de leitura."));
        }
        Path destino = Path.of(caminhoArquivo).toAbsolutePath();
        Path temporario = null;
        try {
            Files.createDirectories(destino.getParent());
            temporario = Files.createTempFile(destino.getParent(), "clientes-", ".csv.tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                writer.write(CABECALHO);
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
