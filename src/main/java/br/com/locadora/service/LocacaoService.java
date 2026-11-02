package br.com.locadora.service;

import br.com.locadora.domain.Cliente;
import br.com.locadora.domain.Locacao;
import br.com.locadora.domain.StatusLocacao;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.domain.Veiculo;
import br.com.locadora.dto.DevolucaoRequest;
import br.com.locadora.dto.LocacaoRequest;
import br.com.locadora.dto.LocacaoResponse;
import br.com.locadora.dto.LocacaoUpdateRequest;
import br.com.locadora.dto.PageResponse;
import br.com.locadora.exception.ConflitoException;
import br.com.locadora.exception.RecursoNaoEncontradoException;
import br.com.locadora.exception.RegraNegocioException;
import br.com.locadora.repository.ClienteRepository;
import br.com.locadora.repository.LocacaoRepository;
import br.com.locadora.repository.LocacaoSpecs;
import br.com.locadora.repository.VeiculoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/**
 * Núcleo das regras de negócio da locadora.
 *
 * <p>Regras ao criar uma locação (violação → HTTP 422):
 * <ol>
 *   <li>Retirada não pode ser no passado; devolução prevista deve ser posterior à retirada;
 *       período máximo de {@value #MAX_DIAS_LOCACAO} dias.</li>
 *   <li>Cliente com no mínimo {@value #IDADE_MINIMA} anos ({@value #IDADE_MINIMA_PREMIUM} para
 *       categorias LUXO e ESPORTIVO).</li>
 *   <li>CNH precisa estar válida até a data prevista de devolução.</li>
 *   <li>Veículo precisa estar DISPONIVEL.</li>
 *   <li>Cliente pode ter no máximo {@value #MAX_LOCACOES_ATIVAS} locações ativas.</li>
 * </ol>
 */
@Service
public class LocacaoService {

    public static final int MAX_DIAS_LOCACAO = 30;
    public static final int MAX_LOCACOES_ATIVAS = 2;
    public static final int IDADE_MINIMA = 21;
    public static final int IDADE_MINIMA_PREMIUM = 25;

    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final Clock clock;

