package br.unifor.healthsys.user.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    // Dentro do cluster o fabric8 detecta sozinho a config in-cluster (token + CA
    // montados na ServiceAccount). O build e lazy: nao conecta no startup, entao a
    // app sobe normalmente mesmo rodando fora do Kubernetes (dev local).
    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
