package br.com.locadora.repository;

import br.com.locadora.domain.CategoriaVeiculo;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.domain.Veiculo;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Filtros dinâmicos de veículo: todos opcionais e combináveis. */
public final class VeiculoSpecs {

    private VeiculoSpecs() {
    }

    public static Specification<Veiculo> filtro(String placa, String marca, String modelo,
                                                CategoriaVeiculo categoria, StatusVeiculo status,
                                                BigDecimal valorMin, BigDecimal valorMax) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (texto(placa)) {
                String normalizada = placa.replace("-", "").trim().toUpperCase();
                predicados.add(cb.like(root.get("placa"), "%" + normalizada + "%"));
            }
            if (texto(marca)) {
                predicados.add(cb.like(cb.lower(root.get("marca")), "%" + marca.trim().toLowerCase() + "%"));
            }
            if (texto(modelo)) {
                predicados.add(cb.like(cb.lower(root.get("modelo")), "%" + modelo.trim().toLowerCase() + "%"));
            }
            if (categoria != null) {
                predicados.add(cb.equal(root.get("categoria"), categoria));
            }
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (valorMin != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("valorDiaria"), valorMin));
            }
            if (valorMax != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("valorDiaria"), valorMax));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static boolean texto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
