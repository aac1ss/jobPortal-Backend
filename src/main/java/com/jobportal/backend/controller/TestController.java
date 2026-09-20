package com.jobportal.backend.controller;
import com.jobportal.backend.dto.GenericResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    private final Environment env;

    public TestController(Environment env) {
        this.env = env;
    }

    @GetMapping("/test")
    public ResponseEntity<GenericResponse<String>> testEndpoint() {
        String[] profiles = env.getActiveProfiles();
        String activeProfile = profiles.length > 0 ? profiles[0] : "default";
        String message = "Application is running! Active profile: " + activeProfile;
        return ResponseEntity.ok(GenericResponse.success(message));
    }
}
