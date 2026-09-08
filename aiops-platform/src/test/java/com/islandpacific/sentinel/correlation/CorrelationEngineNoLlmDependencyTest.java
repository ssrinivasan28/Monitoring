package com.islandpacific.sentinel.correlation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 1.1 must run fully with the LLM disabled (1.2 enriches later) — enforced structurally: the
 * correlation engine has zero compile-time dependency on the LLM package.
 */
class CorrelationEngineNoLlmDependencyTest {

    @Test
    void correlationEngineHasNoLlmTypeInItsSignature() {
        Class<?> clazz = CorrelationEngineService.class;

        for (Field field : clazz.getDeclaredFields()) {
            assertThat(field.getType().getName()).doesNotContain(".llm.");
        }
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            for (Class<?> paramType : ctor.getParameterTypes()) {
                assertThat(paramType.getName()).doesNotContain(".llm.");
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            for (Class<?> paramType : method.getParameterTypes()) {
                assertThat(paramType.getName()).doesNotContain(".llm.");
            }
            assertThat(method.getReturnType().getName()).doesNotContain(".llm.");
        }
    }
}
