package com.islandpacific.sentinel.query;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LogQL Lexer / AST Stream Selector Transformer.
 * Ensures tenant_id and __sentinel_source are strictly reserved labels.
 * Parses LogQL stream selectors ({...}) and injects authoritative tenant_id="<tenantId>".
 * Fails closed on syntax errors or invalid matcher contexts.
 */
@Component
public class LogQlAstSanitizer {

    private static final String RESERVED_TENANT_LABEL = "tenant_id";
    private static final String RESERVED_SOURCE_LABEL = "__sentinel_source";

    // Pattern matching label matchers inside stream selector braces e.g., tenant_id= or __sentinel_source=
    private static final Pattern RESERVED_LABEL_PATTERN = Pattern.compile(
            "(\"|'|\\b)(" + RESERVED_TENANT_LABEL + "|" + RESERVED_SOURCE_LABEL + ")(\"|'|\\b)\\s*(=|!=|=~|!~)"
    );

    // Matches LogQL stream selector braces: {...}
    private static final Pattern LOGQL_STREAM_SELECTOR_PATTERN = Pattern.compile(
            "\\{([^}]*)\\}"
    );

    public String sanitize(String query, String tenantId) {
        if (query == null || query.isBlank()) {
            throw new InvalidTenantQueryException("LogQL query must not be null or blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidTenantQueryException("Tenant context ID must not be null or blank");
        }

        String trimmed = query.trim();

        // 1. Fail-closed syntax verification for balanced delimiters
        validateSyntax(trimmed);

        if (!trimmed.contains("{")) {
            throw new InvalidTenantQueryException("LogQL query must contain at least one stream selector '{...}'");
        }

        String escapedTenantId = tenantId.replace("\"", "\\\"");
        String tenantMatcher = RESERVED_TENANT_LABEL + "=\"" + escapedTenantId + "\"";

        StringBuilder sb = new StringBuilder();
        int len = trimmed.length();
        int i = 0;

        while (i < len) {
            char c = trimmed.charAt(i);

            // 1. Skip string literals e.g. "...", '...', or `...`
            if (c == '"' || c == '\'' || c == '`') {
                char quote = c;
                sb.append(c);
                i++;
                while (i < len) {
                    char ch = trimmed.charAt(i);
                    sb.append(ch);
                    i++;
                    if (ch == quote && (quote == '`' || trimmed.charAt(i - 2) != '\\')) {
                        break;
                    }
                }
                continue;
            }

            // 2. Process stream selector braces {...} outside quotes/backticks
            if (c == '{') {
                sb.append(c);
                i++;
                StringBuilder labelBuf = new StringBuilder();
                int braceDepth = 1;
                while (i < len && braceDepth > 0) {
                    char ch = trimmed.charAt(i);
                    if (ch == '{') braceDepth++;
                    else if (ch == '}') braceDepth--;

                    if (braceDepth > 0) {
                        labelBuf.append(ch);
                    }
                    i++;
                }
                String inner = labelBuf.toString().trim();

                // Check if inner labels contain reserved tenant_id or __sentinel_source matcher
                if (RESERVED_LABEL_PATTERN.matcher(inner).find()) {
                    throw new InvalidTenantQueryException(
                            "Reserved label '" + RESERVED_TENANT_LABEL + "' or '" + RESERVED_SOURCE_LABEL + "' cannot be specified in stream selector"
                    );
                }

                if (inner.isEmpty()) {
                    sb.append(tenantMatcher);
                } else {
                    sb.append(inner).append(",").append(tenantMatcher);
                }
                sb.append('}');
                continue;
            }

            sb.append(c);
            i++;
        }

        String transformed = sb.toString();
        validateSyntax(transformed);
        return transformed;
    }

    private void validateSyntax(String query) {
        int braceDepth = 0;
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        boolean inBacktick = false;
        boolean escaped = false;

        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '`' && !inDoubleQuote && !inSingleQuote) {
                inBacktick = !inBacktick;
                continue;
            }

            if (c == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (c == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (inDoubleQuote || inSingleQuote || inBacktick) {
                continue;
            }

            switch (c) {
                case '{': braceDepth++; break;
                case '}': braceDepth--; break;
                case '(': parenDepth++; break;
                case ')': parenDepth--; break;
                case '[': bracketDepth++; break;
                case ']': bracketDepth--; break;
            }

            if (braceDepth < 0 || parenDepth < 0 || bracketDepth < 0) {
                throw new InvalidTenantQueryException("Unbalanced delimiters in LogQL query");
            }
        }

        if (inDoubleQuote || inSingleQuote || inBacktick) {
            throw new InvalidTenantQueryException("Unterminated string literal in LogQL query");
        }

        if (braceDepth != 0 || parenDepth != 0 || bracketDepth != 0) {
            throw new InvalidTenantQueryException("Unbalanced delimiters in LogQL query");
        }
    }
}
