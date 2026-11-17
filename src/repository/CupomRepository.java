package repository;

import model.Cupom;
import model.TipoCupom;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CupomRepository implements Repositorio<Cupom, String> {
    private static final String CABECALHO = "codigo;tipo;valor;dataValidade;clientesUtilizados";
    private static final String DELIMITADOR = ";";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

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
        cupons.put(cupom.getCodigo(), cupom);
        salvarNoArquivo();
    }

    @Override
    public boolean removerPorId(String codigo) {
        if (codigo == null) return false;
        Cupom removido = cupons.remove(codigo.trim().toUpperCase());
        if (removido != null) {
            salvarNoArquivo();
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String linha = reader.readLine(); // Cabeçalho
            int linhaNum = 1;
            while ((linha = reader.readLine()) != null) {
                linhaNum++;
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split(DELIMITADOR, -1);
                if (partes.length >= 4) {
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
                                if (!id.trim().isEmpty()) {
                                    clientesUtilizados.add(id.trim().toUpperCase());
                                }
                            }
                        }

                        Cupom cupom = new Cupom(codigo, tipo, valor, dataValidade, clientesUtilizados);
                        cupons.put(cupom.getCodigo(), cupom);
                    } catch (IllegalArgumentException | DateTimeParseException e) {
                        System.err.printf("[AVISO CSV] Linha %d corrompida em '%s': %s%n", linhaNum, caminhoArquivo, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ERRO REPOSITORY] Falha ao carregar cupons: " + e.getMessage());
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
            for (Cupom c : cupons.values()) {
                String dataStr = c.getDataValidade() != null ? c.getDataValidade().format(DATE_FORMATTER) : "";
                String clientesStr = String.join(",", c.getClientesUtilizados());
                writer.write(String.format(Locale.US, "%s;%s;%.2f;%s;%s",
                        c.getCodigo(), c.getTipo(), c.getValor(), dataStr, clientesStr));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[ERRO REPOSITORY] Falha ao gravar cupons: " + e.getMessage());
        }
    }
}
