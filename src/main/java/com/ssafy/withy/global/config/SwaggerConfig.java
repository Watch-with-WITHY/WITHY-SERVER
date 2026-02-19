package com.ssafy.withy.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

        private final org.springframework.core.env.Environment env;

        public SwaggerConfig(org.springframework.core.env.Environment env) {
                this.env = env;
        }

        @Bean
        public OpenAPI openAPI() {
                io.swagger.v3.oas.models.servers.Server localServer = new io.swagger.v3.oas.models.servers.Server()
                                .url("http://localhost:8080").description("Local Server");
                io.swagger.v3.oas.models.servers.Server prodServer = new io.swagger.v3.oas.models.servers.Server()
                                .url("https://3.36.95.236.nip.io").description("Production Server");

                OpenAPI openAPI = new OpenAPI();

                if (java.util.Arrays.asList(env.getActiveProfiles()).contains("prod")) {
                        openAPI.addServersItem(prodServer);
                        openAPI.addServersItem(localServer);
                } else {
                        openAPI.addServersItem(localServer);
                        openAPI.addServersItem(prodServer);
                }

                return openAPI
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .info(apiInfo());
        }

        private Info apiInfo() {
                return new Info()
                                .title("Withy API")
                                .description("SSAFY 공통 프로젝트 Withy API 명세서")
                                .version("v1.0");
        }
}
