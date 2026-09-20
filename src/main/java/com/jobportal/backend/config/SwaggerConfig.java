package com.jobportal.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setEmail("admin@jobportal.com");
        contact.setName("Job Portal Team");
        contact.setUrl("https://jobportal.com");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Job Portal API")
                .version("1.0")
                .contact(contact)
                .description("""
                    This API exposes endpoints for Job Portal application.
                    
                    ## Authentication
                    1. **Role-Specific Login Endpoints:**
                       - `POST /api/auth/candidate/login` - For candidate users
                       - `POST /api/auth/recruiter/login` - For recruiter users  
                       - `POST /api/auth/admin/login` - For admin users
                    2. **Registration:** `POST /api/auth/signup`
                    3. **After login**, click the **Authorize** button above to set the JWT token for protected endpoints.
                    
                    ## Roles
                    - **ADMIN**: Full system access
                    - **RECRUITER**: Job posting and candidate management
                    - **CANDIDATE**: Job searching and applications
                    """)
                .license(mitLicense);

        // JWT Security Scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Security Requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}