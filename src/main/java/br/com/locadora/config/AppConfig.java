package br.com.locadora.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    /** Relógio injetável: facilita testes e garante o fuso horário de Brasília nas regras de data. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("API Locadora de Veículos")
                .version("1.0.0")
                .description("API REST para gestão de veículos, clientes e locações, com filtros, "
                        + "regras de negócio e tratamento padronizado de erros.")
                .contact(new Contact().name("Trabalho de Programação Backend")));
    }

    /** Permite que o front-end seja servido de outra origem durante o desenvolvimento. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