    public LocacaoService(LocacaoRepository locacaoRepository, ClienteRepository clienteRepository,
                          VeiculoRepository veiculoRepository, Clock clock) {
        this.locacaoRepository = locacaoRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public PageResponse<LocacaoResponse> listar(StatusLocacao status, Long clienteId, Long veiculoId,
                                                LocalDate retiradaDe, LocalDate retiradaAte,
                                                Boolean atrasadas, Pageable pageable) {
        LocalDate hoje = hoje();
        return PageResponse.from(
                locacaoRepository.findAll(
                                LocacaoSpecs.filtro(status, clienteId, veiculoId, retiradaDe, retiradaAte, atrasadas, hoje),
                                pageable)
                        .map(l -> LocacaoResponse.from(l, hoje)));
    }

    @Transactional(readOnly = true)
    public LocacaoResponse buscar(Long id) {
        return LocacaoResponse.from(buscarEntidade(id), hoje());
    }

    // ------------------------------------------------------------------ criação

    @Transactional
    public LocacaoResponse criar(LocacaoRequest req) {
        Cliente cliente = clienteRepository.findById(req.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", req.clienteId()));
        Veiculo veiculo = veiculoRepository.findById(req.veiculoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", req.veiculoId()));

        validarPeriodo(req.dataRetirada(), req.dataPrevistaDevolucao());
        validarCliente(cliente, veiculo, req.dataPrevistaDevolucao());

        if (veiculo.getStatus() != StatusVeiculo.DISPONIVEL) {
            throw new RegraNegocioException(
                    "Veículo indisponível para locação (status atual: " + veiculo.getStatus() + ")");
        }
        if (locacaoRepository.countByClienteIdAndStatus(cliente.getId(), StatusLocacao.ATIVA) >= MAX_LOCACOES_ATIVAS) {
            throw new RegraNegocioException(
                    "Cliente já possui " + MAX_LOCACOES_ATIVAS + " locações ativas (limite máximo)");
        }

        long dias = CalculadoraLocacao.dias(req.dataRetirada(), req.dataPrevistaDevolucao());
        CalculadoraLocacao.Valores valores = CalculadoraLocacao.calcular(veiculo.getValorDiaria(), dias);

        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setVeiculo(veiculo);
        locacao.setDataRetirada(req.dataRetirada());
        locacao.setDataPrevistaDevolucao(req.dataPrevistaDevolucao());
        locacao.setValorDiaria(veiculo.getValorDiaria());
        locacao.setDesconto(valores.desconto());
        locacao.setValorPrevisto(valores.total());
        locacao.setObservacoes(normalizar(req.observacoes()));
        locacao.setStatus(StatusLocacao.ATIVA);

        veiculo.setStatus(StatusVeiculo.ALUGADO);
        veiculoRepository.save(veiculo);

        return LocacaoResponse.from(locacaoRepository.save(locacao), hoje());
    }

    // ------------------------------------------------------------------ atualização

    @Transactional
    public LocacaoResponse atualizar(Long id, LocacaoUpdateRequest req) {
        Locacao locacao = buscarEntidade(id);
        exigirAtiva(locacao, "alterada");
        if (locacao.getDataRetirada().isBefore(hoje())) {
            throw new RegraNegocioException(
                    "Locação já iniciada não pode ser alterada; registre a devolução");
        }

        validarPeriodo(locacao.getDataRetirada(), req.dataPrevistaDevolucao());
        validarCnh(locacao.getCliente(), req.dataPrevistaDevolucao());

        long dias = CalculadoraLocacao.dias(locacao.getDataRetirada(), req.dataPrevistaDevolucao());
        CalculadoraLocacao.Valores valores = CalculadoraLocacao.calcular(locacao.getValorDiaria(), dias);

        locacao.setDataPrevistaDevolucao(req.dataPrevistaDevolucao());
        locacao.setDesconto(valores.desconto());
        locacao.setValorPrevisto(valores.total());
        locacao.setObservacoes(normalizar(req.observacoes()));

        return LocacaoResponse.from(locacaoRepository.save(locacao), hoje());
    }

    // ------------------------------------------------------------------ devolução

    /**
     * Finaliza a locação calculando o valor final:
     * no prazo → valor previsto; atraso → dias extras + multa de 20% da diária por dia;
     * antecipada → dias usados com desconto recalculado + taxa de 10% da diária por dia não usado.
     */
    @Transactional
    public LocacaoResponse devolver(Long id, DevolucaoRequest req) {
        Locacao locacao = buscarEntidade(id);
        exigirAtiva(locacao, "finalizada");

        LocalDate hoje = hoje();
        LocalDate devolucao = req.dataDevolucao() != null ? req.dataDevolucao() : hoje;
        if (devolucao.isAfter(hoje)) {
            throw new RegraNegocioException("A data de devolução não pode ser futura");
        }
        if (devolucao.isBefore(locacao.getDataRetirada())) {
            throw new RegraNegocioException("A data de devolução não pode ser anterior à data de retirada");
        }

        Veiculo veiculo = locacao.getVeiculo();
        if (req.quilometragemFinal() < veiculo.getQuilometragem()) {
            throw new RegraNegocioException(
                    "Quilometragem final não pode ser menor que a atual do veículo (" + veiculo.getQuilometragem() + " km)");
        }

        LocalDate prevista = locacao.getDataPrevistaDevolucao();
        BigDecimal diaria = locacao.getValorDiaria();
        BigDecimal multa = BigDecimal.ZERO;
        BigDecimal desconto = locacao.getDesconto();
        BigDecimal valorFinal;

        if (devolucao.isAfter(prevista)) {
            long diasAtraso = CalculadoraLocacao.dias(prevista, devolucao);
            BigDecimal diasExtras = CalculadoraLocacao.money(diaria.multiply(BigDecimal.valueOf(diasAtraso)));
            multa = CalculadoraLocacao.multaAtraso(diaria, diasAtraso);
            valorFinal = locacao.getValorPrevisto().add(diasExtras).add(multa);
        } else if (devolucao.isBefore(prevista)) {
            long diasPrevistos = CalculadoraLocacao.dias(locacao.getDataRetirada(), prevista);
            long diasUsados = Math.max(1, CalculadoraLocacao.dias(locacao.getDataRetirada(), devolucao));
            long diasNaoUsados = Math.max(0, diasPrevistos - diasUsados);
            CalculadoraLocacao.Valores usados = CalculadoraLocacao.calcular(diaria, diasUsados);
            desconto = usados.desconto();
            multa = CalculadoraLocacao.taxaAntecipacao(diaria, diasNaoUsados);
            valorFinal = usados.total().add(multa);
        } else {
            valorFinal = locacao.getValorPrevisto();
        }

        locacao.setDataDevolucao(devolucao);
        locacao.setDesconto(desconto);
        locacao.setMulta(CalculadoraLocacao.money(multa));
        locacao.setValorFinal(CalculadoraLocacao.money(valorFinal));
        locacao.setStatus(StatusLocacao.FINALIZADA);

        veiculo.setQuilometragem(req.quilometragemFinal());
        veiculo.setStatus(StatusVeiculo.DISPONIVEL);
        veiculoRepository.save(veiculo);

        return LocacaoResponse.from(locacaoRepository.save(locacao), hoje);
    }

    // ------------------------------------------------------------------ cancelamento

    @Transactional
    public LocacaoResponse cancelar(Long id) {
        Locacao locacao = buscarEntidade(id);
        exigirAtiva(locacao, "cancelada");
        if (locacao.getDataRetirada().isBefore(hoje())) {
            throw new RegraNegocioException(
                    "Locação já iniciada não pode ser cancelada; registre a devolução");
        }

        locacao.setStatus(StatusLocacao.CANCELADA);
        Veiculo veiculo = locacao.getVeiculo();
        veiculo.setStatus(StatusVeiculo.DISPONIVEL);
        veiculoRepository.save(veiculo);

        return LocacaoResponse.from(locacaoRepository.save(locacao), hoje());
    }

    // ------------------------------------------------------------------ exclusão

    @Transactional
    public void excluir(Long id) {
        Locacao locacao = buscarEntidade(id);
        if (locacao.getStatus() == StatusLocacao.ATIVA) {
            throw new ConflitoException("Locação ativa não pode ser excluída; cancele ou finalize antes");
        }
        locacaoRepository.delete(locacao);
    }

    // ------------------------------------------------------------------ validações

    private void validarPeriodo(LocalDate retirada, LocalDate prevista) {
        if (retirada.isBefore(hoje())) {
            throw new RegraNegocioException("A data de retirada não pode estar no passado");
        }
        if (!prevista.isAfter(retirada)) {
            throw new RegraNegocioException("A data prevista de devolução deve ser posterior à data de retirada");
        }
        if (CalculadoraLocacao.dias(retirada, prevista) > MAX_DIAS_LOCACAO) {
            throw new RegraNegocioException("O período máximo de locação é de " + MAX_DIAS_LOCACAO + " dias");
        }
    }

    private void validarCliente(Cliente cliente, Veiculo veiculo, LocalDate prevista) {
        int idade = Period.between(cliente.getDataNascimento(), hoje()).getYears();
        if (idade < IDADE_MINIMA) {
            throw new RegraNegocioException("Cliente deve ter no mínimo " + IDADE_MINIMA + " anos para alugar");
        }
        if (veiculo.getCategoria().isPremium() && idade < IDADE_MINIMA_PREMIUM) {
            throw new RegraNegocioException("Veículos da categoria " + veiculo.getCategoria()
                    + " exigem cliente com no mínimo " + IDADE_MINIMA_PREMIUM + " anos");
        }
        validarCnh(cliente, prevista);
    }

    private void validarCnh(Cliente cliente, LocalDate prevista) {
        if (cliente.getCnhValidade().isBefore(prevista)) {
            throw new RegraNegocioException(
                    "CNH do cliente vence em " + cliente.getCnhValidade() + ", antes da devolução prevista");
        }
    }

    private void exigirAtiva(Locacao locacao, String acao) {
        if (locacao.getStatus() != StatusLocacao.ATIVA) {
            throw new RegraNegocioException(
                    "Apenas locações ativas podem ser " + acao + " (status atual: " + locacao.getStatus() + ")");
        }
    }

    private Locacao buscarEntidade(Long id) {
        return locacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Locação", id));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
