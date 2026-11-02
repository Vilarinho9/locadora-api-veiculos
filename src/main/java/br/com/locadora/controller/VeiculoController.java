package br.com.locadora.controller;

import br.com.locadora.domain.CategoriaVeiculo;
import br.com.locadora.domain.StatusVeiculo;
import br.com.locadora.dto.ErroResponse;
import br.com.locadora.dto.PageResponse;
import br.com.locadora.dto.VeiculoRequest;
import br.com.locadora.dto.VeiculoResponse;
import br.com.locadora.service.VeiculoService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/veiculos")
@Tag(name = "Veículos", description = "CRUD, filtros e paginação da frota")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflito (placa duplicada ou veículo em uso)",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada",
                content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class VeiculoController {

    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista veículos",
            description = "Todos os filtros são opcionais e combináveis. Ordenação: ?sort=valorDiaria,desc")
    public PageResponse<VeiculoResponse> listar(
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) CategoriaVeiculo categoria,
            @RequestParam(required = false) StatusVeiculo status,
            @RequestParam(required = false) BigDecimal valorMin,
            @RequestParam(required = false) BigDecimal valorMax,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return service.listar(placa, marca, modelo, categoria, status, valorMin, valorMax, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um veículo pelo id")
    public VeiculoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um veículo")
    @ApiResponse(responseCode = "201", description = "Veículo criado")
    public ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request) {
        VeiculoResponse criado = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um veículo",
            description = "Não é possível definir status ALUGADO manualmente, nem reduzir a quilometragem.")
    public VeiculoResponse atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um veículo",
            description = "Bloqueado (409) se o veículo estiver alugado ou tiver histórico de locações.")
    @ApiResponse(responseCode = "204", description = "Veículo excluído")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
