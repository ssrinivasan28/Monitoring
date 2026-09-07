package com.islandpacific.sentinel.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redaction Service enforcing SOC 2 PII and secret stripping prior to external LLM calls or storage.
 */
@Service
public class RedactionService {

    // Secret Patterns
    private static final Pattern ANTHROPIC_KEY_PATTERN = Pattern.compile("sk-ant-[A-Za-z0-9_-]+");
    private static final Pattern OPENAI_KEY_PATTERN = Pattern.compile("sk-[A-Za-z0-9_-]{20,}");
    private static final Pattern GENERIC_API_KEY_PATTERN = Pattern.compile("(?:api[_-]?key|secret[_-]?key)\\s*[:=]\\s*[\"']?([A-Za-z0-9_-]{16,})[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9\\-\\._~\\+\\/]+=*");
    private static final Pattern PASSWORD_KV_PATTERN = Pattern.compile("(?i)(password|pwd|secret|client_secret|auth_token)\\s*[:=]\\s*[\"']?([^\"'\\s,;{}]+)[\"']?");
    private static final Pattern DB_URI_PATTERN = Pattern.compile("(?i)(postgres|postgresql|mysql|oracle|jdbc:[a-z0-9]+)://([^:\\s]+):([^@\\s]+)@");
    private static final Pattern AWS_KEY_PATTERN = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern AWS_SECRET_PATTERN = Pattern.compile("(?i)aws_secret_access_key\\s*=\\s*([A-Za-z0-9/+=]{40})");
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]+");
    private static final Pattern DPAPI_PATTERN = Pattern.compile("DPAPI\\([^)]+\\)");

    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile("-----BEGIN (?:RSA|EC|DSA|OPENSSH|PRIVATE) KEY-----[\\s\\S]*?-----END (?:RSA|EC|DSA|OPENSSH|PRIVATE) KEY-----");

    // PII Patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13})\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");

    /**
     * Redacts all secrets and PII from text context.
     *
     * @param text input string
     * @return redacted string with sensitive tokens masked
     */
    public String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // 1. Redact Private Keys
        result = PRIVATE_KEY_PATTERN.matcher(result).replaceAll("[SECRET_REDACTED]");

        // 2. Redact DPAPI Blobs
        result = DPAPI_PATTERN.matcher(result).replaceAll("[SECRET_REDACTED]");

        // 3. Redact JWTs
        result = JWT_PATTERN.matcher(result).replaceAll("[SECRET_REDACTED]");

        // 4. Redact Anthropic / OpenAI Keys
        result = ANTHROPIC_KEY_PATTERN.matcher(result).replaceAll("[SECRET_REDACTED]");
        result = OPENAI_KEY_PATTERN.matcher(result).replaceAll("[SECRET_REDACTED]");
        result = AWS_KEY_PATTERN.matcher(result).replaceAll("[SECRET_REDACTED]");

        // 5. Redact AWS secret access keys
        result = AWS_SECRET_PATTERN.matcher(result).replaceAll("aws_secret_access_key=[SECRET_REDACTED]");

        // 6. Redact Bearer tokens
        result = BEARER_TOKEN_PATTERN.matcher(result).replaceAll("Bearer [SECRET_REDACTED]");

        // 7. Redact generic API keys
        Matcher apiKeyMatcher = GENERIC_API_KEY_PATTERN.matcher(result);
        if (apiKeyMatcher.find()) {
            result = apiKeyMatcher.replaceAll(match -> match.group().replace(match.group(1), "[SECRET_REDACTED]"));
        }

        // 8. Redact Password Key-Values
        Matcher pwdMatcher = PASSWORD_KV_PATTERN.matcher(result);
        if (pwdMatcher.find()) {
            result = pwdMatcher.replaceAll(match -> match.group(1) + "=[SECRET_REDACTED]");
        }

        // 9. Redact DB URI credentials
        Matcher dbUriMatcher = DB_URI_PATTERN.matcher(result);
        if (dbUriMatcher.find()) {
            result = dbUriMatcher.replaceAll(match -> match.group(1) + "://" + match.group(2) + ":[SECRET_REDACTED]@");
        }

        // 10. Redact PII: SSN, Credit Cards, Emails, Phone numbers
        result = SSN_PATTERN.matcher(result).replaceAll("[SSN_REDACTED]");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[CREDIT_CARD_REDACTED]");
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL_REDACTED]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE_REDACTED]");

        // 11. Redact IP addresses (excluding localhost 127.0.0.1 if wanted, but standard IP redaction)
        result = redactIpAddresses(result);

        return result;
    }

    private String redactIpAddresses(String input) {
        Matcher matcher = IP_ADDRESS_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String ip = matcher.group();
            // Preserve 127.0.0.1 and 0.0.0.0 for dev/local diagnostics if needed, redact all others
            if ("127.0.0.1".equals(ip) || "0.0.0.0".equals(ip)) {
                matcher.appendReplacement(sb, ip);
            } else {
                matcher.appendReplacement(sb, "[IP_REDACTED]");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
