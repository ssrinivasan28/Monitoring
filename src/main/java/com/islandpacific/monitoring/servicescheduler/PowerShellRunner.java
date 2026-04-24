package com.islandpacific.monitoring.servicescheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class PowerShellRunner {

    private static final Logger logger = AppLogger.getLogger();

    public static class Result {
        public final int exitCode;
        public final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public boolean success() {
            return exitCode == 0;
        }
    }

    public static Result stopService(String server, String username, String password, String serviceName)
            throws IOException, InterruptedException {
        logger.info("Stopping service '" + serviceName + "' on " + server);
        // Use sc.exe stop + polling loop — more reliable than Stop-Service over WinRM
        // which can return success before the service control manager completes the stop.
        // Poll sc.exe query instead of Get-Service — sc.exe reads directly from SCM
        // and will show STOP_PENDING rather than prematurely returning Stopped over WinRM.
        String script =
            "sc.exe stop " + escapeSingle(serviceName) + "\n" +
            "$deadline = (Get-Date).AddSeconds(300)\n" +
            "do {\n" +
            "  Start-Sleep -Seconds 5\n" +
            "  $scOut = sc.exe query " + escapeSingle(serviceName) + "\n" +
            "  $stopped = ($scOut | Select-String 'STATE.*STOPPED') -ne $null\n" +
            "} while (-not $stopped -and (Get-Date) -lt $deadline)\n" +
            "if (-not $stopped) { throw 'Service did not stop within 300 seconds' }\n" +
            "Write-Output 'Stopped'";
        return runRemote(server, username, password, script);
    }

    public static Result startService(String server, String username, String password, String serviceName)
            throws IOException, InterruptedException {
        logger.info("Starting service '" + serviceName + "' on " + server);
        // Use sc.exe start + polling loop for consistency with stop
        String script =
            "sc.exe start " + escapeSingle(serviceName) + "\n" +
            "$deadline = (Get-Date).AddSeconds(120)\n" +
            "do {\n" +
            "  Start-Sleep -Seconds 5\n" +
            "  $scOut = sc.exe query " + escapeSingle(serviceName) + "\n" +
            "  $running = ($scOut | Select-String 'STATE.*RUNNING') -ne $null\n" +
            "} while (-not $running -and (Get-Date) -lt $deadline)\n" +
            "if (-not $running) { throw 'Service did not start within 120 seconds' }\n" +
            "Write-Output 'Running'";
        return runRemote(server, username, password, script);
    }

    public static Result runRemote(String server, String username, String password, String scriptBlock)
            throws IOException, InterruptedException {
        if (isLocalServer(server)) {
            // Server is this machine — run directly without WinRM to avoid loopback credential issues
            logger.info("Server " + server + " is local — running script directly (no WinRM)");
            return runScript(scriptBlock + "\n");
        }
        String script =
            "$ErrorActionPreference = 'Stop'\n" +
            "$pass = ConvertTo-SecureString '" + escapeSingle(password) + "' -AsPlainText -Force\n" +
            "$cred = New-Object System.Management.Automation.PSCredential ('" + escapeSingle(username) + "', $pass)\n" +
            "$sb = [ScriptBlock]::Create(@'\n" +
            scriptBlock + "\n" +
            "'@)\n" +
            "Invoke-Command -ComputerName " + server + " -Credential $cred -ScriptBlock $sb\n";
        return runScript(script);
    }

    private static boolean isLocalServer(String server) {
        try {
            InetAddress target = InetAddress.getByName(server);
            if (target.isLoopbackAddress()) return true;
            InetAddress local = InetAddress.getLocalHost();
            if (target.equals(local)) return true;
            // Also check all network interfaces
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                java.util.Enumeration<InetAddress> addrs = ifaces.nextElement().getInetAddresses();
                while (addrs.hasMoreElements()) {
                    if (addrs.nextElement().equals(target)) return true;
                }
            }
        } catch (Exception e) {
            logger.fine("isLocalServer check failed for " + server + ": " + e.getMessage());
        }
        return false;
    }

    public static Result run(String psCommand) throws IOException, InterruptedException {
        return runScript(psCommand);
    }

    private static Result runScript(String scriptContent) throws IOException, InterruptedException {
        Path tmpScript = null;
        try {
            tmpScript = Files.createTempFile("ipss_", ".ps1");
            Files.write(tmpScript, scriptContent.getBytes(StandardCharsets.UTF_8));

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NonInteractive", "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File", tmpScript.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(420, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warning("PowerShell timed out");
            }
            int exitCode = process.exitValue();
            String out = output.toString().trim();
            logger.info("PowerShell exit=" + exitCode + " output=" + out);
            return new Result(exitCode, out);
        } finally {
            if (tmpScript != null) {
                try { Files.deleteIfExists(tmpScript); } catch (IOException ignored) {}
            }
        }
    }

    // Escape for use inside PowerShell single-quoted strings: double any single quotes.
    // No backtick escaping needed — inside a .ps1 file, single-quoted strings are truly literal.
    private static String escapeSingle(String value) {
        return value.replace("'", "''");
    }
}
