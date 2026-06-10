package com.islandpacific.monitoring.sslcertmonitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class EmailService {

    private static final Logger logger = AppLogger.getLogger();

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    private final SSLCertMonitorConfig config;
    private final OAuth2TokenProvider oauth2TokenProvider;

    public EmailService(SSLCertMonitorConfig config) {
        this.config = config;
        if ("OAUTH2".equals(config.getAuthMethod())
                && !config.getOauth2TenantId().isEmpty()
                && !config.getOauth2ClientId().isEmpty()) {
            this.oauth2TokenProvider = new OAuth2TokenProvider(
                    config.getOauth2TenantId(),
                    config.getOauth2ClientId(),
                    config.getOauth2ClientSecret(),
                    null,
                    config.getOauth2TokenUrl());
        } else {
            this.oauth2TokenProvider = null;
        }
    }

    public void sendExpiryWarning(SSLCertInfo info, int threshold) {
        String clientPrefix = config.getClientName().isEmpty() ? "" : "[" + config.getClientName() + "] ";
        String subject = clientPrefix + "SSL CERT WARNING [" + threshold + " day alert]: " + info.getHostPort()
                + " expires in " + info.getDaysUntilExpiry() + " days";
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        String expiryStr = info.getExpiryDate() != null
                ? new java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss z").format(info.getExpiryDate())
                : "Unknown";
        String body = buildHtmlEmail(
                "#e67e22", "WARNING",
                "#fff8e1", "#e67e22",
                "SSL Certificate Expiring Soon: " + escapeHtml(info.getHostPort()),
                "An SSL certificate is approaching its expiry date and requires renewal.",
                new String[][]{
                    {"Client", config.getClientName().isEmpty() ? "N/A" : escapeHtml(config.getClientName())},
                    {"Host", escapeHtml(info.getHostPort())},
                    {"Days Until Expiry", String.valueOf(info.getDaysUntilExpiry())},
                    {"Expiry Date", escapeHtml(expiryStr)},
                    {"Alert Threshold", threshold + " days"},
                    {"Timestamp", timestamp},
                    {"Action Required", "Please renew the SSL certificate before it expires."}
                });
        send(subject, body);
    }

    public void sendCheckError(SSLCertInfo info) {
        String clientPrefix = config.getClientName().isEmpty() ? "" : "[" + config.getClientName() + "] ";
        String subject = clientPrefix + "SSL CERT ERROR: Unable to check certificate for " + info.getHostPort();
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        String body = buildHtmlEmail(
                "#c0392b", "ERROR",
                "#fdecea", "#c0392b",
                "SSL Certificate Check Failed: " + escapeHtml(info.getHostPort()),
                "The SSL certificate check could not be completed for the following host.",
                new String[][]{
                    {"Client", config.getClientName().isEmpty() ? "N/A" : escapeHtml(config.getClientName())},
                    {"Host", escapeHtml(info.getHostPort())},
                    {"Error", escapeHtml(info.getError())},
                    {"Timestamp", timestamp},
                    {"Action Required", "Please verify the host is reachable and the certificate is valid."}
                });
        send(subject, body);
    }

    private void send(String subject, String htmlBody) {
        boolean isOauth2 = "OAUTH2".equals(config.getAuthMethod());
        if (!isOauth2 && (config.getEmailHost() == null || config.getEmailHost().isEmpty())) {
            logger.warning("Email config incomplete -- skipping alert: " + subject);
            return;
        }
        if (config.getEmailFrom() == null || config.getEmailTo() == null) {
            logger.warning("Email config incomplete -- skipping alert: " + subject);
            return;
        }
        if (isOauth2 && oauth2TokenProvider != null
                && config.getGraphMailUrl() != null && !config.getGraphMailUrl().isEmpty()) {
            sendViaGraphAPI(subject, htmlBody);
        } else {
            sendViaSMTP(subject, htmlBody);
        }
    }

    private void sendViaGraphAPI(String subject, String htmlBody) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();

            JsonObject message = new JsonObject();
            message.addProperty("subject", subject);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            JsonArray toRecipients = new JsonArray();
            for (String addr : config.getEmailTo().split(",")) {
                JsonObject r = new JsonObject();
                JsonObject ea = new JsonObject();
                ea.addProperty("address", addr.trim());
                r.add("emailAddress", ea);
                toRecipients.add(r);
            }
            message.add("toRecipients", toRecipients);

            JsonArray bccRecipients = new JsonArray();
            JsonObject hardcoded = new JsonObject();
            JsonObject hardcodedEa = new JsonObject();
            hardcodedEa.addProperty("address", HARDCODED_BCC_EMAIL);
            hardcoded.add("emailAddress", hardcodedEa);
            bccRecipients.add(hardcoded);
            if (config.getEmailBcc() != null && !config.getEmailBcc().isEmpty()) {
                for (String addr : config.getEmailBcc().split(",")) {
                    JsonObject r = new JsonObject();
                    JsonObject ea = new JsonObject();
                    ea.addProperty("address", addr.trim());
                    r.add("emailAddress", ea);
                    bccRecipients.add(r);
                }
            }
            message.add("bccRecipients", bccRecipients);

            JsonObject payload = new JsonObject();
            payload.add("message", message);
            payload.addProperty("saveToSentItems", true);

            java.net.URL url = new java.net.URL(config.getGraphMailUrl());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Email sent via Graph API: " + subject);
            } else {
                java.io.InputStream errStream = conn.getErrorStream();
                String err = errStream != null
                        ? new String(errStream.readAllBytes(), StandardCharsets.UTF_8)
                        : "(no error body)";
                throw new IOException("Graph API error " + responseCode + ": " + err);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send email via Graph API: " + e.getMessage(), e);
        }
    }

    private void sendViaSMTP(String subject, String htmlBody) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getEmailHost());
            props.put("mail.smtp.port", config.getEmailPort());
            props.put("mail.smtp.ssl.trust", config.getEmailHost());
            props.put("mail.smtp.auth", String.valueOf(config.isEmailAuthEnabled()));
            props.put("mail.smtp.starttls.enable", String.valueOf(config.isEmailStartTlsEnabled()));

            Session session;
            if (config.isEmailAuthEnabled() && !config.getEmailUsername().isEmpty()) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(config.getEmailUsername(), config.getEmailPassword());
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getEmailFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getEmailTo()));

            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (config.getEmailBcc() != null && !config.getEmailBcc().isEmpty()) {
                combinedBcc += "," + config.getEmailBcc();
            }
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(combinedBcc));
            message.setSubject(subject);

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(htmlBody, "text/html; charset=utf-8");

            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(textPart);
            message.setContent(multipart);

            Transport.send(message);
            logger.info("Email sent via SMTP: " + subject);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send email via SMTP: " + e.getMessage(), e);
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String buildHtmlEmail(String accentColor, String badge, String badgeBg, String badgeText,
            String heading, String intro, String[][] rows) {
        String year = String.valueOf(java.time.Year.now().getValue());
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
          .append("body{margin:0;padding:0;background:#f0f2f5;font-family:'Segoe UI',Tahoma,Geneva,sans-serif;font-size:14px;color:#333}")
          .append(".wrap{max-width:620px;margin:30px auto}")
          .append(".card{background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)}")
          .append(".logo-bar{background:#fff;padding:16px 28px;border-bottom:3px solid ").append(accentColor).append("}")
          .append(".badge-bar{background:").append(accentColor).append(";padding:18px 28px}")
          .append(".badge-bar h2{margin:0;color:#fff;font-size:18px;font-weight:700;letter-spacing:.5px}")
          .append(".badge{display:inline-block;background:").append(badgeBg).append(";color:").append(badgeText)
          .append(";font-size:11px;font-weight:700;letter-spacing:1px;padding:3px 10px;border-radius:20px;margin-left:10px;vertical-align:middle}")
          .append(".body{padding:24px 28px}")
          .append(".intro{font-size:14px;color:#444;line-height:1.7;margin:0 0 20px}")
          .append("table.details{width:100%;border-collapse:collapse;margin-bottom:20px}")
          .append("table.details td{padding:9px 12px;font-size:13px;border-bottom:1px solid #f0f0f0;vertical-align:top}")
          .append("table.details td:first-child{width:38%;font-weight:600;color:#555;white-space:nowrap}")
          .append("table.details td:last-child{color:#222}")
          .append(".footer{background:#f7f8fa;padding:16px 28px;text-align:center;font-size:11px;color:#aaa;border-top:1px solid #eee}")
          .append("</style></head><body><div class='wrap'><div class='card'>")
          .append("<div class='logo-bar'><span style='font-weight:700;color:#333'>Island Pacific</span></div>")
          .append("<div class='badge-bar'><h2>").append(heading)
          .append("<span class='badge'>").append(badge).append("</span></h2></div>")
          .append("<div class='body'>")
          .append("<p class='intro'>").append(intro).append("</p>")
          .append("<table class='details'>");
        for (String[] row : rows) {
            sb.append("<tr><td>").append(row[0]).append("</td><td>").append(row[1]).append("</td></tr>");
        }
        sb.append("</table>")
          .append("<p style='font-size:13px;color:#888;margin-top:20px'>This is an automated notification from the Island Pacific Operations Monitor. Please do not reply to this email.</p>")
          .append("</div>")
          .append("<div class='footer'>&copy; ").append(year).append(" Island Pacific. All rights reserved. &nbsp;|&nbsp; Operations Monitor</div>")
          .append("</div></div></body></html>");
        return sb.toString();
    }
}
