package br.com.locadora.dto;

import br.com.locadora.domain.CategoriaVeiculo;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.domain.Veiculo;

import java.math.BigDecimal;

public record VeiculoResponse(
        Long id,
        String placa,
        String marca,
        String modelo,
        Integer ano,
        CategoriaVeiculo categoria,
        BigDecimal valorDiaria,
        Integer quilometragem,
        StatusVeiculo status) {

    public static VeiculoResponse from(Veiculo v) {
        return new VeiculoResponse(
                v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAno(),
                v.getCategoria(), v.getValorDiaria(), v.getQuilometragem(), v.getStatus());
    }
}
