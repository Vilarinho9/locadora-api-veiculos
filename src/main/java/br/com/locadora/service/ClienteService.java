package br.com.locadora.service;

import br.com.locadora.domain.Cliente;
import br.com.locadora.dto.ClienteRequest;
import br.com.locadora.dto.ClienteResponse;
import br.com.locadora.dto.PageResponse;
import br.com.locadora.exception.ConflitoException;
import br.com.locadora.exception.RecursoNaoEncontradoException;
import br.com.locadora.exception.RegraNegocioException;
import br.com.locadora.repository.ClienteRepository;
import br.com.locadora.repository.ClienteSpecs;
import br.com.locadora.repository.LocacaoRepository;
import br.com.locadora.validation.CpfUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

@Service
public class ClienteService {

    public static final int IDADE_MINIMA_CADASTRO = 18;

    private final ClienteRepository clienteRepository;
    private final LocacaoRepository locacaoRepository;
    private final Clock clock;

    public ClienteService(ClienteRepository clienteRepository, LocacaoRepository locacaoRepository, Clock clock) {
        this.clienteRepository = clienteRepository;
        this.locacaoRepository = locacaoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> listar(String nome, String cpf, String email, Pageable pageable) {
        return PageResponse.from(
                clienteRepository.findAll(ClienteSpecs.filtro(nome, cpf, email), pageable)
                        .map(ClienteResponse::from));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return ClienteResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest req) {
        String cpf = CpfUtil.somenteDigitos(req.cpf());
        if (clienteRepository.existsByCpf(cpf)) {
            throw new ConflitoException("Já existe um cliente cadastrado com este CPF");
        }
        if (clienteRepository.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ConflitoException("Já existe um cliente cadastrado com este e-mail");
        }
        validarIdade(req.dataNascimento());

        Cliente cliente = new Cliente();
        preencher(cliente, req, cpf);
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest req) {
        Cliente cliente = buscarEntidade(id);
        String cpf = CpfUtil.somenteDigitos(req.cpf());
        if (clienteRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new ConflitoException("Já existe outro cliente cadastrado com este CPF");
        }
        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(req.email().trim(), id)) {
            throw new ConflitoException("Já existe outro cliente cadastrado com este e-mail");
        }
        validarIdade(req.dataNascimento());

        preencher(cliente, req, cpf);
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidade(id);
        if (locacaoRepository.existsByClienteId(id)) {
            throw new ConflitoException("Cliente possui locações registradas e não pode ser excluído");
        }
        clienteRepository.delete(cliente);
    }

    private Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    private void validarIdade(LocalDate nascimento) {
        int idade = Period.between(nascimento, LocalDate.now(clock)).getYears();
        if (idade < IDADE_MINIMA_CADASTRO) {
            throw new RegraNegocioException("Cliente deve ter no mínimo " + IDADE_MINIMA_CADASTRO + " anos");
        }
    }

    private void preencher(Cliente cliente, ClienteRequest req, String cpf) {
        cliente.setNome(req.nome().trim());
        cliente.setCpf(cpf);
        cliente.setEmail(req.email().trim());
        cliente.setTelefone(req.telefone() == null || req.telefone().isBlank() ? null : req.telefone().trim());
        cliente.setDataNascimento(req.dataNascimento());
        cliente.setCnhNumero(req.cnhNumero());
        cliente.setCnhValidade(req.cnhValidade());
    }
}
