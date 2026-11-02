package br.com.locadora.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Na atualização só é possível alterar a data prevista de devolução e as observações. */
public record LocacaoUpdateRequest(

        @Schema(example = "2026-11-08")
        @NotNull(message = "Data prevista de devolução é obrigatória")
        LocalDate dataPrevistaDevolucao,

        @Size(max = 255, message = "Observações deve ter no máximo 255 caracteres")
        String observacoes) {
}
