package br.unifor.healthsys.user.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Estado do teste de carga exibido no painel admin.
 *
 * status: IDLE (nunca rodou / sem teste ativo), RUNNING (em execucao),
 *         COMPLETED (terminou), FAILED (terminou com erro de execucao).
 * summary: resumo do k6 ja parseado (vindo do handleSummary do script). Null
 *          enquanto nao ha resultado.
 */
public record LoadTestStatusResponse(
        String status,
        String jobName,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> summary,
        String message
) {
}
