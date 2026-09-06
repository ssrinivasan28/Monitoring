package com.islandpacific.sentinel.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresEntitlement {
    EntitlementTier value() default EntitlementTier.PRO;
}
