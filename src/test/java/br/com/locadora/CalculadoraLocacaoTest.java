package br.com.locadora;

import br.com.locadora.service.CalculadoraLocacao;
import br.com.locadora.service.CalculadoraLocacao.Valores;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraLocacaoTest {

    private static final BigDecimal DIARIA = new BigDecimal("100.00");

    @Test
    void semDescontoAteSeisDias() {
        Valores v = CalculadoraLocacao.calcular(DIARIA, 6);
        assertEquals(new BigDecimal("600.00"), v.subtotal());
        assertEquals(new BigDecimal("0.00"), v.desconto());
        assertEquals(new BigDecimal("600.00"), v.total());
    }

    @Test
    void descontoDeDezPorCentoApartirDeSeteDias() {
        Valores v = CalculadoraLocacao.calcular(DIARIA, 10);
        assertEquals(new BigDecimal("1000.00"), v.subtotal());
        assertEquals(new BigDecimal("100.00"), v.desconto());
        assertEquals(new BigDecimal("900.00"), v.total());
    }

    @Test
    void descontoDeQuinzePorCentoApartirDeQuinzeDias() {
        Valores v = CalculadoraLocacao.calcular(DIARIA, 20);
        assertEquals(new BigDecimal("2000.00"), v.subtotal());
        assertEquals(new BigDecimal("300.00"), v.desconto());
        assertEquals(new BigDecimal("1700.00"), v.total());
    }

    @Test
    void multaDeAtrasoEVinteECentoDaDiariaPorDia() {
        assertEquals(new BigDecimal("100.00"), CalculadoraLocacao.multaAtraso(DIARIA, 5));
    }

    @Test
    void taxaDeAntecipacaoEDezPorCentoDaDiariaPorDiaNaoUsado() {
        assertEquals(new BigDecimal("20.00"), CalculadoraLocacao.taxaAntecipacao(DIARIA, 2));
    }

    @Test
    void contaDiasEntreDatas() {
        assertEquals(4, CalculadoraLocacao.dias(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5)));
    }
}
