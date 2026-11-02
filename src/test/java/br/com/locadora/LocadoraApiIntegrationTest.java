package br.com.locadora;

import br.com.locadora.domain.CategoriaVeiculo;
import br.com.locadora.domain.Cliente;
import br.com.locadora.domain.Locacao;
import br.com.locadora.domain.StatusLocacao;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.domain.Veiculo;
import br.com.locadora.repository.ClienteRepository;
import br.com.locadora.repository.LocacaoRepository;
import br.com.locadora.repository.VeiculoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes de integração: sobem o contexto completo (H2 em memória) e chamam a API via MockMvc. */
@SpringBootTest
@AutoConfigureMockMvc
class LocadoraApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ClienteRepository clientes;
    @Autowired VeiculoRepository veiculos;
    @Autowired LocacaoRepository locacoes;
    @Autowired Clock clock;

    private int sequencia = 0;

    @BeforeEach
    void limpar() {
        locacoes.deleteAll();
        veiculos.deleteAll();
        clientes.deleteAll();
    }

    // ------------------------------------------------------------------ helpers

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private Cliente cliente(String cpf, LocalDate nascimento, LocalDate cnhValidade) {
        sequencia++;
        Cliente c = new Cliente();
        c.setNome("Cliente " + sequencia);
        c.setCpf(cpf);
        c.setEmail("cliente" + sequencia + "@teste.com");
        c.setDataNascimento(nascimento);
        c.setCnhNumero("1234567890" + (sequencia % 10));
        c.setCnhValidade(cnhValidade);
        return clientes.save(c);
    }

    private Cliente clienteAdulto() {
        return cliente("52998224725", hoje().minusYears(35), hoje().plusYears(3));
    }

    private Veiculo veiculo(String placa, CategoriaVeiculo categoria, String diaria) {
        Veiculo v = new Veiculo();
        v.setPlaca(placa);
        v.setMarca("Fiat");
        v.setModelo("Argo");
        v.setAno(2023);
        v.setCategoria(categoria);
        v.setValorDiaria(new BigDecimal(diaria));
        v.setQuilometragem(1000);
        v.setStatus(StatusVeiculo.DISPONIVEL);
        return veiculos.save(v);
    }

    private String json(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    private Map<String, Object> locacaoBody(Cliente c, Veiculo v, LocalDate retirada, LocalDate devolucao) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clienteId", c.getId());
        body.put("veiculoId", v.getId());
        body.put("dataRetirada", retirada.toString());
        body.put("dataPrevistaDevolucao", devolucao.toString());
        return body;
    }

    // ------------------------------------------------------------------ cliente / veículo

    @Test
    void clienteComCpfInvalidoRetorna400ComCampoNoErro() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", "Fulano");
        body.put("cpf", "111.111.111-11");
        body.put("email", "fulano@email.com");
        body.put("dataNascimento", "1990-01-01");
        body.put("cnhNumero", "12345678901");
        body.put("cnhValidade", hoje().plusYears(2).toString());

        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cpf"));
    }

    @Test
    void clienteComCpfDuplicadoRetorna409() throws Exception {
        clienteAdulto();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", "Outro Nome");
        body.put("cpf", "529.982.247-25");
        body.put("email", "outro@email.com");
        body.put("dataNascimento", "1990-01-01");
        body.put("cnhNumero", "12345678901");
        body.put("cnhValidade", hoje().plusYears(2).toString());

        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void criaVeiculoRetorna201ComLocationENormalizaPlaca() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("placa", "abc-1234");
        body.put("marca", "Fiat");
        body.put("modelo", "Uno");
        body.put("ano", 2020);
        body.put("categoria", "ECONOMICO");
        body.put("valorDiaria", 99.9);
        body.put("quilometragem", 500);

        mvc.perform(post("/api/veiculos").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.placa").value("ABC1234"))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"));
    }

    @Test
    void filtraVeiculosPorCategoriaEStatus() throws Exception {
        veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");
        veiculo("BBB2B22", CategoriaVeiculo.SUV, "200.00");
        Veiculo manutencao = veiculo("CCC3C33", CategoriaVeiculo.SUV, "210.00");
        manutencao.setStatus(StatusVeiculo.MANUTENCAO);
        veiculos.save(manutencao);

        mvc.perform(get("/api/veiculos").param("categoria", "SUV").param("status", "DISPONIVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].placa").value("BBB2B22"));
    }

    @Test
    void filtroComEnumInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/veiculos").param("status", "VOANDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void veiculoInexistenteRetorna404NoFormatoPadrao() throws Exception {
        mvc.perform(get("/api/veiculos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.caminho").value("/api/veiculos/999999"));
    }

    // ------------------------------------------------------------------ locação: regras de negócio

    @Test
    void criaLocacaoMarcaVeiculoComoAlugadoECalculaDescontoDeDezDias() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c, v, hoje(), hoje().plusDays(10)))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.desconto").value(100.0))
                .andExpect(jsonPath("$.valorPrevisto").value(900.0));

        assertEquals(StatusVeiculo.ALUGADO, veiculos.findById(v.getId()).orElseThrow().getStatus());
    }

    @Test
    void naoAlugaVeiculoJaAlugado() throws Exception {
        Cliente c1 = clienteAdulto();
        Cliente c2 = cliente("11144477735", hoje().minusYears(40), hoje().plusYears(2));
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c1, v, hoje(), hoje().plusDays(2)))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c2, v, hoje(), hoje().plusDays(2)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("indisponível")));
    }

    @Test
    void clienteComMenosDeVinteECincoAnosNaoAlugaVeiculoDeLuxo() throws Exception {
        Cliente jovem = cliente("52998224725", hoje().minusYears(23), hoje().plusYears(3));
        Veiculo luxo = veiculo("BMW1A11", CategoriaVeiculo.LUXO, "500.00");

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(jovem, luxo, hoje(), hoje().plusDays(2)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("25 anos")));
    }

    @Test
    void cnhVencidaAntesDaDevolucaoImpedeLocacao() throws Exception {
        Cliente c = cliente("52998224725", hoje().minusYears(35), hoje().plusDays(2));
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c, v, hoje(), hoje().plusDays(5)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("CNH")));
    }

    @Test
    void clienteNaoPodeTerMaisDeDuasLocacoesAtivas() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v1 = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");
        Veiculo v2 = veiculo("BBB2B22", CategoriaVeiculo.ECONOMICO, "100.00");
        Veiculo v3 = veiculo("CCC3C33", CategoriaVeiculo.ECONOMICO, "100.00");

        for (Veiculo v : new Veiculo[]{v1, v2}) {
            mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                            .content(json(locacaoBody(c, v, hoje(), hoje().plusDays(2)))))
                    .andExpect(status().isCreated());
        }

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c, v3, hoje(), hoje().plusDays(2)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("2 locações ativas")));
    }

    @Test
    void periodoMaiorQueTrintaDiasEhRejeitado() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c, v, hoje(), hoje().plusDays(31)))))
                .andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------------ devolução e cancelamento

    @Test
    void devolucaoComAtrasoCobraDiasExtrasMaisMulta() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");
        v.setStatus(StatusVeiculo.ALUGADO);
        veiculos.save(v);

        // locação iniciada há 10 dias, com devolução prevista há 5 dias (5 dias de atraso hoje)
        Locacao l = new Locacao();
        l.setCliente(c);
        l.setVeiculo(v);
        l.setDataRetirada(hoje().minusDays(10));
        l.setDataPrevistaDevolucao(hoje().minusDays(5));
        l.setValorDiaria(new BigDecimal("100.00"));
        l.setDesconto(BigDecimal.ZERO);
        l.setValorPrevisto(new BigDecimal("500.00"));
        l.setStatus(StatusLocacao.ATIVA);
        l = locacoes.save(l);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quilometragemFinal", 1500);

        // previsto 500 + 5 dias extras (500) + multa 20% (100) = 1100
        mvc.perform(post("/api/locacoes/" + l.getId() + "/devolucao")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.multa").value(100.0))
                .andExpect(jsonPath("$.valorFinal").value(1100.0));

        Veiculo atualizado = veiculos.findById(v.getId()).orElseThrow();
        assertEquals(StatusVeiculo.DISPONIVEL, atualizado.getStatus());
        assertEquals(1500, atualizado.getQuilometragem());
    }

    @Test
    void devolucaoAntecipadaCobraDiasUsadosMaisTaxa() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");
        v.setStatus(StatusVeiculo.ALUGADO);
        veiculos.save(v);

        // retirada há 2 dias, prevista daqui a 3 (5 dias previstos); devolve hoje → 2 usados, 3 não usados
        Locacao l = new Locacao();
        l.setCliente(c);
        l.setVeiculo(v);
        l.setDataRetirada(hoje().minusDays(2));
        l.setDataPrevistaDevolucao(hoje().plusDays(3));
        l.setValorDiaria(new BigDecimal("100.00"));
        l.setDesconto(BigDecimal.ZERO);
        l.setValorPrevisto(new BigDecimal("500.00"));
        l.setStatus(StatusLocacao.ATIVA);
        l = locacoes.save(l);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quilometragemFinal", 1200);

        // 2 diárias (200) + taxa de 10% sobre 3 dias não usados (30) = 230
        mvc.perform(post("/api/locacoes/" + l.getId() + "/devolucao")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multa").value(30.0))
                .andExpect(jsonPath("$.valorFinal").value(230.0));
    }

    @Test
    void naoDevolveLocacaoJaFinalizada() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        String resposta = mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c, v, hoje(), hoje().plusDays(2)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(resposta).get("id").asLong();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quilometragemFinal", 1100);

        mvc.perform(post("/api/locacoes/" + id + "/devolucao")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/locacoes/" + id + "/devolucao")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelarLiberaOVeiculo() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        String resposta = mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c, v, hoje().plusDays(1), hoje().plusDays(3)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(resposta).get("id").asLong();

        mvc.perform(patch("/api/locacoes/" + id + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        assertEquals(StatusVeiculo.DISPONIVEL, veiculos.findById(v.getId()).orElseThrow().getStatus());
    }

    // ------------------------------------------------------------------ integridade referencial

    @Test
    void naoExcluiClienteComLocacoesNemVeiculoAlugado() throws Exception {
        Cliente c = clienteAdulto();
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        mvc.perform(post("/api/locacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(locacaoBody(c, v, hoje(), hoje().plusDays(2)))))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/clientes/" + c.getId())).andExpect(status().isConflict());
        mvc.perform(delete("/api/veiculos/" + v.getId())).andExpect(status().isConflict());
    }

    @Test
    void excluiVeiculoSemHistoricoRetorna204() throws Exception {
        Veiculo v = veiculo("AAA1A11", CategoriaVeiculo.ECONOMICO, "100.00");

        mvc.perform(delete("/api/veiculos/" + v.getId())).andExpect(status().isNoContent());
        mvc.perform(get("/api/veiculos/" + v.getId())).andExpect(status().isNotFound());
    }
}
