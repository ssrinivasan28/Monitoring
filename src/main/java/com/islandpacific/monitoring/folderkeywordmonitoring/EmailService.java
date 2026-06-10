package com.islandpacific.monitoring.folderkeywordmonitoring;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    public EmailService(Properties emailProps, String clientName, Logger logger) {
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
            String clientSecret = emailProps.getProperty("mail.oauth2.client.secret");
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
            String htmlBody = buildAlertHtmlContent(subject, messageBody, true);

            JsonObject message = new JsonObject();
            message.addProperty("subject", "[" + clientName + "] " + subject);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            JsonArray toRecipients = new JsonArray();
            String globalTo = emailProps.getProperty("mail.to");
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                for (String r : globalTo.split(",")) {
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
                for (String a : globalBcc.split(",")) {
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
                String err = new String(conn.getErrorStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
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
        final String password = emailProps.getProperty("mail.smtp.password");

        if (host == null || from == null) {
            logger.warning("mail.smtp.host or mail.from not configured. Skipping.");
            return;
        }

        try {
            Address[] toAddresses = null;
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                toAddresses = InternetAddress.parse(globalTo);
            }
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                combinedBcc += "," + globalBcc;
            }
            Address[] bccAddresses = InternetAddress.parse(combinedBcc);

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

            String htmlContent = buildAlertHtmlContent(subject, messageBody, false);
            MimeMultipart multipart = new MimeMultipart("related");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            byte[] logoBytes = Base64.getDecoder().decode(DEFAULT_LOGO_BASE64.split(",")[1]);
            MimeBodyPart logoPart = new MimeBodyPart();
            logoPart.setDataHandler(new DataHandler(new ByteArrayDataSource(logoBytes, "image/jpeg")));
            logoPart.setHeader("Content-ID", "<logo>");
            multipart.addBodyPart(logoPart);

            message.setContent(multipart);
            Transport.send(message);
            logger.info("Email sent via SMTP.");
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email via SMTP: " + e.getMessage(), e);
        }
    }

    private String buildAlertHtmlContent(String subject, String messageBody, boolean useDataUri) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        String year = String.valueOf(java.time.Year.now().getValue());
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
          .append("<div class='logo-bar'>");
        if (useDataUri) {
            sb.append("<img src='").append(DEFAULT_LOGO_BASE64).append("' alt='Island Pacific'/>");
        } else {
            sb.append("<img src='cid:logo' alt='Island Pacific'/>");
        }
        sb.append("</div>")
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
}
