package com.islandpacific.sentinel.security;

import com.islandpacific.sentinel.service.EntitlementService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
public class EntitlementAspect {

    private final EntitlementService entitlementService;

    @Autowired
    public EntitlementAspect(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @Before("@annotation(com.islandpacific.sentinel.security.RequiresEntitlement) || @within(com.islandpacific.sentinel.security.RequiresEntitlement)")
    public void verifyEntitlement(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresEntitlement annotation = method.getAnnotation(RequiresEntitlement.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequiresEntitlement.class);
        }

        if (annotation != null) {
            EntitlementTier requiredTier = annotation.value();
            UUID tenantId = TenantContextHolder.getRequiredTenantId();
            entitlementService.checkEntitlement(tenantId, requiredTier);
        }
    }
}
