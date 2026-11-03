package com.example.predictteniz.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final String frontendUrl;

    public DashboardController(@Value("${tennis.frontend.url:http://localhost:4200}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/")
    public String dashboard() { return "redirect:" + frontendUrl; }
}
