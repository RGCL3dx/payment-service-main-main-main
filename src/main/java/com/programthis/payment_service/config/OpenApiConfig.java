package com.programthis.payment_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Ecomarket - Servicio de Pagos",
        version = "1.0.0",
        description = "API RESTful para procesar, consultar y gestionar pagos de Ecomarket. Incluye soporte para HATEOAS y está documentada con OpenAPI 3.",
        contact = @Contact(
            name = "Equipo de Desarrollo Ecomarket",
            email = "dev-payments@ecomarket.com",
            url = "https://developer.ecomarket.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
        )
    ),
    security = @SecurityRequirement(name = "BearerAuth")
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Se requiere un token JWT para la autenticación. Ingrese 'Bearer' seguido de un espacio y el token."
)
public class OpenApiConfig {
}