package br.com.locadora.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record DevolucaoRequest(

        @Schema(description = "Opcional: se omitida, usa a data de hoje", example = "2026-11-05")
        LocalDate dataDevolucao,

        @Schema(description = "Quilometragem do veículo na devolução", example = "15450")
        @NotNull(message = "Quilometragem final é obrigatória")
        @PositiveOrZero(message = "Quilometragem final não pode ser negativa")
        Integer quilometragemFinal) {
}
