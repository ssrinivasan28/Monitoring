package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.security.EntitlementTier;
import com.islandpacific.sentinel.security.RequiresEntitlement;
import com.islandpacific.sentinel.security.TenantContextHolder;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiresEntitlement(EntitlementTier.PRO)
public class AssistantController {

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String prompt = request.getOrDefault("prompt", "");
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId.toString(),
                "status", "success",
                "answer", "AI Assistant response for prompt: " + prompt,
                "tier", "PRO"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId.toString(),
                "assistantEnabled", true,
                "tier", "PRO"
        ));
    }
}
