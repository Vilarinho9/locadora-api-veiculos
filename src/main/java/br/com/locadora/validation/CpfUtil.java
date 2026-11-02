package br.com.locadora.validation;

/** Utilitário puro (sem dependências) para normalizar e validar CPF. */
public final class CpfUtil {

    private CpfUtil() {
    }

    /** Remove tudo que não for dígito. */
    public static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    /** Valida CPF pelos dígitos verificadores (aceita com ou sem máscara). */
    public static boolean valido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d == null || d.length() != 11) {
            return false;
        }
        // rejeita sequências como 111.111.111-11
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = digitoVerificador(d, 9);
        int dv2 = digitoVerificador(d, 10);
        return dv1 == (d.charAt(9) - '0') && dv2 == (d.charAt(10) - '0');
    }

    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        int peso = quantidade + 1;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (peso - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
