package br.unifor.healthsys.user.audit;

import br.unifor.healthsys.user.dto.AuthResponse;
import br.unifor.healthsys.user.dto.AuthRequest;
import br.unifor.healthsys.user.dto.LogoutRequest;
import br.unifor.healthsys.user.dto.RefreshTokenRequest;
import br.unifor.healthsys.user.dto.UserRequest;
import br.unifor.healthsys.user.dto.UserResponse;
import br.unifor.healthsys.user.dto.UserUpdateRequest;
import br.unifor.healthsys.user.model.AuditLog;
import br.unifor.healthsys.user.repository.AuditLogRepository;
import br.unifor.healthsys.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    public AuditAspect(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            HttpServletRequest request
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.request = request;
    }

    @Around("@annotation(audited)")
    public Object recordAudit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        auditLogRepository.save(AuditLog.builder()
                .userId(resolveUserId(result))
                .action(audited.action())
                .resource(audited.resource())
                .resourceId(resolveResourceId(result, joinPoint.getArgs()))
                .ipAddress(request.getRemoteAddr())
                .build());

        return result;
    }

    private Long resolveUserId(Object result) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return userRepository.findByUsername(authentication.getName())
                    .or(() -> userRepository.findByEmail(authentication.getName()))
                    .map(user -> user.getId())
                    .orElseGet(() -> fromResult(result));
        }
        return fromResult(result);
    }

    private Long fromResult(Object result) {
        if (result instanceof UserResponse userResponse) {
            return userResponse.getId();
        }
        if (result instanceof AuthResponse authResponse) {
            return authResponse.getUserId();
        }
        return null;
    }

    private String resolveResourceId(Object result, Object[] args) {
        if (result instanceof UserResponse userResponse && userResponse.getId() != null) {
            return String.valueOf(userResponse.getId());
        }
        if (result instanceof AuthResponse authResponse && authResponse.getUserId() != null) {
            return String.valueOf(authResponse.getUserId());
        }
        if (args != null && args.length > 0 && args[0] != null) {
            Object firstArg = args[0];
            if (firstArg instanceof Number || firstArg instanceof CharSequence || firstArg instanceof java.util.UUID) {
                return truncate(String.valueOf(firstArg));
            }
            if (firstArg instanceof AuthRequest authRequest) {
                return truncate(authRequest.getUsername());
            }
            if (firstArg instanceof RefreshTokenRequest refreshTokenRequest) {
                return truncate(refreshTokenRequest.getRefreshToken());
            }
            if (firstArg instanceof LogoutRequest logoutRequest) {
                return truncate(logoutRequest.getRefreshToken());
            }
            if (firstArg instanceof UserRequest userRequest) {
                return truncate(userRequest.getUsername());
            }
            if (firstArg instanceof UserUpdateRequest userUpdateRequest) {
                return truncate(userUpdateRequest.getUsername());
            }
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 120 ? value : value.substring(0, 120);
    }
}
