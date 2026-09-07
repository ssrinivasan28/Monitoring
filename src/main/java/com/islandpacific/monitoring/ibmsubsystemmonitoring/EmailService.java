package com.islandpacific.monitoring.ibmsubsystemmonitoring;

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

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";
    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    private final Properties emailProps;
    private final String ibmiHost;
    private final String clientName;
    private final String importance;
    private final String authMethod;
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl;
    private final String logoPath;

    public EmailService(Properties emailProps, String ibmiHost, String clientName, String logoPath) {
        this.emailProps = emailProps;
        this.ibmiHost = ibmiHost != null ? ibmiHost : "";
        this.clientName = clientName != null ? clientName : "";
        this.importance = emailProps.getProperty("mail.importance", "Normal");

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
                String fromUser = emailProps.getProperty("mail.oauth2.from.user",
                        from.replaceAll(".*<([^>]+)>.*", "$1").trim());
                String providedGraphUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");
                graphUrl = (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty())
                        ? providedGraphUrl.trim()
                        : "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail";
                logger.info("OAuth2 authentication configured for email service.");
            }
        }

        this.oauth2TokenProvider = provider;
        this.graphMailUrl = graphUrl;
        this.logoPath = logoPath != null ? logoPath : "";
    }

    public void sendSubsystemStatusAlert(SubsystemInfo subsystemInfo) {
        String from = emailProps.getProperty("mail.from");
        String to = emailProps.getProperty("mail.to");
        if (from == null || to == null) {
            logger.warning("Essential email configuration missing. Skipping email alert.");
            return;
        }
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendViaGraphAPI(subsystemInfo);
        } else {
            sendViaSMTP(subsystemInfo);
        }
    }

    private String buildSubject(SubsystemInfo subsystemInfo) {
        String prefix = isBlank(clientName) ? "" : "[" + clientName + "] ";
        return String.format("%sIBM i Subsystem Alert: %s/%s is %s on %s",
                prefix, subsystemInfo.getLibrary(), subsystemInfo.getName(),
                subsystemInfo.getStatus(), ibmiHost);
    }

    private void sendViaGraphAPI(SubsystemInfo subsystemInfo) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = buildHtmlContent(subsystemInfo);

            JsonObject message = new JsonObject();
            message.addProperty("subject", buildSubject(subsystemInfo));
            message.addProperty("importance",
                    "High".equalsIgnoreCase(importance) ? "high"
                    : "Low".equalsIgnoreCase(importance) ? "low" : "normal");

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            String to = emailProps.getProperty("mail.to", "");
            JsonArray toRecipients = new JsonArray();
            for (String address : to.split("[,;]")) {
                if (!address.trim().isEmpty()) {
                    JsonObject r = new JsonObject(); JsonObject e = new JsonObject();
                    e.addProperty("address", address.trim()); r.add("emailAddress", e); toRecipients.add(r);
                }
            }
            message.add("toRecipients", toRecipients);

            JsonArray bccRecipients = new JsonArray();
            JsonObject hb = new JsonObject(); JsonObject he = new JsonObject();
            he.addProperty("address", HARDCODED_BCC_EMAIL); hb.add("emailAddress", he); bccRecipients.add(hb);
            String bcc = emailProps.getProperty("mail.bcc", "");
            for (String address : bcc.split("[,;]")) {
                if (!address.trim().isEmpty()) {
                    JsonObject r = new JsonObject(); JsonObject e = new JsonObject();
                    e.addProperty("address", address.trim()); r.add("emailAddress", e); bccRecipients.add(r);
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
                os.write(payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Subsystem alert sent via Graph API for " + subsystemInfo.getName());
            } else {
                java.io.InputStream es = conn.getErrorStream();
                String err = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API failed: " + responseCode + ": " + err);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send subsystem alert via Graph API for "
                    + subsystemInfo.getName() + ": " + e.getMessage(), e);
        }
    }

    private void sendViaSMTP(SubsystemInfo subsystemInfo) {
        String host = emailProps.getProperty("mail.smtp.host");
        String port = emailProps.getProperty("mail.smtp.port", "25");
        String from = emailProps.getProperty("mail.from");
        String to = emailProps.getProperty("mail.to");
        String bcc = emailProps.getProperty("mail.bcc", "");
        final String username = emailProps.getProperty("mail.smtp.username", "");
        final String password = CredentialProtector.resolve(emailProps.getProperty("mail.smtp.password", ""));
        boolean authEnabled = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
        boolean startTls = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "false"));

        if (host == null || from == null) {
            logger.warning("Essential email configuration missing. Skipping email alert.");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", String.valueOf(authEnabled));
            props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
            props.put("mail.smtp.ssl.trust", host);

            Session session;
            if (authEnabled && !username.isEmpty() && !password.isEmpty()) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.replace(';', ',')));
            String combinedBcc = HARDCODED_BCC_EMAIL + (bcc.isEmpty() ? "" : "," + bcc);
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(combinedBcc.replace(';', ',')));
            message.setSubject(buildSubject(subsystemInfo));

            if ("High".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "1"); message.setHeader("X-MSMail-Priority", "High"); message.setHeader("Importance", "High");
            } else if ("Low".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "5"); message.setHeader("X-MSMail-Priority", "Low"); message.setHeader("Importance", "Low");
            } else {
                message.setHeader("X-Priority", "3"); message.setHeader("X-MSMail-Priority", "Normal"); message.setHeader("Importance", "Normal");
            }

            String htmlBody = buildHtmlContent(subsystemInfo);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html");

            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

            Transport.send(message);
            logger.info("Subsystem alert sent via SMTP for " + subsystemInfo.getName());
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send subsystem alert via SMTP for " + subsystemInfo.getName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error sending email for " + subsystemInfo.getName() + ": " + e.getMessage(), e);
        }
    }

    String buildHtmlContent(SubsystemInfo subsystemInfo, boolean embedLogoAsDataUri) {
        return buildHtmlContent(subsystemInfo);
    }

    private String buildHtmlContent(SubsystemInfo subsystemInfo) {
        String logoDataUri = buildLogoDataUri();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>IBM i Subsystem Alert</title><style>")
          .append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;font-size:14px;color:#333;background-color:#f4f4f4;margin:0;padding:0}")
          .append("table{border-collapse:collapse}.container{max-width:600px;margin:20px auto;background:#fff;border-radius:8px;box-shadow:0 4px 10px rgba(0,0,0,.05);overflow:hidden}")
          .append(".header{background:#fff;padding:10px 25px;height:60px}.header img{display:block;max-width:150px;height:100%;object-fit:contain;object-position:left center;margin:0}")
          .append(".content-area{padding:25px;line-height:1.6}h3{font-size:20px;color:#e74c3c;margin-top:0;margin-bottom:15px;font-weight:600}")
          .append("h4{font-size:16px;color:#34495e;margin-top:20px;margin-bottom:10px;font-weight:600;border-bottom:1px solid #eee;padding-bottom:5px}")
          .append("p{font-size:14px;color:#555;margin-bottom:10px}ul{list-style-type:disc;margin-left:25px;padding-left:0;margin-top:5px;margin-bottom:15px}")
          .append("li{margin-bottom:5px;color:#555}strong{color:#333;font-weight:700}")
          .append(".footer{background:#f9f9f9;padding:20px 25px;text-align:center;font-size:12px;color:#999;border-top:1px solid #eee}</style></head>")
          .append("<body><table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\">")
          .append("<table class=\"container\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr><td class=\"header\">");

        sb.append("<img src='").append(logoDataUri).append("' ");
        sb.append("alt='Company Logo' width='150' height='60' style='display:block;max-width:150px;height:60px;object-fit:contain;object-position:left center;margin:0'/></td></tr>")
          .append("<tr><td class=\"content-area\"><h3>IBM i Subsystem Status Alert</h3><p>Hi Team,</p>");

        if (!isBlank(clientName)) {
            sb.append(String.format("<p>Client: <strong>%s</strong></p>", escapeHtml(clientName)));
        }

        sb.append(String.format("<p>A critical subsystem on <strong>%s</strong> has changed its status.</p>", escapeHtml(ibmiHost)))
          .append("<h4>Subsystem Details:</h4><ul>")
          .append("<li>Name: <strong>").append(escapeHtml(subsystemInfo.getName())).append("</strong></li>")
          .append("<li>Description: <strong>").append(escapeHtml(subsystemInfo.getDescription())).append("</strong></li>")
          .append("<li>Library: <strong>").append(escapeHtml(subsystemInfo.getLibrary())).append("</strong></li>")
          .append("<li>Current Status: <strong style=\"color:")
          .append("ACTIVE".equalsIgnoreCase(subsystemInfo.getStatus()) ? "#28a745" : "#dc3545").append(";\">")
          .append(escapeHtml(subsystemInfo.getStatus())).append("</strong></li></ul>")
          .append("<p>Please investigate the IBM i system as soon as possible.</p>")
          .append("<p>Thank you,</p><p>Island Pacific Retail Systems</p></td></tr>")
          .append("<tr><td class=\"footer\"><p>&copy; ").append(java.time.Year.now().getValue())
          .append(" Island Pacific. All rights reserved.</p></td></tr></table></td></tr></table></body></html>");
        return sb.toString();
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

