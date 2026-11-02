package br.com.locadora.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalVeiculos,
        long veiculosDisponiveis,
        long veiculosAlugados,
        long veiculosManutencao,
        long totalClientes,
        long locacoesAtivas,
        long locacoesAtrasadas,
        BigDecimal receitaTotal) {
}
