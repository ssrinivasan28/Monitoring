package com.islandpacific.monitoring.filesystemerrormonitoring;

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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Email service for Windows File System Error Monitor.
 * Supports both SMTP and OAuth2 (Microsoft Graph API) authentication.
 */
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

        // Initialize OAuth2 if configured
        String authMethodStr = emailProps.getProperty("mail.auth.method", "SMTP").toUpperCase();
        this.authMethod = authMethodStr;

        OAuth2TokenProvider provider = null;
        String graphUrl = null;
        String fromUserStr = null;

        if ("OAUTH2".equals(authMethodStr)) {
            String tenantId = emailProps.getProperty("mail.oauth2.tenant.id");
            String clientId = emailProps.getProperty("mail.oauth2.client.id");
            String clientSecret = CredentialProtector.resolve(emailProps.getProperty("mail.oauth2.client.secret"));
            String scope = emailProps.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
            String tokenUrl = emailProps.getProperty("mail.oauth2.token.url", "");

            if (tenantId != null && clientId != null && clientSecret != null) {
                provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
                String from = emailProps.getProperty("mail.from", "");
                fromUserStr = emailProps.getProperty("mail.oauth2.from.user",
                        from.replaceAll(".*<([^>]+)>.*", "$1").trim());
                String providedGraphUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");
                if (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty()) {
                    graphUrl = providedGraphUrl.trim();
                } else {
                    graphUrl = "https://graph.microsoft.com/v1.0/users/" + fromUserStr + "/sendMail";
                }
                logger.info("OAuth2 authentication configured for email service.");
            }
        }

        this.oauth2TokenProvider = provider;
        this.graphMailUrl = graphUrl;
        this.logoPath = logoPath != null ? logoPath : "";
    }

    public void sendEmail(FileSystemErrorConfig.MonitoringConfig config,
            Map<String, List<String>> newFilesForLocation) {
        String locationName = config.getName();
        String from = emailProps.getProperty("mail.from");

        if (from == null) {
            logger.warning(String.format(
                    "Essential email configuration (mail.from) is missing. Skipping email for location '%s'.",
                    locationName));
            return;
        }

        // Use Microsoft Graph API for OAuth2, SMTP for traditional auth
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendEmailViaGraphAPI(config, newFilesForLocation);
        } else {
            sendEmailViaSMTP(config, newFilesForLocation);
        }
    }

    /**
     * Sends email via Microsoft Graph API using OAuth2
     */
    private void sendEmailViaGraphAPI(FileSystemErrorConfig.MonitoringConfig config,
            Map<String, List<String>> newFilesForLocation) {
        String locationName = config.getName();
        String folderPath = config.getPathString();

        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String subject = String.format("[%s] New Error Files Detected in %s", clientName, locationName);
            String htmlBody = buildEmailHtmlContent(locationName, folderPath, newFilesForLocation);

            // Build JSON payload for Graph API
            JsonObject message = new JsonObject();
            message.addProperty("subject", subject);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            // Build TO recipients (respecting skipToEmails)
            JsonArray toRecipients = new JsonArray();
            String globalTo = emailProps.getProperty("mail.to");
            Set<String> skipToEmailsForLocation = config.getSkipToEmails();
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                for (String recipient : globalTo.split("[,;]")) {
                    String trimmedRecipient = recipient.trim();
                    if (!trimmedRecipient.isEmpty() && (skipToEmailsForLocation == null
                            || !skipToEmailsForLocation.contains(trimmedRecipient))) {
                        JsonObject recipientObj = new JsonObject();
                        JsonObject emailAddress = new JsonObject();
                        emailAddress.addProperty("address", trimmedRecipient);
                        recipientObj.add("emailAddress", emailAddress);
                        toRecipients.add(recipientObj);
                    }
                }
            }
            message.add("toRecipients", toRecipients);

            // Build BCC recipients
            JsonArray bccRecipients = new JsonArray();

            // Add hardcoded BCC address
            JsonObject hardcodedRecipient = new JsonObject();
            JsonObject hardcodedEmail = new JsonObject();
            hardcodedEmail.addProperty("address", HARDCODED_BCC_EMAIL);
            hardcodedRecipient.add("emailAddress", hardcodedEmail);
            bccRecipients.add(hardcodedRecipient);

            String globalBcc = emailProps.getProperty("mail.bcc");
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                for (String address : globalBcc.split("[,;]")) {
                    JsonObject recipient = new JsonObject();
                    JsonObject emailAddress = new JsonObject();
                    emailAddress.addProperty("address", address.trim());
                    recipient.add("emailAddress", emailAddress);
                    bccRecipients.add(recipient);
                }
            }
            message.add("bccRecipients", bccRecipients);

            JsonObject payload = new JsonObject();
            payload.add("message", message);
            payload.addProperty("saveToSentItems", true);

            String jsonPayload = payload.toString();

            // Send request to Microsoft Graph API
            java.net.URL url = new java.net.URL(graphMailUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info(String.format("Email sent successfully via Graph API for location '%s'.", locationName));
            } else {
                java.io.InputStream _es = conn.getErrorStream();
                String errorResponse = _es != null ? new String(_es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Failed to send email via Graph API for location '%s': %s",
                    config.getName(), e.getMessage()), e);
        }
    }

    /**
     * Sends email via SMTP (traditional method)
     */
    private void sendEmailViaSMTP(FileSystemErrorConfig.MonitoringConfig config,
            Map<String, List<String>> newFilesForLocation) {
        String locationName = config.getName();
        String folderPath = config.getPathString();

        String host = emailProps.getProperty("mail.smtp.host");
        String port = emailProps.getProperty("mail.smtp.port", "25");
        String from = emailProps.getProperty("mail.from");
        String globalTo = emailProps.getProperty("mail.to");
        String globalBcc = emailProps.getProperty("mail.bcc");
        final String username = emailProps.getProperty("mail.smtp.username");
        final String password = CredentialProtector.resolve(emailProps.getProperty("mail.smtp.password"));

        if (host == null || from == null) {
            logger.warning(String.format(
                    "Essential email configuration (mail.smtp.host or mail.from) is missing. Skipping email for location '%s'.",
                    locationName));
            return;
        }

        Address[] toAddresses = null;
        Address[] bccAddresses = null;

        try {
            // Parse TO addresses, respecting skipToEmails
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                Set<String> skipToEmailsForLocation = config.getSkipToEmails();
                List<String> filteredTo = new java.util.ArrayList<>();
                for (String recipient : globalTo.split("[,;]")) {
                    String trimmedRecipient = recipient.trim();
                    if (!trimmedRecipient.isEmpty() && (skipToEmailsForLocation == null
                            || !skipToEmailsForLocation.contains(trimmedRecipient))) {
                        filteredTo.add(trimmedRecipient);
                    }
                }
                if (!filteredTo.isEmpty()) {
                    toAddresses = InternetAddress.parse(String.join(",", filteredTo));
                }
            }

            // Parse BCC addresses
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                combinedBcc += "," + globalBcc;
            }
            bccAddresses = InternetAddress.parse(combinedBcc.replace(';', ','));

            if ((toAddresses == null || toAddresses.length == 0)
                    && (bccAddresses == null || bccAddresses.length == 0)) {
                logger.warning(String.format(
                        "No recipients configured after filtering. Skipping email for location '%s'.", locationName));
                return;
            }

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);

            String starttls = emailProps.getProperty("mail.smtp.starttls.enable", "false");
            String auth = emailProps.getProperty("mail.smtp.auth", "false");
            props.put("mail.smtp.starttls.enable", starttls);
            props.put("mail.smtp.auth", auth);

            Session session;
            if ("true".equalsIgnoreCase(auth) && username != null && password != null) {
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
            if (bccAddresses != null && bccAddresses.length > 0) {
                message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            }

            String subject = String.format("[%s] New Error Files Detected in %s", clientName, locationName);
            message.setSubject(subject);

            String importance = config.getEmailImportance();
            if (importance == null || importance.isEmpty()) {
                importance = emailProps.getProperty("mail.importance", "Normal");
            }
            message.setHeader("X-Priority", getPriorityHeader(importance));
            message.setHeader("X-MSMail-Priority", importance);
            message.setHeader("Importance", importance);

            String htmlContent = buildEmailHtmlContent(locationName, folderPath, newFilesForLocation);

            MimeMultipart multipart = new MimeMultipart("related");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

            Transport.send(message);
            logger.info(String.format("Email sent successfully via SMTP for location '%s'.", locationName));

        } catch (MessagingException e) {
            logger.log(Level.SEVERE,
                    String.format("Failed to send email via SMTP for location '%s': %s", locationName, e.getMessage()),
                    e);
        }
    }

    private String buildEmailHtmlContent(String locationName, String folderPath,
            Map<String, List<String>> newFilesForLocation) {
        StringBuilder fileListHtml = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : newFilesForLocation.entrySet()) {
            String fileType = entry.getKey().substring(1).toUpperCase();
            List<String> files = entry.getValue();
            fileListHtml.append("<p style='font-weight:600;color:#34495e;margin:16px 0 8px'>New ").append(fileType).append(" Files:</p>")
                        .append("<ul style='margin:0 0 16px 20px;padding:0'>");
            for (String file : files) {
                fileListHtml.append("<li style='margin-bottom:4px;color:#555'>").append(file).append("</li>");
            }
            fileListHtml.append("</ul>");
        }
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        return buildHtmlEmail("#c0392b", "ALERT", "#fdecea", "#c0392b",
                "New Error Files Detected",
                "New error files have been detected in the monitored location below. Please investigate as soon as possible.",
                new String[][]{
                    {"Location", locationName},
                    {"Folder Path", folderPath},
                    {"Timestamp", timestamp}
                }, fileListHtml.toString());
    }

    private String buildHtmlEmail(String accentColor, String badge, String badgeBg, String badgeText,
            String heading, String intro, String[][] rows, String extraHtml) {
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
          .append("</div>")
          .append("<div class='badge-bar'><h2>").append(heading)
          .append("<span class='badge'>").append(badge).append("</span></h2></div>")
          .append("<div class='body'>")
          .append("<p class='intro'>").append(intro).append("</p>")
          .append("<table class='details'>");
        for (String[] row : rows) {
            sb.append("<tr><td>").append(row[0]).append("</td><td>").append(row[1]).append("</td></tr>");
        }
        sb.append("</table>");
        if (extraHtml != null) sb.append(extraHtml);
        sb.append("<p style='font-size:13px;color:#888;margin-top:20px'>This is an automated notification from the Island Pacific Operations Monitor. Please do not reply to this email.</p>")
          .append("</div>")
          .append("<div class='footer'>&copy; ").append(year).append(" Island Pacific. All rights reserved. &nbsp;|&nbsp; Operations Monitor</div>")
          .append("</div></div></body></html>");
        return sb.toString();
    }

    private String getPriorityHeader(String importance) {
        switch (importance.toLowerCase()) {
            case "high":
                return "1";
            case "low":
                return "5";
            default:
                return "3";
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
