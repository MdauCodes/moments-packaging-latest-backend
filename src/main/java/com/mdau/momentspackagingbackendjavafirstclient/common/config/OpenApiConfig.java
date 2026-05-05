package com.mdau.momentspackagingbackendjavafirstclient.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("Moments Packaging Kenya API")
                        .description("""
                            REST API for Moments Packaging Kenya.
                            
                            **Authentication:** JWT Bearer tokens via `/api/v1/auth/login`.
                            Access tokens expire in 15 minutes. Use `/api/v1/auth/refresh` to rotate.
                            
                            **Roles:**
                            - `ROLE_ADMIN` — full access including user management and destructive operations
                            - `ROLE_STAFF` — read/write access to products, blogs, enquiries
                            
                            **Rate limits:**
                            - Login: 10/min/IP
                            - Enquiries: 10/min/IP
                            - Leads: 20/min/IP
                            - Clicks: 60/min/IP
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Moments Packaging Kenya")
                                .email("info@momentspackaging.com")
                                .url("https://www.momentspackaging.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.momentspackaging.com").description("Production")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}