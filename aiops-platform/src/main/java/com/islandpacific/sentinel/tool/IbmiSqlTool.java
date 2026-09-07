package com.islandpacific.sentinel.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.security.SecretProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Executes SELECT-only queries against IBM i DB2 / QSYS2 views via JDBC.
 * Rejects INSERT, UPDATE, DELETE, DDL, and data modification queries.
 */
@Component
public class IbmiSqlTool implements SentinelTool {

    private static final Logger log = LoggerFactory.getLogger(IbmiSqlTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/|--[^\r\n]*");
    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
            "MERGE", "CALL", "GRANT", "REVOKE", "EXEC", "EXECUTE", "RENAME", "INTO"
    );

    @Value("${ibmi.server:localhost}")
    private String defaultHost;

    @Value("${ibmi.user:qsysopr}")
    private String defaultUser;

    @Value("${ibmi.password:}")
    private String defaultPassword;

    private final SecretProtector secretProtector;

    @Autowired
    public IbmiSqlTool(SecretProtector secretProtector) {
        this.secretProtector = secretProtector;
        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        } catch (ClassNotFoundException e) {
            log.warn("IBM AS400 JDBC driver com.ibm.as400.access.AS400JDBCDriver not on classpath. Mock JDBC fallback will be used if needed.");
        }
    }

    @Override
    public String getName() {
        return "ibmi_sql";
    }

    @Override
    public String getDescription() {
        return "Executes a SELECT-only SQL query against IBM i DB2 and QSYS2 system views.";
    }

    @Override
    public LlmTool getLlmToolDefinition() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");

        Map<String, Object> props = new LinkedHashMap<>();

        Map<String, Object> queryProp = new LinkedHashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "SELECT-only SQL query to run against DB2/QSYS2, e.g. 'SELECT * FROM QSYS2.SYSTEM_STATUS_INFO'");
        props.put("query", queryProp);

        Map<String, Object> paramsProp = new LinkedHashMap<>();
        paramsProp.put("type", "array");
        paramsProp.put("description", "Optional parameter values for SQL statement placeholders (?)");
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("type", "string");
        paramsProp.put("items", items);
        props.put("params", paramsProp);

        params.put("properties", props);
        params.put("required", List.of("query"));

        return new LlmTool(getName(), getDescription(), params);
    }

    @Override
    public ToolExecutionResult execute(UUID tenantId, Map<String, Object> arguments) {
        if (arguments == null || !arguments.containsKey("query")) {
            return ToolExecutionResult.failure("Missing required 'query' argument");
        }

        String sql = (String) arguments.get("query");
        try {
            validateSelectOnly(sql);
        } catch (IllegalArgumentException e) {
            return ToolExecutionResult.failure(e.getMessage());
        }

        @SuppressWarnings("unchecked")
        List<Object> sqlParams = (List<Object>) arguments.getOrDefault("params", Collections.emptyList());

        String host = (String) arguments.getOrDefault("host", defaultHost);
        String user = (String) arguments.getOrDefault("user", defaultUser);
        String rawPass = (String) arguments.getOrDefault("password", defaultPassword);
        String pass = secretProtector != null ? secretProtector.resolve(rawPass) : rawPass;

        String url = "jdbc:as400://" + host;

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (sqlParams != null) {
                for (int i = 0; i < sqlParams.size(); i++) {
                    stmt.setObject(i + 1, sqlParams.get(i));
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = metaData.getColumnLabel(i);
                        Object val = rs.getObject(i);
                        row.put(colName, val);
                    }
                    rows.add(row);
                }
            }

            String jsonRows = objectMapper.writeValueAsString(rows);
            String summary = String.format("Query executed successfully. Returned %d row(s).", rows.size());
            return ToolExecutionResult.success(rows, summary + "\n" + jsonRows);

        } catch (SQLException e) {
            log.error("IBM i SQL execution error for tenant {}: {}", tenantId, e.getMessage());
            return ToolExecutionResult.failure("IBM i SQL execution error: " + e.getMessage());
        } catch (Exception e) {
            return ToolExecutionResult.failure("Execution failed: " + e.getMessage());
        }
    }

    /**
     * Validates that the provided SQL query is strictly a single SELECT or WITH statement.
     */
    public static void validateSelectOnly(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Query rejected: SQL statement is empty.");
        }

        // Strip comments
        String stripped = COMMENT_PATTERN.matcher(sql).replaceAll(" ").trim();
        if (stripped.endsWith(";")) {
            stripped = stripped.substring(0, stripped.length() - 1).trim();
        }

        // Reject multiple statements
        if (stripped.contains(";")) {
            throw new IllegalArgumentException("Query rejected: Multiple SQL statements are not permitted.");
        }

        String upper = stripped.toUpperCase(Locale.ROOT);

        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw new IllegalArgumentException("Query rejected: Only read-only SELECT or WITH statements are permitted.");
        }

        // Tokenize and check forbidden keywords
        String[] tokens = upper.split("\\s+");
        for (String token : tokens) {
            String cleanToken = token.replaceAll("[^A-Z]", "");
            if (FORBIDDEN_KEYWORDS.contains(cleanToken)) {
                throw new IllegalArgumentException("Query rejected: Statements containing '" + cleanToken + "' are not permitted.");
            }
        }
    }
}
