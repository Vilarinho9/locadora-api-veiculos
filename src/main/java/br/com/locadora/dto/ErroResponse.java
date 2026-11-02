package br.com.locadora.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** Formato único de erro devolvido por toda a API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta padrão de erro")
public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> campos) {

    @Schema(description = "Erro de validação em um campo específico")
    public record CampoErro(String campo, String mensagem) {
    }
}
