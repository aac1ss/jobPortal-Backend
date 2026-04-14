

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")  // allow all endpoints
                        .allowedOrigins("http://localhost:5173") // your React frontend
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // all methods
                        .allowedHeaders("*") // allow any headers
                        .allowCredentials(true);
            }
        };
    }
}
