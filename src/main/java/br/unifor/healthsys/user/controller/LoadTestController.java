package br.unifor.healthsys.user.controller;

import br.unifor.healthsys.user.dto.LoadTestStatusResponse;
import br.unifor.healthsys.user.service.LoadTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dispara/acompanha o teste de carga (k6) pelo painel admin.
 *
 * Fica sob /api/users/** de proposito: essa rota ja existe no gateway, entao
 * nao precisa mexer no api-gateway. O gateway exige JWT e aqui exigimos ADMIN.
 */
@RestController
@RequestMapping("/api/users/load-test")
@Tag(name = "Teste de carga", description = "Dispara e acompanha o teste de carga k6 (somente ADMIN)")
public class LoadTestController {

    private final LoadTestService loadTestService;

    public LoadTestController(LoadTestService loadTestService) {
        this.loadTestService = loadTestService;
    }

    @PostMapping
    @Operation(summary = "Dispara um teste de carga", description = "Cria um Job k6 no cluster. 409 se ja houver um rodando. Somente ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoadTestStatusResponse> start() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(loadTestService.start());
    }

    @GetMapping
    @Operation(summary = "Status do teste de carga", description = "Retorna o estado atual e, ao terminar, o resumo das metricas. Somente ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoadTestStatusResponse> status() {
        return ResponseEntity.ok(loadTestService.getStatus());
    }
}
