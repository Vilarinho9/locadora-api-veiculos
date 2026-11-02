package br.com.locadora.service;

import br.com.locadora.domain.CategoriaVeiculo;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.domain.Veiculo;
import br.com.locadora.dto.PageResponse;
import br.com.locadora.dto.VeiculoRequest;
import br.com.locadora.dto.VeiculoResponse;
import br.com.locadora.exception.ConflitoException;
import br.com.locadora.exception.RecursoNaoEncontradoException;
import br.com.locadora.exception.RegraNegocioException;
import br.com.locadora.repository.LocacaoRepository;
import br.com.locadora.repository.VeiculoRepository;
import br.com.locadora.repository.VeiculoSpecs;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final LocacaoRepository locacaoRepository;

    public VeiculoService(VeiculoRepository veiculoRepository, LocacaoRepository locacaoRepository) {
        this.veiculoRepository = veiculoRepository;
        this.locacaoRepository = locacaoRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<VeiculoResponse> listar(String placa, String marca, String modelo,
                                                CategoriaVeiculo categoria, StatusVeiculo status,
                                                BigDecimal valorMin, BigDecimal valorMax, Pageable pageable) {
        return PageResponse.from(
                veiculoRepository.findAll(
                                VeiculoSpecs.filtro(placa, marca, modelo, categoria, status, valorMin, valorMax),
                                pageable)
                        .map(VeiculoResponse::from));
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscar(Long id) {
        return VeiculoResponse.from(buscarEntidade(id));
    }

    @Transactional
    public VeiculoResponse criar(VeiculoRequest req) {
        String placa = normalizarPlaca(req.placa());
        if (veiculoRepository.existsByPlaca(placa)) {
            throw new ConflitoException("Já existe um veículo cadastrado com a placa " + placa);
        }
        StatusVeiculo status = req.status() != null ? req.status() : StatusVeiculo.DISPONIVEL;
        if (status == StatusVeiculo.ALUGADO) {
            throw new RegraNegocioException("O status ALUGADO é definido apenas ao registrar uma locação");
        }

        Veiculo veiculo = new Veiculo();
        preencher(veiculo, req, placa);
        veiculo.setStatus(status);
        return VeiculoResponse.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest req) {
        Veiculo veiculo = buscarEntidade(id);
        String placa = normalizarPlaca(req.placa());
        if (veiculoRepository.existsByPlacaAndIdNot(placa, id)) {
            throw new ConflitoException("Já existe outro veículo cadastrado com a placa " + placa);
        }

        StatusVeiculo atual = veiculo.getStatus();
        StatusVeiculo novo = req.status() != null ? req.status() : atual;
        if (atual == StatusVeiculo.ALUGADO && novo != StatusVeiculo.ALUGADO) {
            throw new RegraNegocioException(
                    "Veículo alugado: finalize ou cancele a locação antes de alterar o status");
        }
        if (atual != StatusVeiculo.ALUGADO && novo == StatusVeiculo.ALUGADO) {
            throw new RegraNegocioException("O status ALUGADO é definido apenas ao registrar uma locação");
        }
        if (req.quilometragem() < veiculo.getQuilometragem()) {
            throw new RegraNegocioException(
                    "A quilometragem não pode ser reduzida (atual: " + veiculo.getQuilometragem() + " km)");
        }

        preencher(veiculo, req, placa);
        veiculo.setStatus(novo);
        return VeiculoResponse.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public void excluir(Long id) {
        Veiculo veiculo = buscarEntidade(id);
        if (veiculo.getStatus() == StatusVeiculo.ALUGADO) {
            throw new ConflitoException("Veículo está alugado e não pode ser excluído");
        }
        if (locacaoRepository.existsByVeiculoId(id)) {
            throw new ConflitoException("Veículo possui locações registradas e não pode ser excluído");
        }
        veiculoRepository.delete(veiculo);
    }

    private Veiculo buscarEntidade(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", id));
    }

    static String normalizarPlaca(String placa) {
        return placa.replace("-", "").trim().toUpperCase();
    }

    private void preencher(Veiculo veiculo, VeiculoRequest req, String placa) {
        veiculo.setPlaca(placa);
        veiculo.setMarca(req.marca().trim());
        veiculo.setModelo(req.modelo().trim());
        veiculo.setAno(req.ano());
        veiculo.setCategoria(req.categoria());
        veiculo.setValorDiaria(CalculadoraLocacao.money(req.valorDiaria()));
        veiculo.setQuilometragem(req.quilometragem());
    }
}
