package view;

import java.util.Scanner;

public class Entrada {
    private static final Scanner scanner = new Scanner(System.in);

    private Entrada() {
    }

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

    public static String lerId(String rotulo) {
        while (true) {
            String id = lerTexto(rotulo, true);
            if (id.matches("[a-zA-Z0-9_-]+")) {
                return id.toUpperCase();
            }
            System.out.println("  [Aviso] O identificador deve conter apenas letras, numeros e hifens.");
        }
    }

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

    public static double lerDouble(String rotulo, double min) {
        while (true) {
            System.out.print(rotulo);
            String entrada = scanner.nextLine().trim().replace(',', '.');
            try {
                double valor = Double.parseDouble(entrada);
                if (Double.isFinite(valor) && valor >= min) {
                    return valor;
                }
                System.out.printf("  [Aviso] O valor deve ser maior ou igual a %.2f.%n", min);
            } catch (NumberFormatException e) {
                System.out.println("  [Aviso] Digite um numero valido.");
            }
        }
    }

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

    public static void aguardarEnter() {
        System.out.print("\nPressione ENTER para continuar...");
        scanner.nextLine();
    }
}
