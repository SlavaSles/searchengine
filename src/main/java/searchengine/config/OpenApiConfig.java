package searchengine.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(info = @Info(
        title = "Searching engine API",
        description = "Searching engine",
        version = "1.0.0",
        contact = @Contact(
                name = "Vyacheslav Sles"
        )
))

public class OpenApiConfig {
}
