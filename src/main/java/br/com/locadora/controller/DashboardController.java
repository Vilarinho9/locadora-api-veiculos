package br.com.locadora.controller;

import br.com.locadora.dto.DashboardResponse;
import br.com.locadora.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores gerais da locadora")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Resumo: frota por status, clientes, locações ativas/atrasadas e receita")
    public DashboardResponse resumo() {
        return service.resumo();
    }
}
