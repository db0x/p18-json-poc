package de.poc.json;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jsonPocOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JSON Document API")
                        .description("REST API for storing, retrieving, updating "
                                + "and searching JSON documents.")
                        .version("0.0.1")
                        .contact(new Contact().email("thomas.schwerdt@ituc.de")));
    }
}