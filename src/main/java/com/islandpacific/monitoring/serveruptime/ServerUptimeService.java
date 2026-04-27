package com.islandpacific.monitoring.serveruptime;

import io.prometheus.client.Gauge;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ServerUptimeService {

    private final Logger logger;
    private final List<String> servers;
    private final EmailService emailService;
    private final Gauge serverStatusGauge;
    private final Map<String, Boolean> currentStatus;

    public ServerUptimeService(Logger logger, List<String> servers, EmailService emailService,
                               Gauge serverStatusGauge, Map<String, Boolean> currentStatus) {
        this.logger = logger;
        this.servers = servers;
        this.emailService = emailService;
        this.serverStatusGauge = serverStatusGauge;
        this.currentStatus = currentStatus;
    }

    /** Called once at startup to establish initial state and alert on any down servers. */
    public void initializeStatus() {
        for (String server : servers) {
            logger.info("Pinging " + server + " ...");
            boolean isUp = pingServer(server);
            serverStatusGauge.labels(server).set(isUp ? 1 : 0);
            currentStatus.put(server, isUp);
            if (!isUp) {
                logger.warning("Server " + server + " is DOWN on startup. Sending initial alert.");
                emailService.sendServerStatusAlert(server, false);
            } else {
                logger.info("Server " + server + " is UP on startup.");
            }
        }
    }

    /** Called on each scheduled cycle — detects status changes and sends alerts. */
    public void checkAndAlert() {
        for (String server : servers) {
            boolean isUp = pingServer(server);
            boolean previous = currentStatus.getOrDefault(server, false);

            serverStatusGauge.labels(server).set(isUp ? 1 : 0);

            if (isUp != previous) {
                logger.info("Status change for " + server + ": " + (previous ? "DOWN -> UP" : "UP -> DOWN"));
                currentStatus.put(server, isUp);
                emailService.sendServerStatusAlert(server, isUp);
            } else {
                logger.fine("Server " + server + " remains " + (isUp ? "UP" : "DOWN") + ".");
            }
        }
    }

    /** Visible for testing — subclasses or tests can override ping behaviour. */
    protected boolean pingServer(String serverAddress) {
        // Try TCP port 445 (SMB) first — works without admin rights on Windows.
        // Fall back to ICMP isReachable only if TCP connect fails, to handle non-Windows targets.
        int[] tcpPorts = {445, 135, 80};
        for (int port : tcpPorts) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(InetAddress.getByName(serverAddress), port), 3000);
                return true;
            } catch (IOException ignored) {
                // port not open on this server — try next
            }
        }
        // ICMP fallback (requires admin on Windows, best-effort)
        try {
            return InetAddress.getByName(serverAddress).isReachable(3000);
        } catch (IOException e) {
            logger.warning("Error reaching " + serverAddress + ": " + e.getMessage());
            return false;
        }
    }
}
