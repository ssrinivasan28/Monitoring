package com.islandpacific.monitoring.folderkeywordmonitoring;

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

    private final Properties emailProps;
    private final Logger logger;
    private final String clientName;
    private final String authMethod;
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl;
    private final String logoPath;
    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    public EmailService(Properties emailProps, String clientName, Logger logger, String logoPath) {
        this.emailProps = emailProps;
        this.clientName = clientName;
        this.logger = logger;

        String authMethodStr = emailProps.getProperty("mail.auth.method", "SMTP").toUpperCase();
        this.authMethod = authMethodStr;

        OAuth2TokenProvider provider = null;
        String graphUrl = null;

        if ("OAUTH2".equals(authMethodStr)) {
            String tenantId = emailProps.getProperty("mail.oauth2.tenant.id");
            String clientId = emailProps.getProperty("mail.oauth2.client.id");
            String clientSecret = CredentialProtector.resolve(emailProps.getProperty("mail.oauth2.client.secret"));
            String scope = emailProps.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
            String tokenUrl = emailProps.getProperty("mail.oauth2.token.url", "");

            if (tenantId != null && clientId != null && clientSecret != null) {
                provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
                String from = emailProps.getProperty("mail.from", "");
                String fromUserStr = emailProps.getProperty("mail.oauth2.from.user",
                        from.replaceAll(".*<([^>]+)>.*", "$1").trim());
                String providedGraphUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");
                if (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty()) {
                    graphUrl = providedGraphUrl.trim();
                } else {
                    graphUrl = "https://graph.microsoft.com/v1.0/users/" + fromUserStr + "/sendMail";
                }
            }
        }

        this.oauth2TokenProvider = provider;
        this.graphMailUrl = graphUrl;
        this.logoPath = logoPath != null ? logoPath : "";
    }

    public void sendAlert(String subject, String messageBody) {
        String from = emailProps.getProperty("mail.from");
        if (from == null) {
            logger.warning("mail.from not configured. Skipping email alert.");
            return;
        }
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendAlertViaGraphAPI(subject, messageBody);
        } else {
            sendAlertViaSMTP(subject, messageBody);
        }
    }

    private void sendAlertViaGraphAPI(String subject, String messageBody) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = buildAlertHtmlContent(subject, messageBody);

            JsonObject message = new JsonObject();
            message.addProperty("subject", "[" + clientName + "] " + subject);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            JsonArray toRecipients = new JsonArray();
            String globalTo = emailProps.getProperty("mail.to");
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                for (String r : globalTo.split("[,;]")) {
                    String t = r.trim();
                    if (!t.isEmpty()) {
                        JsonObject rec = new JsonObject();
                        JsonObject ea = new JsonObject();
                        ea.addProperty("address", t);
                        rec.add("emailAddress", ea);
                        toRecipients.add(rec);
                    }
                }
            }
            message.add("toRecipients", toRecipients);

            JsonArray bccRecipients = new JsonArray();
            JsonObject hardcoded = new JsonObject();
            JsonObject hardcodedEa = new JsonObject();
            hardcodedEa.addProperty("address", HARDCODED_BCC_EMAIL);
            hardcoded.add("emailAddress", hardcodedEa);
            bccRecipients.add(hardcoded);
            String globalBcc = emailProps.getProperty("mail.bcc");
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                for (String a : globalBcc.split("[,;]")) {
                    JsonObject rec = new JsonObject();
                    JsonObject ea = new JsonObject();
                    ea.addProperty("address", a.trim());
                    rec.add("emailAddress", ea);
                    bccRecipients.add(rec);
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
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Email sent via Graph API.");
            } else {
                java.io.InputStream _es = conn.getErrorStream();
                String err = _es != null ? new String(_es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API error " + responseCode + ": " + err);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send email via Graph API: " + e.getMessage(), e);
        }
    }

    private void sendAlertViaSMTP(String subject, String messageBody) {
        String host = emailProps.getProperty("mail.smtp.host");
        String port = emailProps.getProperty("mail.smtp.port", "25");
        String from = emailProps.getProperty("mail.from");
        String globalTo = emailProps.getProperty("mail.to");
        String globalBcc = emailProps.getProperty("mail.bcc");
        final String username = emailProps.getProperty("mail.smtp.username");
        final String password = CredentialProtector.resolve(emailProps.getProperty("mail.smtp.password"));

        if (host == null || from == null) {
            logger.warning("mail.smtp.host or mail.from not configured. Skipping.");
            return;
        }

        try {
            Address[] toAddresses = null;
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                toAddresses = InternetAddress.parse(globalTo.replace(';', ','));
            }
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                combinedBcc += "," + globalBcc;
            }
            Address[] bccAddresses = InternetAddress.parse(combinedBcc.replace(';', ','));

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", emailProps.getProperty("mail.smtp.starttls.enable", "false"));
            props.put("mail.smtp.auth", emailProps.getProperty("mail.smtp.auth", "false"));

            Session session;
            if ("true".equalsIgnoreCase(emailProps.getProperty("mail.smtp.auth", "false"))
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
            if (toAddresses != null && toAddresses.length > 0) {
                message.setRecipients(Message.RecipientType.TO, toAddresses);
            }
            message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            message.setSubject("[" + clientName + "] " + subject);

            String importance = emailProps.getProperty("mail.importance", "High");
            message.setHeader("X-Priority", getPriorityHeader(importance));
            message.setHeader("X-MSMail-Priority", importance);
            message.setHeader("Importance", importance);

            String htmlContent = buildAlertHtmlContent(subject, messageBody);
            MimeMultipart multipart = new MimeMultipart("related");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);
            Transport.send(message);
            logger.info("Email sent via SMTP.");
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email via SMTP: " + e.getMessage(), e);
        }
    }

    private String buildAlertHtmlContent(String subject, String messageBody) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        String year = String.valueOf(java.time.Year.now().getValue());
        String logoSrc = buildLogoDataUri();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
          .append("body{margin:0;padding:0;background:#f0f2f5;font-family:'Segoe UI',Tahoma,Geneva,sans-serif;font-size:14px;color:#333}")
          .append(".wrap{max-width:620px;margin:30px auto}")
          .append(".card{background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)}")
          .append(".logo-bar{background:#fff;padding:16px 28px;border-bottom:3px solid #c0392b}")
          .append(".logo-bar img{display:block;max-width:150px;height:50px;object-fit:contain}")
          .append(".badge-bar{background:#c0392b;padding:18px 28px}")
          .append(".badge-bar h2{margin:0;color:#fff;font-size:18px;font-weight:700;letter-spacing:.5px}")
          .append(".badge{display:inline-block;background:#fdecea;color:#c0392b;font-size:11px;font-weight:700;letter-spacing:1px;padding:3px 10px;border-radius:20px;margin-left:10px;vertical-align:middle}")
          .append(".body{padding:24px 28px}")
          .append(".intro{font-size:14px;color:#444;line-height:1.7;margin:0 0 20px}")
          .append("table.details{width:100%;border-collapse:collapse;margin-bottom:20px}")
          .append("table.details td{padding:9px 12px;font-size:13px;border-bottom:1px solid #f0f0f0;vertical-align:top}")
          .append("table.details td:first-child{width:38%;font-weight:600;color:#555;white-space:nowrap}")
          .append(".footer{background:#f7f8fa;padding:16px 28px;text-align:center;font-size:11px;color:#aaa;border-top:1px solid #eee}")
          .append("</style></head><body><div class='wrap'><div class='card'>")
          .append("<div class='logo-bar'><img src='").append(logoSrc).append("' alt='Island Pacific'/></div>")
          .append("</div>")
          .append("<div class='badge-bar'><h2>Folder Keyword Alert<span class='badge'>ALERT</span></h2></div>")
          .append("<div class='body'>")
          .append("<p class='intro'>One or more monitored keywords were detected in the folder. Please review the details below.</p>")
          .append("<table class='details'>")
          .append("<tr><td>Timestamp</td><td>").append(timestamp).append("</td></tr>")
          .append("</table>")
          .append(messageBody)
          .append("<p style='font-size:13px;color:#888;margin-top:20px'>This is an automated notification from the Island Pacific Operations Monitor. Please do not reply to this email.</p>")
          .append("</div>")
          .append("<div class='footer'>&copy; ").append(year).append(" Island Pacific. All rights reserved. &nbsp;|&nbsp; Operations Monitor</div>")
          .append("</div></div></body></html>");
        return sb.toString();
    }

    private String getPriorityHeader(String importance) {
        switch (importance.toLowerCase()) {
            case "high": return "1";
            case "low": return "5";
            default: return "3";
        }
    }

    private String buildLogoDataUri() {
        if (!logoPath.isEmpty()) {
            java.nio.file.Path p = java.nio.file.Paths.get(logoPath);
            if (java.nio.file.Files.exists(p)) {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(p);
                    String mime = logoPath.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                } catch (java.io.IOException e) {
                    logger.warning("Could not load logo from " + logoPath + ": " + e.getMessage());
                }
            }
        }
        return DEFAULT_LOGO_BASE64;
    }
}
