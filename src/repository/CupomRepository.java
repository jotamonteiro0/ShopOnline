package repository;

import model.Cupom;
import model.TipoCupom;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CupomRepository implements Repositorio<Cupom, String> {
    private static final String CABECALHO = "codigo;tipo;valor;dataValidade;clientesUtilizados";
    private static final String DELIMITADOR = ";";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private boolean arquivoComErros;
    private final String caminhoArquivo;
    private final Map<String, Cupom> cupons = new LinkedHashMap<>();

    public CupomRepository(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
        carregarDoArquivo();
    }

    @Override
    public List<Cupom> listarTodos() {
        return new ArrayList<>(cupons.values());
    }

    @Override
    public Cupom buscarPorId(String codigo) {
        if (codigo == null) return null;
        return cupons.get(codigo.trim().toUpperCase());
    }

    public Cupom buscarPorCodigo(String codigo) {
        return buscarPorId(codigo);
    }

    @Override
    public void salvar(Cupom cupom) {
        if (cupom == null) {
            throw new IllegalArgumentException("Cupom não pode ser nulo.");
        }
        Cupom anterior = cupons.put(cupom.getCodigo(), cupom);
        try {
            salvarNoArquivo();
        } catch (RuntimeException erro) {
            if (anterior == null) cupons.remove(cupom.getCodigo());
            else cupons.put(cupom.getCodigo(), anterior);
            throw erro;
        }
    }

    @Override
    public boolean removerPorId(String codigo) {
        if (codigo == null) return false;
        Cupom removido = cupons.remove(codigo.trim().toUpperCase());
        if (removido != null) {
            try {
                salvarNoArquivo();
            } catch (RuntimeException erro) {
                cupons.put(removido.getCodigo(), removido);
                throw erro;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean existePorId(String codigo) {
        if (codigo == null) return false;
        return cupons.containsKey(codigo.trim().toUpperCase());
    }

    @Override
    public void carregarDoArquivo() {
        File file = new File(caminhoArquivo);
        if (!file.exists()) {
            salvarNoArquivo(); // Cria o arquivo inicial com cabeçalho
            return;
        }

        cupons.clear();
        arquivoComErros = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String linha = reader.readLine();
            boolean legado = "codigo;tipo;valor;dataValidade;utilizado".equals(linha);
            if (!legado && !CABECALHO.equals(linha)) throw new IOException("Cabecalho de cupons invalido.");
            int linhaNum = 1;
            while ((linha = reader.readLine()) != null) {
                linhaNum++;
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split(DELIMITADOR, -1);
                if (partes.length == 5) {
                    try {
                        String codigo = partes[0].trim();
                        TipoCupom tipo = TipoCupom.valueOf(partes[1].trim().toUpperCase());
                        double valor = Double.parseDouble(partes[2].trim());

                        LocalDate dataValidade = null;
                        String dataStr = partes[3].trim();
                        if (!dataStr.isEmpty() && !dataStr.equalsIgnoreCase("null")) {
                            dataValidade = LocalDate.parse(dataStr, DATE_FORMATTER);
                        }

                        Set<String> clientesUtilizados = new HashSet<>();
                        if (partes.length >= 5 && !partes[4].trim().isEmpty()) {
                            String[] ids = partes[4].trim().split(",");
                            for (String id : ids) {
                                String trimmedId = id.trim();
                                if (!legado && !trimmedId.isEmpty()) {
                                    clientesUtilizados.add(trimmedId);
                                }
                            }
                        }

                        if (legado) {
                            if (partes[4].equalsIgnoreCase("true")) clientesUtilizados.add("*");
                            else if (!partes[4].equalsIgnoreCase("false")) {
                                throw new IllegalArgumentException("Indicador de utilizacao invalido.");
                            }
                        }
                        Cupom cupom = new Cupom(codigo, tipo, valor, dataValidade, clientesUtilizados);
                        cupons.put(cupom.getCodigo(), cupom);
                    } catch (IllegalArgumentException | DateTimeParseException e) {
                        arquivoComErros = true;
                        System.err.printf("[AVISO CSV] Linha %d corrompida em '%s': %s%n", linhaNum, caminhoArquivo, e.getMessage());
                    }
                } else {
                    arquivoComErros = true;
                    System.err.println("[AVISO CSV] Cupom com numero de colunas invalido: " + linhaNum);
                }
            }
        } catch (IOException e) {
            arquivoComErros = true;
            throw new UncheckedIOException("Falha ao carregar cupons.", e);
        }
    }

    @Override
    public void salvarNoArquivo() {
        if (arquivoComErros) {
            throw new IllegalStateException("Corrija e recarregue o CSV de cupons antes de salvar.");
        }
        Path destino = Path.of(caminhoArquivo).toAbsolutePath();
        Path temporario = null;
        try {
            Files.createDirectories(destino.getParent());
            temporario = Files.createTempFile(destino.getParent(), "cupons-", ".csv.tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                writer.write(CABECALHO);
                writer.newLine();
                for (Cupom c : cupons.values()) {
                    String data = c.getDataValidade() == null ? "" : c.getDataValidade().format(DATE_FORMATTER);
                    writer.write(String.format(Locale.US, "%s;%s;%.2f;%s;%s",
                            c.getCodigo(), c.getTipo(), c.getValor(), data, String.join(",", c.getClientesUtilizados())));
                    writer.newLine();
                }
            }
            try {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException erro) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException erro) {
            throw new UncheckedIOException("Falha ao gravar cupons.", erro);
        } finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); } catch (IOException ignorado) { }
            }
        }
    }
}
