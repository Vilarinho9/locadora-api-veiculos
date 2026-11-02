package br.com.locadora.controller;

import br.com.locadora.dto.ClienteRequest;
import br.com.locadora.dto.ClienteResponse;
import br.com.locadora.dto.ErroResponse;
import br.com.locadora.dto.PageResponse;
import br.com.locadora.service.ClienteService;
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

import java.net.URI;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "CRUD, filtros e paginação de clientes")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflito (CPF/e-mail duplicado ou cliente com locações)",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "422", description = "Regra de negócio violada",
                content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista clientes",
            description = "Filtros opcionais por nome, CPF e e-mail (busca parcial, sem diferenciar maiúsculas).")
    public PageResponse<ClienteResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String email,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return service.listar(nome, cpf, email, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo id")
    public ClienteResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um cliente",
            description = "Valida CPF (dígitos verificadores) e exige idade mínima de 18 anos.")
    @ApiResponse(responseCode = "201", description = "Cliente criado")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse criado = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cliente")
    public ClienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um cliente",
            description = "Bloqueado (409) se o cliente possuir locações registradas.")
    @ApiResponse(responseCode = "204", description = "Cliente excluído")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
