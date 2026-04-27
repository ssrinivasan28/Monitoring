package com.islandpacific.monitoring.winservicemonitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class WinServiceMonitorService {

    private static final Logger logger = AppLogger.getLogger();

    public WinServiceInfo checkServices(String server, List<String> services,
            WinServiceMonitorConfig.Credentials creds) {
        WinServiceInfo info = new WinServiceInfo(server);
        if (services == null || services.isEmpty()) {
            logger.warning("No services configured to monitor for server: " + server);
            return info;
        }

        if (isLocalHost(server)) {
            Map<String, String> statuses = checkLocalServices(services);
            info.setServiceStatuses(statuses);
            // Populate displayNames for local services same as remote (identity mapping)
            Map<String, String> displayNames = new HashMap<>();
            for (String svc : statuses.keySet()) displayNames.put(svc, svc);
            info.setDisplayNames(displayNames);
        } else {
            Map<String, String> displayNames = new HashMap<>();
            info.setServiceStatuses(checkRemoteServices(server, services, creds, displayNames));
            info.setDisplayNames(displayNames);
        }
        return info;
    }

    boolean isLocalHost(String server) {
        if ("localhost".equalsIgnoreCase(server) || "127.0.0.1".equals(server)) return true;
        try {
            java.net.InetAddress target = java.net.InetAddress.getByName(server);
            if (target.isLoopbackAddress()) return true;
            if (target.equals(java.net.InetAddress.getLocalHost())) return true;
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                java.util.Enumeration<java.net.InetAddress> addrs =
                        ifaces.nextElement().getInetAddresses();
                while (addrs.hasMoreElements()) {
                    if (addrs.nextElement().equals(target)) return true;
                }
            }
            if (java.net.InetAddress.getLocalHost().getHostName().equalsIgnoreCase(server)) return true;
        } catch (Exception e) {
            logger.fine("isLocalHost check failed for " + server + ": " + e.getMessage());
        }
        return false;
    }

    private Map<String, String> checkLocalServices(List<String> services) {
        Map<String, String> statuses = new HashMap<>();
        for (String service : services) {
            statuses.put(service, queryLocalServiceStatus(service));
        }
        return statuses;
    }

    String queryLocalServiceStatus(String serviceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sc", "query", serviceName);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String result = "NotFound";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("STATE")) {
                        if (line.contains("RUNNING")) { result = "Running"; break; }
                        if (line.contains("STOPPED")) { result = "Stopped"; break; }
                        if (line.contains("PAUSED"))  { result = "Paused";  break; }
                        if (line.contains("START_PENDING")) { result = "StartPending"; break; }
                        if (line.contains("STOP_PENDING"))  { result = "StopPending";  break; }
                        result = line.contains(":") ? line.split(":")[1].trim() : "Unknown";
                        break;
                    }
                }
            }
            boolean finished = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                logger.warning("sc query timed out for service: " + serviceName);
                return "Unknown";
            }
            return result;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to query local service: " + serviceName, e);
            return "Unknown";
        }
    }

    public static final String SERVER_UNREACHABLE = "ServerUnreachable";

    private Map<String, String> checkRemoteServices(String server, List<String> services,
            WinServiceMonitorConfig.Credentials creds, Map<String, String> displayNamesOut) {
        Map<String, String> statuses = new HashMap<>();
        try {
            if (!isReachable(server)) {
                logger.warning("Server " + server + " is unreachable (ping failed)");
                for (String svc : services) statuses.put(svc, SERVER_UNREACHABLE);
                return statuses;
            }
            String serviceNames = "@('" + String.join("','", services) + "')";
            String psCommand = "Get-Service -DisplayName " + serviceNames
                    + " | Select-Object Name,DisplayName,Status | ConvertTo-Json -Depth 1";

            String output = executePowerShell(server, psCommand, creds);
            statuses = parseServicesJson(output, services, displayNamesOut);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking remote services on " + server, e);
            for (String svc : services) statuses.put(svc, SERVER_UNREACHABLE);
        }
        return statuses;
    }

    private boolean isReachable(String server) {
        try {
            return java.net.InetAddress.getByName(server).isReachable(3000);
        } catch (Exception e) {
            return false;
        }
    }

    Map<String, String> parseServicesJson(String json, List<String> services,
            Map<String, String> displayNamesOut) {
        Map<String, String> statuses = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            for (String svc : services) statuses.put(svc, "Unknown");
            return statuses;
        }

        // Case-insensitive lookup by DisplayName: lowercase(configuredName) -> configuredName
        Map<String, String> lowerToConfigured = new HashMap<>();
        for (String svc : services) {
            lowerToConfigured.put(svc.toLowerCase(), svc);
        }

        // Invoke-Command wraps output with PSComputerName/RunspaceId/PSShowComputerName fields —
        // strip them so nested-brace detection doesn't misidentify PS metadata blocks as service objects.
        String cleaned = json
                .replaceAll("\"PSComputerName\"\\s*:\\s*\"[^\"]*\"\\s*,?", "")
                .replaceAll("\"RunspaceId\"\\s*:\\s*\"[^\"]*\"\\s*,?", "")
                .replaceAll("\"PSShowComputerName\"\\s*:\\s*\\w+\\s*,?", "");

        // Extract each JSON object block and parse fields individually (order-independent).
        // The pattern allows nested braces one level deep to tolerate any remaining metadata.
        java.util.regex.Pattern blockPattern = java.util.regex.Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}");
        java.util.regex.Pattern dispPattern  = java.util.regex.Pattern.compile("\"DisplayName\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Pattern statPattern  = java.util.regex.Pattern.compile("\"Status\"\\s*:\\s*([\\d\"\\w]+)");
        java.util.regex.Matcher blockMatcher = blockPattern.matcher(cleaned);
        while (blockMatcher.find()) {
            String block = blockMatcher.group();
            java.util.regex.Matcher dm = dispPattern.matcher(block);
            java.util.regex.Matcher sm = statPattern.matcher(block);
            if (!dm.find() || !sm.find()) continue;

            String displayName = dm.group(1);
            String rawStatus   = sm.group(1).replace("\"", "").trim();

            String status;
            switch (rawStatus) {
                case "4": case "Running":      status = "Running";      break;
                case "1": case "Stopped":      status = "Stopped";      break;
                case "7": case "Paused":       status = "Paused";       break;
                case "2": case "StartPending": status = "StartPending"; break;
                case "3": case "StopPending":  status = "StopPending";  break;
                default:                       status = rawStatus;       break;
            }

            // Key by the configured DisplayName (matched case-insensitively)
            String configuredName = lowerToConfigured.getOrDefault(displayName.toLowerCase(), displayName);
            statuses.put(configuredName, status);
            if (displayNamesOut != null) {
                displayNamesOut.put(configuredName, configuredName);
            }
        }

        for (String svc : services) {
            statuses.putIfAbsent(svc, "NotFound");
        }
        return statuses;
    }

    private String executePowerShell(String server, String command, WinServiceMonitorConfig.Credentials creds) {
        StringBuilder output = new StringBuilder();
        try {
            String fullCommand;
            if (creds != null) {
                String securePass = "$pass = ConvertTo-SecureString '" + creds.password.replace("'", "''")
                        + "' -AsPlainText -Force; ";
                String credential = "$cred = New-Object System.Management.Automation.PSCredential ('"
                        + creds.username.replace("'", "''") + "', $pass); ";
                // command has no -ComputerName; Invoke-Command handles the remote hop
                fullCommand = securePass + credential
                        + "Invoke-Command -ComputerName " + server
                        + " -Credential $cred -ScriptBlock { " + command + " }";
            } else {
                // Append -ComputerName directly to Get-Service for credential-less remote access
                fullCommand = command.replace("| Select-Object",
                        "-ComputerName " + server + " | Select-Object");
            }

            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command", fullCommand);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warning("PowerShell timed out for server: " + server);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "PowerShell execution failed for " + server + ": " + e.getMessage(), e);
        }
        return output.toString().trim();
    }
}
