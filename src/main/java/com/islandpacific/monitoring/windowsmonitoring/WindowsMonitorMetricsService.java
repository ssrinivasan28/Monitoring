package com.islandpacific.monitoring.windowsmonitoring;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WindowsMonitorMetricsService {
    private static final Logger logger = Logger.getLogger(WindowsMonitorMetricsService.class.getName());
    private final SystemInfo systemInfo = new SystemInfo();
    // Stores the previous CPU tick snapshot; updated each call so no Thread.sleep is needed.
    private long[] prevCpuTicks = systemInfo.getHardware().getProcessor().getSystemCpuLoadTicks();
    private static final String TOP_PROCESS_JSON_PATTERN =
            "\\{[^\\{\\}]*\"Name\"\\s*:\\s*\"([^\"]+)\"[^\\{\\}]*\"PID\"\\s*:\\s*(\\d+)[^\\{\\}]*\"CPUPercent\"\\s*:\\s*([\\d\\.]+)";

    public WindowsMonitorInfo getMetrics(String host, int topN, List<String> services,
            WindowsMonitorConfig.Credentials creds) {
        if ("localhost".equalsIgnoreCase(host) || isLocalHost(host)) {
            return getLocalMetrics(host, topN, services);
        } else {
            return getRemoteMetrics(host, topN, services, creds);
        }
    }

    private boolean isLocalHost(String host) {
        try {
            java.net.InetAddress target = java.net.InetAddress.getByName(host);
            if (target.isLoopbackAddress()) return true;
            if (target.equals(java.net.InetAddress.getLocalHost())) return true;
            // Check all network interfaces — servers often have multiple NICs
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                java.util.Enumeration<java.net.InetAddress> addrs =
                        ifaces.nextElement().getInetAddresses();
                while (addrs.hasMoreElements()) {
                    if (addrs.nextElement().equals(target)) return true;
                }
            }
            // Also match by hostname (case-insensitive)
            if (java.net.InetAddress.getLocalHost().getHostName().equalsIgnoreCase(host)) return true;
        } catch (Exception e) {
            logger.fine("isLocalHost check failed for " + host + ": " + e.getMessage());
        }
        return false;
    }

    private WindowsMonitorInfo getLocalMetrics(String host, int topN, List<String> services) {
        WindowsMonitorInfo info = new WindowsMonitorInfo(host);
        try {
            // CPU — use previous-cycle ticks (no Thread.sleep required)
            CentralProcessor processor = systemInfo.getHardware().getProcessor();
            info.setCpuUtilization(processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100);
            prevCpuTicks = processor.getSystemCpuLoadTicks();

            // Memory — compute utilization from raw bytes before any rounding occurs
            GlobalMemory memory = systemInfo.getHardware().getMemory();
            long totalBytes = memory.getTotal();
            long freeBytes  = memory.getAvailable();
            long usedBytes  = totalBytes - freeBytes;
            info.setMemoryTotalGB(totalBytes / 1073741824.0);
            info.setMemoryUsedGB(usedBytes / 1073741824.0);
            info.setMemoryFreeGB(freeBytes / 1073741824.0);
            info.setMemoryUtilization(totalBytes > 0 ? (usedBytes / (double) totalBytes) * 100 : 0.0);

            // Disks — use OSFileStore (logical volumes) to get actual free/used space
            Map<String, WindowsMonitorInfo.DiskInfo> disks = new HashMap<>();
            for (OSFileStore fs : systemInfo.getOperatingSystem().getFileSystem().getFileStores()) {
                WindowsMonitorInfo.DiskInfo dInfo = new WindowsMonitorInfo.DiskInfo();
                dInfo.totalGB = fs.getTotalSpace() / 1073741824.0;
                dInfo.freeGB = fs.getUsableSpace() / 1073741824.0;
                dInfo.usedGB = dInfo.totalGB - dInfo.freeGB;
                dInfo.usagePercent = dInfo.totalGB > 0 ? (dInfo.usedGB / dInfo.totalGB) * 100 : 0.0;
                disks.put(fs.getMount(), dInfo);
            }
            info.setDisks(disks);

            // Network
            Map<String, WindowsMonitorInfo.NetworkInfo> networks = new HashMap<>();
            for (NetworkIF net : systemInfo.getHardware().getNetworkIFs()) {
                WindowsMonitorInfo.NetworkInfo nInfo = new WindowsMonitorInfo.NetworkInfo();
                nInfo.bytesSent = net.getBytesSent();
                nInfo.bytesReceived = net.getBytesRecv();
                networks.put(net.getName(), nInfo);
            }
            info.setNetworkAdapters(networks);

            OperatingSystem os = systemInfo.getOperatingSystem();
            try {
                populateTopProcessesLocal(info, topN);
            } catch (Exception e) {
                logger.warning("Skipping local top-process collection for " + host + ": " + e.getMessage());
            }

            // System Uptime
            info.setSystemUptimeHours(os.getSystemUptime() / 3600.0);

            // Services
            if (services != null && !services.isEmpty()) {
                Map<String, String> serviceStatuses = new HashMap<>();
                for (String serviceName : services) {
                    serviceStatuses.put(serviceName, getLocalServiceStatus(serviceName));
                }
                info.setServiceStatuses(serviceStatuses);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting local metrics for " + host, e);
        }
        return info;
    }

    private String getLocalServiceStatus(String serviceName) {
        try {
            Process p = new ProcessBuilder("sc", "query", serviceName)
                    .redirectErrorStream(true)
                    .start();
            String result = "NotFound";
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("STATE")) {
                        if (line.contains("RUNNING"))  { result = "Running";  break; }
                        if (line.contains("STOPPED"))  { result = "Stopped";  break; }
                        if (line.contains("PAUSED"))   { result = "Paused";   break; }
                        String[] parts = line.split(":");
                        result = parts.length > 1 ? parts[1].trim() : "Unknown";
                        break;
                    }
                }
            }
            if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
            return result;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private WindowsMonitorInfo getRemoteMetrics(String host, int topN, List<String> services,
            WindowsMonitorConfig.Credentials creds) {
        WindowsMonitorInfo info = new WindowsMonitorInfo(host);
        try {
            // CPU
            info.setCpuUtilization(parseDoubleOrZero(executePowerShell(host,
                    "(Get-WmiObject Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average",
                    creds)));

            // Memory — compute utilization from raw KB values before any rounding occurs
            String memInfo = executePowerShell(host,
                    "Get-WmiObject Win32_OperatingSystem | Select-Object TotalVisibleMemorySize, FreePhysicalMemory | ConvertTo-Json",
                    creds);
            double totalKb = extractDouble(memInfo, "TotalVisibleMemorySize");
            double freeKb  = extractDouble(memInfo, "FreePhysicalMemory");
            double usedKb  = totalKb - freeKb;
            info.setMemoryTotalGB(totalKb / 1048576.0);
            info.setMemoryFreeGB(freeKb / 1048576.0);
            info.setMemoryUsedGB(usedKb / 1048576.0);
            info.setMemoryUtilization(totalKb > 0 ? (usedKb / totalKb) * 100 : 0.0);

            // Uptime
            info.setSystemUptimeHours(parseDoubleOrZero(executePowerShell(host,
                    "(Get-Date) - (Get-WmiObject Win32_OperatingSystem).ConverttoDateTime((Get-WmiObject Win32_OperatingSystem).LastBootUpTime) | Select-Object -ExpandProperty TotalHours",
                    creds)));

            // Remote Disks
            String diskInfo = executePowerShell(host,
                    "Get-WmiObject Win32_LogicalDisk -Filter \"DriveType=3\" | Select-Object DeviceID, Size, FreeSpace | ConvertTo-Json",
                    creds);
            parseRemoteDisks(diskInfo, info);

            try {
                populateTopProcessesRemote(host, topN, creds, info);
            } catch (Exception e) {
                logger.warning("Skipping remote top-process collection for " + host + ": " + e.getMessage());
            }

            // Remote Services — use array syntax so both single and multiple names work correctly
            if (services != null && !services.isEmpty()) {
                StringBuilder nameArray = new StringBuilder("@(");
                for (int i = 0; i < services.size(); i++) {
                    if (i > 0) nameArray.append(",");
                    nameArray.append("'").append(services.get(i).trim().replace("'", "''")).append("'");
                }
                nameArray.append(")");
                // -ErrorAction SilentlyContinue skips services that don't exist on this host
                // Wrap in @() to force array output even when only one service is returned
                String serviceInfo = executePowerShell(host,
                        "@(Get-Service -Name " + nameArray + " -ErrorAction SilentlyContinue | Select-Object Name, Status) | ConvertTo-Json",
                        creds);
                parseRemoteServices(serviceInfo, info);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting remote metrics for " + host, e);
        }
        return info;
    }

    private void parseRemoteDisks(String json, WindowsMonitorInfo info) {
        if (json == null || json.isEmpty())
            return;
        Map<String, WindowsMonitorInfo.DiskInfo> disks = new HashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"DeviceID\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"Size\"\\s*:\\s*(\\d+),\\s*\"FreeSpace\"\\s*:\\s*(\\d+)")
                .matcher(json);
        while (m.find()) {
            WindowsMonitorInfo.DiskInfo d = new WindowsMonitorInfo.DiskInfo();
            d.totalGB = Double.parseDouble(m.group(2)) / 1073741824.0;
            d.freeGB = Double.parseDouble(m.group(3)) / 1073741824.0;
            d.usedGB = d.totalGB - d.freeGB;
            d.usagePercent = d.totalGB > 0 ? (d.usedGB / d.totalGB) * 100 : 0.0;
            disks.put(m.group(1), d);
        }
        info.setDisks(disks);
    }

    private void parseTopProcesses(String json, WindowsMonitorInfo info) {
        if (json == null || json.isEmpty())
            return;
        Map<String, Double> topP = new LinkedHashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(TOP_PROCESS_JSON_PATTERN).matcher(json);
        while (m.find()) {
            topP.put(m.group(1) + " (PID: " + m.group(2) + ")", Double.parseDouble(m.group(3)));
        }
        if (!topP.isEmpty()) {
            info.setTopProcesses(topP);
        }
    }

    private void populateTopProcessesLocal(WindowsMonitorInfo info, int topN) {
        String procInfo = executeLocalPowerShell(buildTopProcessCommand(topN), 8);
        parseTopProcesses(procInfo, info);
    }

    private void populateTopProcessesRemote(String host, int topN, WindowsMonitorConfig.Credentials creds,
            WindowsMonitorInfo info) {
        String procInfo = executePowerShell(host, buildTopProcessCommand(topN), creds, 8);
        parseTopProcesses(procInfo, info);
    }

    private String buildTopProcessCommand(int topN) {
        return "$cpuCount = [double](Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors; " +
                "Get-CimInstance Win32_PerfFormattedData_PerfProc_Process | " +
                "Where-Object { $_.IDProcess -ne 0 -and $_.Name -ne '_Total' -and $_.Name -ne 'Idle' } | " +
                "Sort-Object PercentProcessorTime -Descending | " +
                "Select-Object -First " + topN + " " +
                "@{Name='Name';Expression={$_.Name}}, " +
                "@{Name='PID';Expression={$_.IDProcess}}, " +
                "@{Name='CPUPercent';Expression={[math]::Round(($_.PercentProcessorTime / [math]::Max($cpuCount, 1)), 2)}} | " +
                "ConvertTo-Json";
    }

    private String executeLocalPowerShell(String command, int timeoutSeconds) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command);
            Process process = builder.start();

            Thread stderrDrainer = new Thread(() -> {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                    String line;
                    while ((line = err.readLine()) != null) {
                        logger.warning("[PS stderr][localhost] " + line);
                    }
                } catch (Exception ignored) {}
            });
            stderrDrainer.setDaemon(true);
            stderrDrainer.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            if (!process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                logger.warning("Local PowerShell process forcibly terminated after " + timeoutSeconds + "s");
            }
        } catch (Exception e) {
            logger.warning("Local PowerShell execution failed: " + e.getMessage());
        }
        return output.toString().trim();
    }

    private String executePowerShell(String host, String command, WindowsMonitorConfig.Credentials creds, int timeoutSeconds) {
        StringBuilder output = new StringBuilder();
        try {
            String fullCommand;
            ProcessBuilder builder;
            if (creds != null) {
                String psScript =
                        "$pass = ConvertTo-SecureString $env:PS_CRED_PASS -AsPlainText -Force; " +
                        "$cred = New-Object System.Management.Automation.PSCredential('" +
                        creds.username.replace("'", "''") + "', $pass); " +
                        "Invoke-Command -ComputerName " + host + " -Credential $cred -ScriptBlock { " + command + " }";
                builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", psScript);
                builder.environment().put("PS_CRED_PASS", creds.password);
            } else {
                fullCommand = "Invoke-Command -ComputerName " + host + " -ScriptBlock { " + command + " }";
                builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", fullCommand);
            }

            Process process = builder.start();
            Thread stderrDrainer = new Thread(() -> {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                    String line;
                    while ((line = err.readLine()) != null) {
                        logger.warning("[PS stderr][" + host + "] " + line);
                    }
                } catch (Exception ignored) {}
            });
            stderrDrainer.setDaemon(true);
            stderrDrainer.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            if (!process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                logger.warning("PowerShell process forcibly terminated after " + timeoutSeconds + "s for host " + host);
            }
        } catch (Exception e) {
            logger.warning("PowerShell execution failed for host " + host + ": " + e.getMessage());
        }
        return output.toString().trim();
    }

    private void parseRemoteServices(String json, WindowsMonitorInfo info) {
        if (json == null || json.isEmpty())
            return;
        Map<String, String> status = new HashMap<>();
        java.util.regex.Pattern p = java.util.regex.Pattern
                .compile("\"Name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"Status\"\\s*:\\s*([\\d\"\\w]+)");
        java.util.regex.Matcher m2 = p.matcher(json);
        while (m2.find()) {
            String name = m2.group(1);
            String rawStatus = m2.group(2).replace("\"", "");
            if (rawStatus.equals("4"))
                status.put(name, "Running");
            else if (rawStatus.equals("1"))
                status.put(name, "Stopped");
            else
                status.put(name, rawStatus);
        }
        info.setServiceStatuses(status);
    }

    private String executePowerShell(String host, String command, WindowsMonitorConfig.Credentials creds) {
        StringBuilder output = new StringBuilder();
        try {
            String fullCommand;
            ProcessBuilder builder;
            if (creds != null) {
                // Password is injected via environment variable to keep it out of the process argument list.
                String psScript =
                        "$pass = ConvertTo-SecureString $env:PS_CRED_PASS -AsPlainText -Force; " +
                        "$cred = New-Object System.Management.Automation.PSCredential('" +
                        creds.username.replace("'", "''") + "', $pass); " +
                        "Invoke-Command -ComputerName " + host + " -Credential $cred -ScriptBlock { " + command + " }";
                builder = new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command", psScript);
                builder.environment().put("PS_CRED_PASS", creds.password);
            } else {
                fullCommand = "Invoke-Command -ComputerName " + host + " -ScriptBlock { " + command + " }";
                builder = new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command", fullCommand);
            }

            Process process = builder.start();
            // Drain stderr on a separate thread to prevent the process from blocking on a full stderr pipe.
            Thread stderrDrainer = new Thread(() -> {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                    String line;
                    while ((line = err.readLine()) != null) {
                        logger.warning("[PS stderr][" + host + "] " + line);
                    }
                } catch (Exception ignored) {}
            });
            stderrDrainer.setDaemon(true);
            stderrDrainer.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            // Cap wait at 25s — caller's future.get() already enforces 30s; this prevents zombie processes.
            if (!process.waitFor(25, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                logger.warning("PowerShell process forcibly terminated after 25s for host " + host);
            }
        } catch (Exception e) {
            logger.warning("PowerShell execution failed for host " + host + ": " + e.getMessage());
        }
        return output.toString().trim();
    }

    private double parseDoubleOrZero(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warning("Could not parse numeric value from PowerShell output: '" + value.trim() + "'");
            return 0.0;
        }
    }

    private double extractDouble(String input, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.]+)");
        java.util.regex.Matcher m = p.matcher(input);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0;
    }
}
