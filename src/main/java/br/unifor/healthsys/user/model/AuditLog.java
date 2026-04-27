package br.unifor.healthsys.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    @Column(name = "ip_address", nullable = false, length = 80)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    void onCreate() {
        timestamp = Instant.now();
    }
}
