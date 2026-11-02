package br.com.locadora.dto;

import br.com.locadora.domain.CategoriaVeiculo;
import br.com.locadora.domain.StatusVeiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VeiculoRequest(

        @Schema(example = "ABC1D23", description = "Padrão antigo (ABC1234) ou Mercosul (ABC1D23)")
        @NotBlank(message = "Placa é obrigatória")
        @Pattern(regexp = "^[A-Za-z]{3}-?[0-9][A-Za-z0-9][0-9]{2}$", message = "Placa inválida (use ABC1234 ou ABC1D23)")
        String placa,

        @Schema(example = "Fiat")
        @NotBlank(message = "Marca é obrigatória")
        @Size(max = 60, message = "Marca deve ter no máximo 60 caracteres")
        String marca,

        @Schema(example = "Argo")
        @NotBlank(message = "Modelo é obrigatório")
        @Size(max = 60, message = "Modelo deve ter no máximo 60 caracteres")
        String modelo,

        @Schema(example = "2023")
        @NotNull(message = "Ano é obrigatório")
        @Min(value = 1990, message = "Ano deve ser maior ou igual a 1990")
        @Max(value = 2100, message = "Ano inválido")
        Integer ano,

        @NotNull(message = "Categoria é obrigatória")
        CategoriaVeiculo categoria,

        @Schema(example = "120.00")
        @NotNull(message = "Valor da diária é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor da diária deve ser maior que zero")
        BigDecimal valorDiaria,

        @Schema(example = "15000")
        @NotNull(message = "Quilometragem é obrigatória")
        @PositiveOrZero(message = "Quilometragem não pode ser negativa")
        Integer quilometragem,

        @Schema(description = "Opcional. DISPONIVEL (padrão) ou MANUTENCAO. ALUGADO é definido apenas pelas locações.")
        StatusVeiculo status) {
}
