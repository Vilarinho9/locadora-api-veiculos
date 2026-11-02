package br.com.locadora.repository;

import br.com.locadora.domain.Locacao;
import br.com.locadora.domain.StatusLocacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LocacaoRepository extends JpaRepository<Locacao, Long>, JpaSpecificationExecutor<Locacao> {

    /** Carrega cliente e veículo junto com a listagem (evita N+1 consultas). */
    @Override
    @EntityGraph(attributePaths = {"cliente", "veiculo"})
    Page<Locacao> findAll(Specification<Locacao> spec, Pageable pageable);

    boolean existsByClienteId(Long clienteId);

    boolean existsByVeiculoId(Long veiculoId);

    long countByClienteIdAndStatus(Long clienteId, StatusLocacao status);

    long countByStatus(StatusLocacao status);

    long countByStatusAndDataPrevistaDevolucaoBefore(StatusLocacao status, LocalDate data);

    /** Soma do valor final das locações no status informado (null quando não há registros). */
    @Query("select sum(l.valorFinal) from Locacao l where l.status = :status")
    BigDecimal somarValorFinal(@Param("status") StatusLocacao status);
}
