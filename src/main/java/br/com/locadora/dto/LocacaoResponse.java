package br.com.locadora.dto;

import br.com.locadora.domain.Locacao;
import br.com.locadora.domain.StatusLocacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LocacaoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        Long veiculoId,
        String veiculoDescricao,
        LocalDate dataRetirada,
        LocalDate dataPrevistaDevolucao,
        LocalDate dataDevolucao,
        BigDecimal valorDiaria,
        BigDecimal desconto,
        BigDecimal valorPrevisto,
        BigDecimal multa,
        BigDecimal valorFinal,
        StatusLocacao status,
        boolean atrasada,
        String observacoes) {

    public static LocacaoResponse from(Locacao l, LocalDate hoje) {
        boolean atrasada = l.getStatus() == StatusLocacao.ATIVA && l.getDataPrevistaDevolucao().isBefore(hoje);
        return new LocacaoResponse(
                l.getId(),
                l.getCliente().getId(),
                l.getCliente().getNome(),
                l.getVeiculo().getId(),
                l.getVeiculo().getPlaca() + " - " + l.getVeiculo().getMarca() + " " + l.getVeiculo().getModelo(),
                l.getDataRetirada(),
                l.getDataPrevistaDevolucao(),
                l.getDataDevolucao(),
                l.getValorDiaria(),
                l.getDesconto(),
                l.getValorPrevisto(),
                l.getMulta(),
                l.getValorFinal(),
                l.getStatus(),
                atrasada,
                l.getObservacoes());
    }
}
