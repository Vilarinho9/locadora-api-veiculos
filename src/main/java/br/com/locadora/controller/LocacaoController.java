package br.com.locadora.controller;

import br.com.locadora.domain.StatusLocacao;
import br.com.locadora.dto.DevolucaoRequest;
import br.com.locadora.dto.ErroResponse;
import br.com.locadora.dto.LocacaoRequest;
import br.com.locadora.dto.LocacaoResponse;
import br.com.locadora.dto.LocacaoUpdateRequest;
import br.com.locadora.dto.PageResponse;
import br.com.locadora.service.LocacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/locacoes")
@Tag(name = "Locações", description = "Locação de veículos: criação com regras de negócio, devolução e cancelamento")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "404", description = "Locação, cliente ou veículo não encontrado",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflito de estado ou concorrência",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada",
                content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class LocacaoController {

    private final LocacaoService service;

    public LocacaoController(LocacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista locações",
            description = "Filtros opcionais: status, cliente, veículo, período de retirada e apenas atrasadas.")
    public PageResponse<LocacaoResponse> listar(
            @RequestParam(required = false) StatusLocacao status,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long veiculoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate retiradaDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate retiradaAte,
            @RequestParam(required = false) Boolean atrasadas,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.listar(status, clienteId, veiculoId, retiradaDe, retiradaAte, atrasadas, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma locação pelo id")
    public LocacaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Registra uma locação",
            description = "Aplica as regras: idade mínima (21; 25 para LUXO/ESPORTIVO), CNH válida até a devolução, "
                    + "veículo disponível, máximo de 2 locações ativas por cliente, período de até 30 dias. "
                    + "Calcula o valor previsto com desconto progressivo (7+ dias: 10%; 15+ dias: 15%).")
    @ApiResponse(responseCode = "201", description = "Locação criada; veículo passa a ALUGADO")
    public ResponseEntity<LocacaoResponse> criar(@Valid @RequestBody LocacaoRequest request) {
        LocacaoResponse criada = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera data prevista de devolução e observações",
            description = "Somente locações ativas que ainda não foram iniciadas. O valor previsto é recalculado.")
    public LocacaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody LocacaoUpdateRequest request) {
        return service.atualizar(id, request);
    }

    @PostMapping("/{id}/devolucao")
    @Operation(summary = "Registra a devolução do veículo",
            description = "Calcula o valor final: atraso = dias extras + multa de 20% da diária por dia; "
                    + "devolução antecipada = dias usados + taxa de 10% da diária por dia não usado. "
                    + "Atualiza quilometragem e libera o veículo.")
    public LocacaoResponse devolver(@PathVariable Long id, @Valid @RequestBody DevolucaoRequest request) {
        return service.devolver(id, request);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma locação",
            description = "Somente locações ativas ainda não iniciadas. Libera o veículo.")
    public LocacaoResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma locação",
            description = "Bloqueado (409) para locações ativas. Cancele ou finalize antes.")
    @ApiResponse(responseCode = "204", description = "Locação excluída")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
