package br.unifor.healthsys.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HealthSys - User Service API",
                version = "1.0.0",
                description = """
                        Serviço de autenticação e gestão de usuários da plataforma HealthSys.

                        Responsabilidades:
                        - Login, emissão e renovação de tokens JWT (`/api/auth`)
                        - Publicação das chaves públicas para validação dos tokens (`/.well-known/jwks.json`)
                        - CRUD de usuários e perfis (`/api/users`)

                        Os tokens emitidos aqui são validados pelos demais serviços do ecossistema.""",
                contact = @Contact(name = "HealthSys - UNIFOR", email = "healthsys@unifor.br"),
                license = @License(name = "Uso acadêmico/interno")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtido em POST /api/auth/login. Informe apenas o token (sem o prefixo 'Bearer')."
)
public class OpenApiConfig {
}
