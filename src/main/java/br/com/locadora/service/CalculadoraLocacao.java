package br.com.locadora.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regras de precificação da locação (sem dependência de Spring, fácil de testar).
 *
 * <ul>
 *   <li>Desconto progressivo: 7+ dias = 10%, 15+ dias = 15%.</li>
 *   <li>Atraso: cada dia extra cobra a diária cheia + multa de 20% da diária.</li>
 *   <li>Devolução antecipada: cobra os dias usados (mínimo 1) + taxa de 10% da diária
 *       sobre cada dia não utilizado.</li>
 * </ul>
 */
public final class CalculadoraLocacao {

    public static final BigDecimal PERCENTUAL_MULTA_ATRASO = new BigDecimal("0.20");
    public static final BigDecimal PERCENTUAL_TAXA_ANTECIPACAO = new BigDecimal("0.10");

    private CalculadoraLocacao() {
    }

    public record Valores(BigDecimal subtotal, BigDecimal desconto, BigDecimal total) {
    }

    /** Quantidade de diárias entre duas datas (retirada → devolução). */
    public static long dias(LocalDate inicio, LocalDate fim) {
        return ChronoUnit.DAYS.between(inicio, fim);
    }

    public static BigDecimal percentualDesconto(long dias) {
        if (dias >= 15) {
            return new BigDecimal("0.15");
        }
        if (dias >= 7) {
            return new BigDecimal("0.10");
        }
        return BigDecimal.ZERO;
    }

    public static Valores calcular(BigDecimal valorDiaria, long dias) {
        BigDecimal subtotal = money(valorDiaria.multiply(BigDecimal.valueOf(dias)));
        BigDecimal desconto = money(subtotal.multiply(percentualDesconto(dias)));
        return new Valores(subtotal, desconto, money(subtotal.subtract(desconto)));
    }

    public static BigDecimal multaAtraso(BigDecimal valorDiaria, long diasAtraso) {
        return money(valorDiaria.multiply(BigDecimal.valueOf(diasAtraso)).multiply(PERCENTUAL_MULTA_ATRASO));
    }

    public static BigDecimal taxaAntecipacao(BigDecimal valorDiaria, long diasNaoUsados) {
        return money(valorDiaria.multiply(BigDecimal.valueOf(diasNaoUsados)).multiply(PERCENTUAL_TAXA_ANTECIPACAO));
    }

    public static BigDecimal money(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
