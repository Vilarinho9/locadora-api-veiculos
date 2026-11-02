package br.com.locadora.dto;

import br.com.locadora.validation.Cpf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClienteRequest(

        @Schema(example = "Maria da Silva")
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        @Schema(example = "529.982.247-25", description = "Aceita com ou sem máscara")
        @NotBlank(message = "CPF é obrigatório")
        @Cpf
        String cpf,

        @Schema(example = "maria@email.com")
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 120, message = "E-mail deve ter no máximo 120 caracteres")
        String email,

        @Schema(example = "(86) 99999-0000")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone,

        @Schema(example = "1990-03-15")
        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve estar no passado")
        LocalDate dataNascimento,

        @Schema(example = "12345678901", description = "11 dígitos")
        @NotBlank(message = "Número da CNH é obrigatório")
        @Pattern(regexp = "^\\d{11}$", message = "CNH deve ter exatamente 11 dígitos")
        String cnhNumero,

        @Schema(example = "2030-12-31")
        @NotNull(message = "Validade da CNH é obrigatória")
        LocalDate cnhValidade) {
}
