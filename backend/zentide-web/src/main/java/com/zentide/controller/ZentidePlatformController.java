package com.zentide.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/zentide/v1/platform")
public class ZentidePlatformController {

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "name", "知潮 ZENTIDE",
                "product", "personal-intelligence-network",
                "architecture", "deterministic-facts-plus-personal-meaning",
                "phase", "common-fact-model",
                "status", "migration-ready"
        );
    }
}
