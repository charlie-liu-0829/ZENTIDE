package com.zentide.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/platform")
public class PlatformController {

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "name", "知潮 ZENTIDE",
                "service", "admin-api",
                "phase", "community-governance",
                "status", "ready"
        );
    }
}
