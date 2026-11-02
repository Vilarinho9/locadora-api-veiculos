package br.com.locadora.repository;

import br.com.locadora.domain.Cliente;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Filtros dinâmicos de cliente: todos opcionais e combináveis. */
public final class ClienteSpecs {

    private ClienteSpecs() {
    }

    public static Specification<Cliente> filtro(String nome, String cpf, String email) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (texto(nome)) {
                predicados.add(cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%"));
            }
            if (texto(cpf)) {
                String digitos = cpf.replaceAll("\\D", "");
                predicados.add(cb.like(root.get("cpf"), "%" + digitos + "%"));
            }
            if (texto(email)) {
                predicados.add(cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%"));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static boolean texto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
