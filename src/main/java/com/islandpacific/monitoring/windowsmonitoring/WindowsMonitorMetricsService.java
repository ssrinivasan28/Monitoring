package com.islandpacific.monitoring.windowsmonitoring;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
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
            return java.net.InetAddress.getLocalHost().getHostName().equalsIgnoreCase(host) ||
                    java.net.InetAddress.getLocalHost().getHostAddress().equals(host);
        } catch (Exception e) {
            return false;
        }
    }

    private WindowsMonitorInfo getLocalMetrics(String host, int topN, List<String> services) {
        WindowsMonitorInfo info = new WindowsMonitorInfo(host);
        try {
            // CPU
            CentralProcessor processor = systemInfo.getHardware().getProcessor();
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            Thread.sleep(1000);
            info.setCpuUtilization(processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100);

            // Memory
            GlobalMemory memory = systemInfo.getHardware().getMemory();
            info.setMemoryTotalGB(memory.getTotal() / 1073741824.0);
            info.setMemoryUsedGB((memory.getTotal() - memory.getAvailable()) / 1073741824.0);
            info.setMemoryFreeGB(memory.getAvailable() / 1073741824.0);
            info.setMemoryUtilization((info.getMemoryUsedGB() / info.getMemoryTotalGB()) * 100);

            // Disks
            Map<String, WindowsMonitorInfo.DiskInfo> disks = new HashMap<>();
            for (HWDiskStore disk : systemInfo.getHardware().getDiskStores()) {
                WindowsMonitorInfo.DiskInfo dInfo = new WindowsMonitorInfo.DiskInfo();
                dInfo.totalGB = disk.getSize() / 1073741824.0;
                disks.put(disk.getName(), dInfo);
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

            // Top Processes
            OperatingSystem os = systemInfo.getOperatingSystem();
            Map<String, Double> topP = new LinkedHashMap<>();
            for (OSProcess p : os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, topN)) {
                topP.put(p.getName() + " (PID: " + p.getProcessID() + ")", p.getProcessCpuLoadCumulative() * 100);
            }
            info.setTopProcesses(topP);

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
            Process p = Runtime.getRuntime().exec("sc query " + serviceName);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("STATE")) {
                        if (line.contains("RUNNING"))
                            return "Running";
                        if (line.contains("STOPPED"))
                            return "Stopped";
                        if (line.contains("PAUSED"))
                            return "Paused";
                        return line.split(":")[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            return "Unknown";
        }
        return "NotFound";
    }

    private WindowsMonitorInfo getRemoteMetrics(String host, int topN, List<String> services,
            WindowsMonitorConfig.Credentials creds) {
        WindowsMonitorInfo info = new WindowsMonitorInfo(host);
        try {
            // CPU
            info.setCpuUtilization(Double.parseDouble(executePowerShell(host,
                    "(Get-WmiObject Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average",
                    creds)));

            // Memory
            String memInfo = executePowerShell(host,
                    "Get-WmiObject Win32_OperatingSystem | Select-Object TotalVisibleMemorySize, FreePhysicalMemory | ConvertTo-Json",
                    creds);
            info.setMemoryTotalGB(extractDouble(memInfo, "TotalVisibleMemorySize") / 1048576.0);
            info.setMemoryFreeGB(extractDouble(memInfo, "FreePhysicalMemory") / 1048576.0);
            info.setMemoryUsedGB(info.getMemoryTotalGB() - info.getMemoryFreeGB());
            info.setMemoryUtilization((info.getMemoryUsedGB() / info.getMemoryTotalGB()) * 100);

            // Uptime
            info.setSystemUptimeHours(Double.parseDouble(executePowerShell(host,
                    "(Get-Date) - (Get-WmiObject Win32_OperatingSystem).ConverttoDateTime((Get-WmiObject Win32_OperatingSystem).LastBootUpTime) | Select-Object -ExpandProperty TotalHours",
                    creds)));

            // Remote Disks
            String diskInfo = executePowerShell(host,
                    "Get-WmiObject Win32_LogicalDisk -Filter \"DriveType=3\" | Select-Object DeviceID, Size, FreeSpace | ConvertTo-Json",
                    creds);
            parseRemoteDisks(diskInfo, info);

            // Remote Top Processes
            String procInfo = executePowerShell(host,
                    "Get-Process | Sort-Object CPU -Descending | Select-Object -First " + topN
                            + " -Property Name, CPU | ConvertTo-Json",
                    creds);
            parseRemoteProcesses(procInfo, info);

            // Remote Services
            if (services != null && !services.isEmpty()) {
                String servicesJoined = String.join("','", services);
                String serviceInfo = executePowerShell(host,
                        "Get-Service -Name '" + servicesJoined + "' | Select-Object Name, Status | ConvertTo-Json",
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
            d.usagePercent = (d.usedGB / d.totalGB) * 100;
            disks.put(m.group(1), d);
        }
        info.setDisks(disks);
    }

    private void parseRemoteProcesses(String json, WindowsMonitorInfo info) {
        if (json == null || json.isEmpty())
            return;
        Map<String, Double> topP = new LinkedHashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"Name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"CPU\"\\s*:\\s*([\\d\\.]+)").matcher(json);
        while (m.find()) {
            topP.put(m.group(1), Double.parseDouble(m.group(2)));
        }
        info.setTopProcesses(topP);
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
            if (creds != null) {
                String securePass = "$pass = ConvertTo-SecureString '" + creds.password.replace("'", "''")
                        + "' -AsPlainText -Force; ";
                String credential = "$cred = New-Object System.Management.Automation.PSCredential ('"
                        + creds.username.replace("'", "''") + "', $pass); ";
                fullCommand = securePass + credential + "Invoke-Command -ComputerName " + host
                        + " -Credential $cred -ScriptBlock { " + command + " }";
            } else {
                fullCommand = "Invoke-Command -ComputerName " + host + " -ScriptBlock { " + command + " }";
            }

            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-Command", fullCommand);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logger.warning("PowerShell execution failed for host " + host + ": " + e.getMessage());
        }
        return output.toString().trim();
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
