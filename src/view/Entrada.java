package view;

import java.util.Scanner;

/**
 * Utilitario de leitura segura do teclado via console.
 * Implementa tratamento defensivo de excecoes (try/catch) para que o sistema
 * jamais trave ou aborte por entradas invalidas, letras em campos numericos
 * ou valores fora de faixa.
 */
public class Entrada {
    private static final Scanner scanner = new Scanner(System.in);

    private Entrada() {
        // Construtor privado para classe estatica utilitaria
    }

    /**
     * Le uma linha de texto do teclado.
     *
     * @param rotulo Mensagem exibida para o usuario
     * @param obrigatorio Se true, repete a solicitacao ate o usuario digitar algo valido
     * @return O texto digitado
     */
    public static String lerTexto(String rotulo, boolean obrigatorio) {
        while (true) {
            System.out.print(rotulo);
            String entrada = scanner.nextLine().trim();
            if (!obrigatorio || !entrada.isEmpty()) {
                return entrada;
            }
            System.out.println("  [Aviso] Este campo e obrigatorio. Por favor, digite um valor.");
        }
    }

    /**
     * Le um identificador (codigo/ID) e converte automaticamente para letras maiusculas.
     */
    public static String lerId(String rotulo) {
        while (true) {
            String id = lerTexto(rotulo, true);
            if (id.matches("[a-zA-Z0-9_-]+")) {
                return id.toUpperCase();
            }
            System.out.println("  [Aviso] O identificador deve conter apenas letras, numeros e hifens.");
        }
    }

    /**
     * Le um numero inteiro dentro de uma faixa especificada [min, max].
     */
    public static int lerInteiro(String rotulo, int min, int max) {
        while (true) {
            System.out.print(rotulo);
            String entrada = scanner.nextLine().trim();
            try {
                int valor = Integer.parseInt(entrada);
                if (valor >= min && valor <= max) {
                    return valor;
                }
                System.out.printf("  [Aviso] Digite um numero inteiro entre %d e %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  [Aviso] Entrada invalida. Digite apenas digitos numericos.");
            }
        }
    }

    /**
     * Le um numero decimal (double) com valor minimo permitido.
     * Aceita tanto ponto (.) quanto virgula (,) como separador decimal.
     */
    public static double lerDouble(String rotulo, double min) {
        while (true) {
            System.out.print(rotulo);
            String entrada = scanner.nextLine().trim().replace(',', '.');
            try {
                double valor = Double.parseDouble(entrada);
                if (valor >= min) {
                    return valor;
                }
                System.out.printf("  [Aviso] O valor deve ser maior ou igual a %.2f.%n", min);
            } catch (NumberFormatException e) {
                System.out.println("  [Aviso] Valor monetario ou numerico invalido. Digite um numero valido.");
            }
        }
    }

    /**
     * Le uma confirmacao booleana (S/N ou Sim/Nao).
     */
    public static boolean lerConfirmacao(String rotulo) {
        while (true) {
            System.out.print(rotulo + " (S/N): ");
            String entrada = scanner.nextLine().trim().toUpperCase();
            if (entrada.equals("S") || entrada.equals("SIM")) {
                return true;
            } else if (entrada.equals("N") || entrada.equals("NAO") || entrada.equals("NÃO")) {
                return false;
            }
            System.out.println("  [Aviso] Responda apenas com 'S' para sim ou 'N' para nao.");
        }
    }

    /**
     * Pausa a execucao e aguarda o usuario pressionar ENTER para prosseguir.
     */
    public static void aguardarEnter() {
        System.out.print("\nPressione ENTER para continuar...");
        scanner.nextLine();
    }
}
