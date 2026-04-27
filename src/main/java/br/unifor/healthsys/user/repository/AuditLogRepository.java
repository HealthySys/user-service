package br.unifor.healthsys.user.repository;

import br.unifor.healthsys.user.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
