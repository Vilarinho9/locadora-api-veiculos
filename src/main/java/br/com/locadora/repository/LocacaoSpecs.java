package br.com.locadora.repository;

import br.com.locadora.domain.Locacao;
import br.com.locadora.domain.StatusLocacao;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Filtros dinâmicos de locação: todos opcionais e combináveis. */
public final class LocacaoSpecs {

    private LocacaoSpecs() {
    }

    public static Specification<Locacao> filtro(StatusLocacao status, Long clienteId, Long veiculoId,
                                                LocalDate retiradaDe, LocalDate retiradaAte,
                                                Boolean atrasadas, LocalDate hoje) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (veiculoId != null) {
                predicados.add(cb.equal(root.get("veiculo").get("id"), veiculoId));
            }
            if (retiradaDe != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("dataRetirada"), retiradaDe));
            }
            if (retiradaAte != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<LocalDate>get("dataRetirada"), retiradaAte));
            }
            if (Boolean.TRUE.equals(atrasadas)) {
                predicados.add(cb.equal(root.get("status"), StatusLocacao.ATIVA));
                predicados.add(cb.lessThan(root.<LocalDate>get("dataPrevistaDevolucao"), hoje));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
