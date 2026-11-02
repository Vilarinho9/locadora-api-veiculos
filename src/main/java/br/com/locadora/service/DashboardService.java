package br.com.locadora.service;

import br.com.locadora.domain.StatusLocacao;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.dto.DashboardResponse;
import br.com.locadora.repository.ClienteRepository;
import br.com.locadora.repository.LocacaoRepository;
import br.com.locadora.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Service
public class DashboardService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final LocacaoRepository locacaoRepository;
    private final Clock clock;

    public DashboardService(VeiculoRepository veiculoRepository, ClienteRepository clienteRepository,
                            LocacaoRepository locacaoRepository, Clock clock) {
        this.veiculoRepository = veiculoRepository;
        this.clienteRepository = clienteRepository;
        this.locacaoRepository = locacaoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo() {
        BigDecimal receita = locacaoRepository.somarValorFinal(StatusLocacao.FINALIZADA);
        return new DashboardResponse(
                veiculoRepository.count(),
                veiculoRepository.countByStatus(StatusVeiculo.DISPONIVEL),
                veiculoRepository.countByStatus(StatusVeiculo.ALUGADO),
                veiculoRepository.countByStatus(StatusVeiculo.MANUTENCAO),
                clienteRepository.count(),
                locacaoRepository.countByStatus(StatusLocacao.ATIVA),
                locacaoRepository.countByStatusAndDataPrevistaDevolucaoBefore(
                        StatusLocacao.ATIVA, LocalDate.now(clock)),
                receita == null ? BigDecimal.ZERO.setScale(2) : receita);
    }
}
