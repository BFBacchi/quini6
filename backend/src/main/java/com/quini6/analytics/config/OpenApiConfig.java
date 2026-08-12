package com.quini6.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Quini6 Analytics API")
                .version("1.0.0")
                .description("""
                    API de análisis estadístico histórico del Quini 6 (lotería argentina).

                    **IMPORTANTE:** Esta plataforma realiza únicamente análisis descriptivo
                    sobre datos pasados. Los sorteos son eventos independientes (i.i.d.).
                    Ningún resultado histórico predice el próximo sorteo.

                    El generador de combinaciones es una herramienta de ENTRETENIMIENTO
                    y no tiene ningún valor predictivo.
                    """)
                .contact(new Contact()
                    .name("Quini6 Analytics")
                    .url("https://github.com/quini6-analytics"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url("/").description("Servidor actual"),
                new Server().url("http://localhost:8080").description("Desarrollo local")
            ));
    }
}
