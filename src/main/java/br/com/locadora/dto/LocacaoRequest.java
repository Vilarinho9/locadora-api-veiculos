package br.com.locadora.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LocacaoRequest(

        @Schema(example = "1")
        @NotNull(message = "Cliente é obrigatório")
        Long clienteId,

        @Schema(example = "1")
        @NotNull(message = "Veículo é obrigatório")
        Long veiculoId,

        @Schema(example = "2026-11-01")
        @NotNull(message = "Data de retirada é obrigatória")
        LocalDate dataRetirada,

        @Schema(example = "2026-11-05")
        @NotNull(message = "Data prevista de devolução é obrigatória")
        LocalDate dataPrevistaDevolucao,

        @Schema(example = "Cliente solicitou cadeirinha infantil")
        @Size(max = 255, message = "Observações deve ter no máximo 255 caracteres")
        String observacoes) {
}
