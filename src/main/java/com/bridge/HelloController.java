package com.bridge;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/health")
    public Health health() {
        return new Health("UP", "fhir-bridge");
    }

    public record Health(String status, String service) {}
}
