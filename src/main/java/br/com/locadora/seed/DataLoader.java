package br.com.locadora.seed;

import br.com.locadora.domain.CategoriaVeiculo;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.dto.ClienteRequest;
import br.com.locadora.dto.LocacaoRequest;
import br.com.locadora.dto.VeiculoRequest;
import br.com.locadora.repository.VeiculoRepository;
import br.com.locadora.service.ClienteService;
import br.com.locadora.service.LocacaoService;
import br.com.locadora.service.VeiculoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Popula o banco com dados de demonstração na primeira execução (banco vazio).
 * Desative com {@code app.seed=false}.
 *
 * <p>Os dados foram escolhidos para permitir demonstrar as regras de negócio:
 * cliente jovem (bloqueado em LUXO/ESPORTIVO), cliente com CNH vencida e veículo em manutenção.
 */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final VeiculoRepository veiculoRepository;
    private final VeiculoService veiculoService;
    private final ClienteService clienteService;
    private final LocacaoService locacaoService;
    private final Clock clock;

    public DataLoader(VeiculoRepository veiculoRepository, VeiculoService veiculoService,
                      ClienteService clienteService, LocacaoService locacaoService, Clock clock) {
        this.veiculoRepository = veiculoRepository;
        this.veiculoService = veiculoService;
        this.clienteService = clienteService;
        this.locacaoService = locacaoService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (veiculoRepository.count() > 0) {
            return;
        }
        LocalDate hoje = LocalDate.now(clock);

        var argo = veiculoService.criar(veiculo("ABC1D23", "Fiat", "Argo", 2023, CategoriaVeiculo.ECONOMICO, "120.00", 15200, null));
        veiculoService.criar(veiculo("RIO2E45", "Hyundai", "HB20", 2022, CategoriaVeiculo.ECONOMICO, "130.00", 28400, null));
        veiculoService.criar(veiculo("QWE3F67", "Volkswagen", "T-Cross", 2023, CategoriaVeiculo.SUV, "220.00", 12000, null));
        veiculoService.criar(veiculo("JKL4G89", "Toyota", "Corolla", 2024, CategoriaVeiculo.INTERMEDIARIO, "250.00", 8000, null));
        veiculoService.criar(veiculo("MNO5H01", "Jeep", "Compass", 2023, CategoriaVeiculo.SUV, "300.00", 21000, null));
        veiculoService.criar(veiculo("PQR6I23", "BMW", "320i", 2023, CategoriaVeiculo.LUXO, "520.00", 9500, null));
        veiculoService.criar(veiculo("STU7J45", "Chevrolet", "Camaro", 2022, CategoriaVeiculo.ESPORTIVO, "700.00", 14000, null));
        veiculoService.criar(veiculo("VWX8K67", "Fiat", "Mobi", 2021, CategoriaVeiculo.ECONOMICO, "100.00", 45000, StatusVeiculo.MANUTENCAO));

        var maria = clienteService.criar(cliente("Maria da Silva", "529.982.247-25", "maria@email.com",
                "(86) 99999-0001", LocalDate.of(1990, 3, 15), "12345678901", hoje.plusYears(3)));
        clienteService.criar(cliente("Carlos Souza", "111.444.777-35", "carlos@email.com",
                "(86) 99999-0002", LocalDate.of(1985, 7, 2), "23456789012", hoje.plusYears(2)));
        // 23 anos: pode alugar, mas não categorias LUXO/ESPORTIVO
        clienteService.criar(cliente("Ana Costa", "123.456.789-09", "ana@email.com",
                "(86) 99999-0003", hoje.minusYears(23).minusDays(40), "34567890123", hoje.plusYears(4)));
        // CNH vencida: não consegue alugar
        clienteService.criar(cliente("Pedro Lima", "390.533.447-05", "pedro@email.com",
                "(86) 99999-0004", LocalDate.of(1978, 11, 30), "45678901234", hoje.minusMonths(2)));

        locacaoService.criar(new LocacaoRequest(maria.id(), argo.id(), hoje, hoje.plusDays(3), "Locação de demonstração"));

        log.info("Dados de demonstração carregados: 8 veículos, 4 clientes e 1 locação ativa");
    }

    private VeiculoRequest veiculo(String placa, String marca, String modelo, int ano, CategoriaVeiculo categoria,
                                   String diaria, int km, StatusVeiculo status) {
        return new VeiculoRequest(placa, marca, modelo, ano, categoria, new BigDecimal(diaria), km, status);
    }

    private ClienteRequest cliente(String nome, String cpf, String email, String telefone,
                                   LocalDate nascimento, String cnh, LocalDate cnhValidade) {
        return new ClienteRequest(nome, cpf, email, telefone, nascimento, cnh, cnhValidade);
    }
}
