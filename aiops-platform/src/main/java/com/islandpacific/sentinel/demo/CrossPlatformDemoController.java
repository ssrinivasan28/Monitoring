package com.islandpacific.sentinel.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 1.8 — staff-only trigger for the flagship cross-platform correlation demo. Seeds a fresh demo
 * tenant and fixture, then returns the resulting incident once 1.1 (correlation) and 1.2 (triage)
 * have run against it.
 */
@RestController
@RequestMapping("/api/v1/admin/demo")
public class CrossPlatformDemoController {

    private final CrossPlatformDemoService demoService;

    @Autowired
    public CrossPlatformDemoController(CrossPlatformDemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping("/cross-platform-incident")
    @PreAuthorize("hasRole('STAFF_ADMIN')")
    public ResponseEntity<CrossPlatformDemoResult> runCrossPlatformDemo() {
        return ResponseEntity.ok(demoService.runDemo());
    }
}
