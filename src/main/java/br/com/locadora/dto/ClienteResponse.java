package br.com.locadora.dto;

import br.com.locadora.domain.Cliente;

import java.time.LocalDate;

public record ClienteResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate dataNascimento,
        String cnhNumero,
        LocalDate cnhValidade) {

    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getNome(), c.getCpf(), c.getEmail(), c.getTelefone(),
                c.getDataNascimento(), c.getCnhNumero(), c.getCnhValidade());
    }
}
