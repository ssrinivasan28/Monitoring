package com.islandpacific.sentinel;

import com.islandpacific.sentinel.service.RedactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RedactionServiceTest {

    private RedactionService redactionService;

    @BeforeEach
    void setUp() {
        redactionService = new RedactionService();
    }

    @Test
    void redactsAnthropicAndOpenAiApiKeys() {
        String input = "Using anthropic key sk-ant-api03-abcdef1234567890 and openai key sk-proj-1234567890abcdefghij for model call";
        String redacted = redactionService.redact(input);

        assertThat(redacted).doesNotContain("sk-ant-api03-abcdef1234567890");
        assertThat(redacted).doesNotContain("sk-proj-1234567890abcdefghij");
        assertThat(redacted).contains("[SECRET_REDACTED]");
    }

    @Test
    void redactsBearerTokensAndPasswordKeyValues() {
        String input = "Header Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature and password=superSecretPassword123";
        String redacted = redactionService.redact(input);

        assertThat(redacted).doesNotContain("superSecretPassword123");
        assertThat(redacted).contains("Bearer [SECRET_REDACTED]");
        assertThat(redacted).contains("password=[SECRET_REDACTED]");
    }

    @Test
    void redactsDbConnectionUrisAndAwsKeys() {
        String input = "DB url postgres://admin:secretPass123@db.example.com:5432/mydb with AKIAIOSFODNN7EXAMPLE key";
        String redacted = redactionService.redact(input);

        assertThat(redacted).doesNotContain("secretPass123");
        assertThat(redacted).doesNotContain("AKIAIOSFODNN7EXAMPLE");
        assertThat(redacted).contains("postgres://admin:[SECRET_REDACTED]@");
        assertThat(redacted).contains("[SECRET_REDACTED]");
    }

    @Test
    void redactsPiiEmailSsnCreditCardPhoneAndIp() {
        String input = "User jane.doe@example.com (SSN: 123-45-6789, Phone: 555-123-4567, Card: 4111-1111-1111-1111) connecting from 192.168.1.100";
        String redacted = redactionService.redact(input);

        assertThat(redacted).doesNotContain("jane.doe@example.com");
        assertThat(redacted).doesNotContain("123-45-6789");
        assertThat(redacted).doesNotContain("555-123-4567");
        assertThat(redacted).doesNotContain("192.168.1.100");

        assertThat(redacted).contains("[EMAIL_REDACTED]");
        assertThat(redacted).contains("[SSN_REDACTED]");
        assertThat(redacted).contains("[PHONE_REDACTED]");
        assertThat(redacted).contains("[IP_REDACTED]");
    }

    @Test
    void redactsDpapiAndPrivateKeys() {
        String input = "Encrypted DPAPI(AQAAANCMnd8BFdERjHoAwEACAAA...) and key -----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC6...\n-----END PRIVATE KEY-----";
        String redacted = redactionService.redact(input);

        assertThat(redacted).doesNotContain("AQAAANCMnd8BFdERjHoAwEACAAA...");
        assertThat(redacted).doesNotContain("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC6...");
        assertThat(redacted).contains("[SECRET_REDACTED]");
    }
}
