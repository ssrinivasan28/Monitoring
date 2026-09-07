package com.islandpacific.monitoring.apiurlmonitoring;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private final Properties emailProperties;
    private final String authMethod;
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl;
    private final String clientName;
    private final String logoPath;
    private final String globalEmailImportance;

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";
    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    public EmailService(Properties emailProperties, String clientName, String logoPath) {
        this.emailProperties = emailProperties;
        this.logoPath = logoPath != null ? logoPath : "";
        this.clientName = clientName != null && !clientName.trim().isEmpty() ? clientName : "API URL Monitor";
        this.globalEmailImportance = emailProperties.getProperty("mail.importance", "High");

        String authMethodStr = emailProperties.getProperty("mail.auth.method", "SMTP").toUpperCase();
        this.authMethod = authMethodStr;

        OAuth2TokenProvider provider = null;
        String graphUrl = null;

        if ("OAUTH2".equals(authMethodStr)) {
            String tenantId = emailProperties.getProperty("mail.oauth2.tenant.id");
            String clientId = emailProperties.getProperty("mail.oauth2.client.id");
            String clientSecret = CredentialProtector.resolve(emailProperties.getProperty("mail.oauth2.client.secret"));
            String scope = emailProperties.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
            String tokenUrl = emailProperties.getProperty("mail.oauth2.token.url", "");

            if (tenantId != null && clientId != null && clientSecret != null) {
                provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
                String from = emailProperties.getProperty("mail.from", "");
                String fromUser = emailProperties.getProperty("mail.oauth2.from.user",
                        from.replaceAll(".*<([^>]+)>.*", "$1").trim());
                String providedGraphUrl = emailProperties.getProperty("mail.oauth2.graph.mail.url", "");
                graphUrl = (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty())
                        ? providedGraphUrl.trim()
                        : "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail";
            }
        }

        this.oauth2TokenProvider = provider;
        this.graphMailUrl = graphUrl;
    }

    public void sendDownAlert(String name, String url, String statusInfo, String responseTime) {
        String subject = "API DOWN: " + name;
        String body = "The API endpoint is not returning HTTP 200. Immediate attention required.";
        String[][] rows = {
            {"Client", clientName},
            {"API Name", name},
            {"URL", url},
            {"Status", statusInfo},
            {"Response Time", responseTime},
            {"Timestamp", timestamp()}
        };
        sendEmail(subject, body, rows, "#c0392b", "DOWN", "#fdecea", "#c0392b",
                "API Endpoint Down", "An API endpoint has failed its health check. Please investigate immediately.");
    }

    public void sendRecoveryAlert(String name, String url, int statusCode, int downCycles) {
        String subject = "API RECOVERED: " + name;
        String body = "The API endpoint has recovered and is now returning HTTP 200.";
        String[][] rows = {
            {"Client", clientName},
            {"API Name", name},
            {"URL", url},
            {"Status", "HTTP " + statusCode},
            {"Down Cycles", String.valueOf(downCycles)},
            {"Timestamp", timestamp()}
        };
        sendEmail(subject, body, rows, "#27ae60", "RECOVERED", "#eafaf1", "#27ae60",
                "API Endpoint Recovered", "The API endpoint has recovered and is responding normally.");
    }

    private void sendEmail(String subject, String body, String[][] rows,
            String accentColor, String badge, String badgeBg, String badgeText,
            String heading, String intro) {
        String from = emailProperties.getProperty("mail.from");
        if (from == null) {
            logger.warning("mail.from not configured. Skipping alert email.");
            return;
        }
        String htmlBody = buildHtmlEmail(accentColor, badge, badgeBg, badgeText, heading, intro, rows);
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendViaGraphApi(subject, htmlBody);
        } else {
            sendViaSMTP(subject, htmlBody);
        }
    }

    private void sendViaGraphApi(String subject, String htmlBody) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            JsonObject message = new JsonObject();
            message.addProperty("subject", "[" + clientName + "] " + subject);

            JsonObject bodyObj = new JsonObject();
            bodyObj.addProperty("contentType", "HTML");
            bodyObj.addProperty("content", htmlBody);
            message.add("body", bodyObj);

            JsonArray toRecipients = new JsonArray();
            String globalTo = emailProperties.getProperty("mail.to");
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                for (String address : globalTo.split("[,;]")) {
                    JsonObject r = new JsonObject();
                    JsonObject e = new JsonObject();
                    e.addProperty("address", address.trim());
                    r.add("emailAddress", e);
                    toRecipients.add(r);
                }
            }
            message.add("toRecipients", toRecipients);

            JsonArray bccRecipients = new JsonArray();
            JsonObject hardcoded = new JsonObject();
            JsonObject hardcodedEmail = new JsonObject();
            hardcodedEmail.addProperty("address", HARDCODED_BCC_EMAIL);
            hardcoded.add("emailAddress", hardcodedEmail);
            bccRecipients.add(hardcoded);
            String globalBcc = emailProperties.getProperty("mail.bcc");
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                for (String address : globalBcc.split("[,;]")) {
                    JsonObject r = new JsonObject();
                    JsonObject e = new JsonObject();
                    e.addProperty("address", address.trim());
                    r.add("emailAddress", e);
                    bccRecipients.add(r);
                }
            }
            message.add("bccRecipients", bccRecipients);

            JsonObject payload = new JsonObject();
            payload.add("message", message);
            payload.addProperty("saveToSentItems", true);

            java.net.URL url = new java.net.URL(graphMailUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            byte[] input = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            conn.getOutputStream().write(input);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                logger.info("Alert email sent via Graph API.");
            } else {
                java.io.InputStream es = conn.getErrorStream();
                String err = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API error " + code + ": " + err);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send email via Graph API: " + e.getMessage(), e);
        }
    }

    private void sendViaSMTP(String subject, String htmlBody) {
        String host = emailProperties.getProperty("mail.smtp.host");
        String port = emailProperties.getProperty("mail.smtp.port", "25");
        String from = emailProperties.getProperty("mail.from");
        String globalTo = emailProperties.getProperty("mail.to");
        String globalBcc = emailProperties.getProperty("mail.bcc");
        final String username = emailProperties.getProperty("mail.smtp.username");
        final String password = CredentialProtector.resolve(emailProperties.getProperty("mail.smtp.password"));

        if (host == null || from == null) {
            logger.warning("SMTP host or from not configured. Skipping alert email.");
            return;
        }

        try {
            InternetAddress[] toAddresses = (globalTo != null && !globalTo.trim().isEmpty())
                    ? InternetAddress.parse(globalTo.replace(';', ',')) : null;
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (globalBcc != null && !globalBcc.trim().isEmpty()) combinedBcc += "," + globalBcc;
            InternetAddress[] bccAddresses = InternetAddress.parse(combinedBcc.replace(';', ','));

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", emailProperties.getProperty("mail.smtp.starttls.enable", "false"));
            props.put("mail.smtp.auth", emailProperties.getProperty("mail.smtp.auth", "false"));

            Session session;
            if ("true".equalsIgnoreCase(emailProperties.getProperty("mail.smtp.auth", "false"))
                    && username != null && password != null) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            if (toAddresses != null && toAddresses.length > 0)
                message.setRecipients(Message.RecipientType.TO, toAddresses);
            if (bccAddresses.length > 0)
                message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            message.setSubject("[" + clientName + "] " + subject);
            message.setHeader("X-Priority", "1");
            message.setHeader("X-MSMail-Priority", "High");
            message.setHeader("Importance", globalEmailImportance);

            MimeMultipart multipart = new MimeMultipart("related");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);
            Transport.send(message);
            logger.info("Alert email sent via SMTP.");
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email via SMTP: " + e.getMessage(), e);
        }
    }

    private String buildLogoDataUri() {
        if (!logoPath.isEmpty()) {
            java.nio.file.Path lp = java.nio.file.Paths.get(logoPath);
            if (java.nio.file.Files.exists(lp)) {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(lp);
                    String mime = logoPath.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    logger.info("Embedding logo from file: " + lp.toAbsolutePath());
                    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                } catch (IOException e) {
                    logger.warning("Failed to read logo file: " + e.getMessage());
                }
            } else {
                logger.warning("Logo file not found at: " + lp.toAbsolutePath() + " — using default");
            }
        }
        return DEFAULT_LOGO_BASE64;
    }

    private String buildHtmlEmail(String accentColor, String badge, String badgeBg, String badgeText,
            String heading, String intro, String[][] rows) {
        String year = String.valueOf(java.time.Year.now().getValue());
        String logoSrc = buildLogoDataUri();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
          .append("body{margin:0;padding:0;background:#f0f2f5;font-family:'Segoe UI',Tahoma,Geneva,sans-serif;font-size:14px;color:#333}")
          .append(".wrap{max-width:620px;margin:30px auto}")
          .append(".card{background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)}")
          .append(".logo-bar{background:#fff;padding:16px 28px;border-bottom:3px solid ").append(accentColor).append("}")
          .append(".logo-bar img{display:block;max-width:150px;height:50px;object-fit:contain}")
          .append(".badge-bar{background:").append(accentColor).append(";padding:18px 28px}")
          .append(".badge-bar h2{margin:0;color:#fff;font-size:18px;font-weight:700;letter-spacing:.5px}")
          .append(".badge{display:inline-block;background:").append(badgeBg).append(";color:").append(badgeText).append(";font-size:11px;font-weight:700;letter-spacing:1px;padding:3px 10px;border-radius:20px;margin-left:10px;vertical-align:middle}")
          .append(".body{padding:24px 28px}")
          .append(".intro{font-size:14px;color:#444;line-height:1.7;margin:0 0 20px}")
          .append("table.details{width:100%;border-collapse:collapse;margin-bottom:20px}")
          .append("table.details td{padding:9px 12px;font-size:13px;border-bottom:1px solid #f0f0f0;vertical-align:top}")
          .append("table.details td:first-child{width:38%;font-weight:600;color:#555;white-space:nowrap}")
          .append("table.details td:last-child{color:#222}")
          .append(".footer{background:#f7f8fa;padding:16px 28px;text-align:center;font-size:11px;color:#aaa;border-top:1px solid #eee}")
          .append("</style></head><body><div class='wrap'><div class='card'>")
          .append("<div class='logo-bar'><img src='").append(logoSrc).append("' alt='Island Pacific'/></div>")
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

    private String timestamp() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
    }
}
