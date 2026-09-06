package com.islandpacific.sentinel.query;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * PromQL Lexer / AST Transformer.
 * Ensures tenant_id and __sentinel_source are strictly reserved labels.
 * Parses PromQL vector selectors and forcibly injects authoritative tenant_id="<tenantId>".
 * Fails closed on syntax errors or invalid matcher contexts.
 */
@Component
public class PromQlAstSanitizer {

    private static final String RESERVED_TENANT_LABEL = "tenant_id";
    private static final String RESERVED_SOURCE_LABEL = "__sentinel_source";

    private static final Pattern RESERVED_LABEL_PATTERN = Pattern.compile(
            "(\"|'|\\b)(" + RESERVED_TENANT_LABEL + "|" + RESERVED_SOURCE_LABEL + ")(\"|'|\\b)\\s*(=|!=|=~|!~)"
    );

    private static final String KEYWORDS = "|sum|avg|min|max|stddev|stdvar|count|count_values|bottomk|topk|quantile|group|"
            + "rate|irate|increase|delta|idelta|deriv|predict_linear|histogram_quantile|histogram_count|histogram_sum|"
            + "histogram_fraction|vector|scalar|time|abs|absent|absent_over_time|ceil|floor|exp|log2|log10|ln|sqrt|"
            + "round|clamp|clamp_min|clamp_max|changes|resets|sort|sort_desc|label_replace|label_join|by|without|offset|bool|on|ignoring|group_left|group_right|and|or|unless|";

    public String sanitize(String query, String tenantId) {
        if (query == null || query.isBlank()) {
            throw new InvalidTenantQueryException("PromQL query must not be null or blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidTenantQueryException("Tenant context ID must not be null or blank");
        }

        String trimmed = query.trim();

        // 1. Fail-closed syntax verification for balanced quotes, braces, brackets, parentheses
        validateSyntax(trimmed);

        // 2. Reject client-supplied reserved labels in any matcher context
        if (RESERVED_LABEL_PATTERN.matcher(trimmed).find()) {
            throw new InvalidTenantQueryException(
                    "Reserved label '" + RESERVED_TENANT_LABEL + "' or '" + RESERVED_SOURCE_LABEL + "' cannot be specified by client"
            );
        }

        String escapedTenantId = tenantId.replace("\"", "\\\"");
        String tenantMatcher = RESERVED_TENANT_LABEL + "=\"" + escapedTenantId + "\"";

        StringBuilder sb = new StringBuilder();
        int len = trimmed.length();
        int i = 0;
        boolean pendingLabelModifier = false;
        int labelModifierParenDepth = -1;
        int currentParenDepth = 0;

        while (i < len) {
            char c = trimmed.charAt(i);

            if (c == '(') {
                currentParenDepth++;
                if (pendingLabelModifier) {
                    labelModifierParenDepth = currentParenDepth;
                    pendingLabelModifier = false;
                }
                sb.append(c);
                i++;
                continue;
            }

            if (c == ')') {
                if (currentParenDepth == labelModifierParenDepth) {
                    labelModifierParenDepth = -1;
                }
                currentParenDepth--;
                sb.append(c);
                i++;
                continue;
            }

            // 1. Skip string literals e.g. "..." or '...'
            if (c == '"' || c == '\'') {
                char quote = c;
                sb.append(c);
                i++;
                while (i < len) {
                    char ch = trimmed.charAt(i);
                    sb.append(ch);
                    i++;
                    if (ch == quote && trimmed.charAt(i - 2) != '\\') {
                        break;
                    }
                }
                continue;
            }

            // 2. Process selector braces {...}
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
                if (inner.isEmpty()) {
                    sb.append(tenantMatcher);
                } else {
                    sb.append(inner).append(",").append(tenantMatcher);
                }
                sb.append('}');
                continue;
            }

            // 3. Process identifiers outside quotes and braces
            if (isIdentifierStart(c)) {
                int start = i;
                while (i < len && isIdentifierPart(trimmed.charAt(i))) {
                    i++;
                }
                String ident = trimmed.substring(start, i);

                // Look ahead to check next non-whitespace char
                int lookAhead = i;
                while (lookAhead < len && Character.isWhitespace(trimmed.charAt(lookAhead))) {
                    lookAhead++;
                }

                char nextChar = lookAhead < len ? trimmed.charAt(lookAhead) : '\0';

                // Check if preceded by digit or dot (e.g. 10m, 5s, 1.5h, 1600000000)
                boolean isDurationOrNumberSuffix = start > 0 && (Character.isDigit(trimmed.charAt(start - 1)) || trimmed.charAt(start - 1) == '.');

                boolean isLabelModifierKeyword = "by".equalsIgnoreCase(ident) || "without".equalsIgnoreCase(ident)
                        || "on".equalsIgnoreCase(ident) || "ignoring".equalsIgnoreCase(ident);
                if (isLabelModifierKeyword) {
                    pendingLabelModifier = true;
                }

                boolean insideLabelModifierList = labelModifierParenDepth != -1;

                // If identifier is not a keyword and not a duration unit and not inside a label list and not followed by '{' or '(', append {tenant_id="..."}
                if (!isKeyword(ident) && !isDurationOrNumberSuffix && !insideLabelModifierList && nextChar != '{' && nextChar != '(' && nextChar != ':') {
                    sb.append(ident).append('{').append(tenantMatcher).append('}');
                } else {
                    sb.append(ident);
                }
                continue;
            }

            sb.append(c);
            i++;
        }

        String result = sb.toString();
        validateSyntax(result);
        return result;
    }

    private boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9') || c == ':';
    }

    private boolean isKeyword(String ident) {
        return KEYWORDS.contains("|" + ident + "|");
    }

    private void validateSyntax(String query) {
        int braceDepth = 0;
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
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

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (inDoubleQuote || inSingleQuote) {
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
                throw new InvalidTenantQueryException("Unbalanced delimiters in PromQL query");
            }
        }

        if (inDoubleQuote || inSingleQuote) {
            throw new InvalidTenantQueryException("Unterminated string literal in PromQL query");
        }

        if (braceDepth != 0 || parenDepth != 0 || bracketDepth != 0) {
            throw new InvalidTenantQueryException("Unbalanced delimiters in PromQL query");
        }
    }
}
