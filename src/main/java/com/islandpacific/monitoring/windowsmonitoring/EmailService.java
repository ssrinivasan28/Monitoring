package com.islandpacific.monitoring.windowsmonitoring;

import com.islandpacific.monitoring.common.AppLogger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {
    private static final Logger logger = AppLogger.getLogger();

    private final String host;
    private final String port;
    private final String from;
    private final String to;
    private final String bcc;
    private final String username;
    private final String password;
    private final boolean auth;
    private final boolean starttls;
    private final String importance;

    public EmailService(WindowsMonitorConfig config) {
        this.host = config.getEmailHost();
        this.port = config.getEmailPort();
        this.from = config.getEmailFrom();
        this.to = config.getEmailTo();
        this.bcc = config.getEmailBcc();
        this.username = config.getEmailUsername();
        this.password = config.getEmailPassword();
        this.auth = config.isEmailAuthEnabled();
        this.starttls = config.isEmailStartTlsEnabled();
        this.importance = config.getEmailImportance();
    }

    public void sendSystemAlert(WindowsMonitorInfo info, double cpuLimit, double memLimit, double diskLimit) {
        StringBuilder body = new StringBuilder();
        body.append("<h1 style='color: #d9534f;'>Windows System Alert: ").append(info.getHostName()).append("</h1>");
        body.append("<p>Threshold breaches detected:</p>");
        body.append("<table border='1' cellpadding='5' style='border-collapse: collapse;'>");
        body.append("<tr style='background-color: #f2f2f2;'><th>Metric</th><th>Value</th><th>Threshold</th></tr>");

        if (info.getCpuUtilization() > cpuLimit) {
            body.append("<tr><td>CPU Usage</td><td>").append(String.format("%.2f%%", info.getCpuUtilization()))
                    .append("</td><td>").append(cpuLimit).append("%</td></tr>");
        }
        if (info.getMemoryUtilization() > memLimit) {
            body.append("<tr><td>Memory Usage</td><td>").append(String.format("%.2f%%", info.getMemoryUtilization()))
                    .append("</td><td>").append(memLimit).append("%</td></tr>");
        }

        // Report disks over threshold
        if (info.getDisks() != null) {
            for (Map.Entry<String, WindowsMonitorInfo.DiskInfo> entry : info.getDisks().entrySet()) {
                if (entry.getValue().usagePercent > diskLimit) {
                    body.append("<tr><td>Disk ").append(entry.getKey()).append("</td><td>")
                            .append(String.format("%.2f%%", entry.getValue().usagePercent))
                            .append("</td><td>").append(diskLimit).append("%</td></tr>");
                }
            }
        }
        body.append("</table>");

        // Service issues
        if (info.getServiceStatuses() != null) {
            boolean hasIssues = info.getServiceStatuses().values().stream()
                    .anyMatch(s -> !"Running".equalsIgnoreCase(s));
            if (hasIssues) {
                body.append("<h3>Service Status Issues</h3><ul>");
                info.getServiceStatuses().forEach((name, status) -> {
                    if (!"Running".equalsIgnoreCase(status)) {
                        body.append("<li style='color: red;'><b>").append(name).append(":</b> ").append(status)
                                .append("</li>");
                    }
                });
                body.append("</ul>");
            }
        }

        body.append("<h3>Top Processes</h3><ul>");
        info.getTopProcesses().forEach((name, cpu) -> {
            body.append("<li>").append(name).append(": ").append(String.format("%.2f%%", cpu)).append("</li>");
        });
        body.append("</ul>");

        body.append("<p><b>System Uptime:</b> ").append(String.format("%.2f", info.getSystemUptimeHours()))
                .append(" hours</p>");

        sendEmail("CRITICAL: Windows System Alert - " + info.getHostName(), body.toString());
    }

    public void sendErrorAlert(String subject, String message) {
        sendEmail("ERROR: Windows Monitor - " + subject, "<h3>Monitor Error</h3><p>" + message + "</p>");
    }

    private void sendEmail(String subject, String htmlContent) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            if (bcc != null && !bcc.isEmpty()) {
                message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
            }
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            message.setSentDate(new Date());
            message.setHeader("X-Priority", importance.equalsIgnoreCase("High") ? "1" : "3");

            Transport.send(message);
            logger.info("Email alert sent successfully: " + subject);
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email alert: " + e.getMessage(), e);
        }
    }
}
