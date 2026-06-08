package br.unifor.healthsys.user.service;

import br.unifor.healthsys.user.dto.LoadTestStatusResponse;
import br.unifor.healthsys.user.exception.ConflictException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Dispara e acompanha o teste de carga (k6) como um Job do Kubernetes.
 *
 * A carga e gerada DENTRO do cluster (bate em {@code http://frontend}), entao
 * nao depende de VPN/rede externa. O resumo final do k6 (handleSummary do
 * script) sai no log do pod entre marcadores; aqui a gente le, parseia e guarda
 * em memoria pro painel admin exibir mesmo depois do Job se auto-limpar.
 */
@Service
public class LoadTestService {

    private static final Logger log = LoggerFactory.getLogger(LoadTestService.class);

    private static final Map<String, String> LABELS = Map.of("app", "healthysys-loadtest");
    private static final String SCRIPT_CONFIGMAP = "loadtest-script";
    private static final String CREDENTIALS_SECRET = "loadtest-credentials";
    private static final String SCRIPT_PATH = "/scripts/healthysys-load.js";
    private static final String MARK_START = "===K6_SUMMARY===";
    private static final String MARK_END = "===K6_SUMMARY_END===";
    private static final String DEFAULT_NAMESPACE = "healthysys";

    private final KubernetesClient client;
    private final ObjectMapper objectMapper;
    private final String image;
    private final String baseUrl;

    // Estado em memoria do ultimo/atual teste. Acesso sempre dentro de metodos
    // synchronized, entao um teste por vez.
    private String status = "IDLE";
    private String jobName;
    private Instant startedAt;
    private Instant finishedAt;
    private Map<String, Object> summary;
    private String message;

    public LoadTestService(KubernetesClient client,
                           ObjectMapper objectMapper,
                           @Value("${loadtest.image:grafana/k6:0.49.0}") String image,
                           @Value("${loadtest.base-url:http://frontend}") String baseUrl) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.image = image;
        this.baseUrl = baseUrl;
    }

    public synchronized LoadTestStatusResponse start() {
        refresh();
        if ("RUNNING".equals(status) || hasActiveJobInCluster()) {
            throw new ConflictException("Ja existe um teste de carga em execucao. Aguarde ele terminar.");
        }

        String name = "loadtest-" + Instant.now().getEpochSecond();
        client.batch().v1().jobs().inNamespace(namespace()).resource(buildJob(name)).create();

        jobName = name;
        startedAt = Instant.now();
        finishedAt = null;
        summary = null;
        message = null;
        status = "RUNNING";
        log.info("Teste de carga iniciado: Job {}", name);
        return toResponse();
    }

    public synchronized LoadTestStatusResponse getStatus() {
        refresh();
        return toResponse();
    }

    /** Atualiza o estado em memoria a partir do Job no cluster. */
    private void refresh() {
        if (jobName == null) {
            return;
        }
        // Ja chegou num estado final pra este Job: nada a reavaliar (o resumo ja
        // foi capturado e persiste mesmo que o Job se auto-limpe).
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            return;
        }

        Job job = client.batch().v1().jobs().inNamespace(namespace()).withName(jobName).get();
        if (job == null || job.getStatus() == null) {
            return; // recem-criado, sem status ainda -> segue RUNNING
        }

        JobStatus st = job.getStatus();
        boolean succeeded = st.getSucceeded() != null && st.getSucceeded() > 0;
        boolean failed = st.getFailed() != null && st.getFailed() > 0;

        if (succeeded) {
            finishedAt = Instant.now();
            summary = readSummary(jobName);
            status = "COMPLETED";
        } else if (failed) {
            finishedAt = Instant.now();
            summary = readSummary(jobName);
            message = "O teste terminou com falha. Verifique os logs do pod do Job " + jobName + ".";
            status = "FAILED";
        } else {
            status = "RUNNING";
        }
    }

    private boolean hasActiveJobInCluster() {
        return client.batch().v1().jobs().inNamespace(namespace()).withLabels(LABELS).list().getItems().stream()
                .anyMatch(j -> j.getStatus() != null
                        && j.getStatus().getActive() != null
                        && j.getStatus().getActive() > 0);
    }

    /** Le o log do pod do Job e extrai o bloco JSON do resumo, se presente. */
    private Map<String, Object> readSummary(String job) {
        try {
            var pods = client.pods().inNamespace(namespace()).withLabel("job-name", job).list().getItems();
            if (pods.isEmpty()) {
                return null;
            }
            String podLog = client.pods().inNamespace(namespace())
                    .withName(pods.get(0).getMetadata().getName()).getLog();
            if (podLog == null) {
                return null;
            }
            int start = podLog.indexOf(MARK_START);
            int end = podLog.indexOf(MARK_END);
            if (start < 0 || end < 0 || end <= start) {
                return null;
            }
            String json = podLog.substring(start + MARK_START.length(), end).trim();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Falha ao ler o resumo do teste de carga (Job {})", job, e);
            return null;
        }
    }

    private Job buildJob(String name) {
        return new JobBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withLabels(LABELS)
                .endMetadata()
                .withNewSpec()
                    .withBackoffLimit(0)
                    .withTtlSecondsAfterFinished(600)
                    .withNewTemplate()
                        .withNewMetadata().withLabels(LABELS).endMetadata()
                        .withNewSpec()
                            .withRestartPolicy("Never")
                            .addNewContainer()
                                .withName("k6")
                                .withImage(image)
                                .withCommand("k6", "run", SCRIPT_PATH)
                                .addNewEnv().withName("BASE_URL").withValue(baseUrl).endEnv()
                                .addNewEnv()
                                    .withName("USERNAME")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName(CREDENTIALS_SECRET).withKey("USERNAME")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("PASSWORD")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName(CREDENTIALS_SECRET).withKey("PASSWORD")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewVolumeMount()
                                    .withName("script").withMountPath("/scripts")
                                .endVolumeMount()
                            .endContainer()
                            .addNewVolume()
                                .withName("script")
                                .withNewConfigMap().withName(SCRIPT_CONFIGMAP).endConfigMap()
                            .endVolume()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }

    private String namespace() {
        String ns = client.getNamespace();
        return (ns == null || ns.isBlank()) ? DEFAULT_NAMESPACE : ns;
    }

    private LoadTestStatusResponse toResponse() {
        return new LoadTestStatusResponse(status, jobName, startedAt, finishedAt, summary, message);
    }
}
