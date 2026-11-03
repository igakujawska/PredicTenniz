package com.example.predictteniz.config;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiSessionController {
    @GetMapping("/api/session")
    public Session session(CsrfToken token) {
        return new Session(token.getHeaderName(), token.getToken());
    }

    public record Session(String headerName, String token) {}
}
