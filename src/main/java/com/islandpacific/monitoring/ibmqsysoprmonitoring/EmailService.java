package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.CredentialProtector;

public class EmailService {

    private static final Logger LOGGER = com.islandpacific.monitoring.common.AppLogger.getLogger(); // Use AppLogger for
                                                                                                    // consistency

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    private final Properties emailProperties; // Properties object holding email configuration
    private final String globalEmailImportance; // Global importance from email.properties
    private final String authMethod; // "SMTP" or "OAUTH2"
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl; // Microsoft Graph API endpoint for sending mail
    private final String logoPath;

    public EmailService(Properties emailProperties, String logoPath) {
        this.emailProperties = emailProperties;
        this.globalEmailImportance = emailProperties.getProperty("mail.importance", "Normal");

        // Initialize OAuth2 if configured
        String authMethodStr = emailProperties.getProperty("mail.auth.method", "SMTP").toUpperCase();
        this.authMethod = authMethodStr;

        OAuth2TokenProvider provider = null;
        String graphUrl = null;
        String fromUserStr = null;

        if ("OAUTH2".equals(authMethodStr)) {
            String tenantId = emailProperties.getProperty("mail.oauth2.tenant.id");
            String clientId = emailProperties.getProperty("mail.oauth2.client.id");
            String clientSecret = CredentialProtector.resolve(emailProperties.getProperty("mail.oauth2.client.secret"));
            String scope = emailProperties.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
            String tokenUrl = emailProperties.getProperty("mail.oauth2.token.url", "");

            if (tenantId != null && clientId != null && clientSecret != null) {
                provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
                String from = emailProperties.getProperty("mail.from", "");
                fromUserStr = emailProperties.getProperty("mail.oauth2.from.user",
                        from.replaceAll(".*<([^>]+)>.*", "$1").trim());
                String providedGraphUrl = emailProperties.getProperty("mail.oauth2.graph.mail.url", "");
                if (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty()) {
                    graphUrl = providedGraphUrl.trim();
                } else {
                    graphUrl = "https://graph.microsoft.com/v1.0/users/" + fromUserStr + "/sendMail";
                }
                LOGGER.info("OAuth2 authentication configured for email service.");
            }
        }

        this.oauth2TokenProvider = provider;
        this.graphMailUrl = graphUrl;
        this.logoPath = logoPath != null ? logoPath : "";
    }

    public void sendEmailAlert(MessageInfo messageData, String subject) {
        String from = emailProperties.getProperty("mail.from");

        // Validate essential email configuration
        if (from == null) {
            LOGGER.warning("Essential email configuration (mail.from) is missing. Skipping email.");
            return;
        }

        // Use Microsoft Graph API for OAuth2, SMTP for traditional auth
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendEmailAlertViaGraphAPI(messageData, subject);
        } else {
            sendEmailAlertViaSMTP(messageData, subject);
        }
    }

    /**
     * Sends email alert via Microsoft Graph API using OAuth2
     */
    private void sendEmailAlertViaGraphAPI(MessageInfo messageData, String subject) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = buildEmailHtmlContent(messageData, subject);

            // Build JSON payload for Graph API
            JsonObject message = new JsonObject();
            message.addProperty("subject", subject);
            message.addProperty("importance", globalEmailImportance);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            // Build TO recipients
            JsonArray toRecipients = new JsonArray();
            String globalTo = emailProperties.getProperty("mail.to");
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                String[] toAddresses = globalTo.split("[,;]");
                for (String address : toAddresses) {
                    JsonObject recipient = new JsonObject();
                    JsonObject emailAddress = new JsonObject();
                    emailAddress.addProperty("address", address.trim());
                    recipient.add("emailAddress", emailAddress);
                    toRecipients.add(recipient);
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

            String globalBcc = emailProperties.getProperty("mail.bcc");
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                String[] bccAddresses = globalBcc.split("[,;]");
                for (String address : bccAddresses) {
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
                LOGGER.info(String.format("Email alert sent successfully via Graph API for message ID '%s'.",
                        messageData.getMessageId()));
            } else {
                java.io.InputStream _es = conn.getErrorStream();
                String errorResponse = _es != null ? new String(_es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send email alert via Graph API for message ID '"
                    + messageData.getMessageId() + "': " + e.getMessage(), e);
            QSYSOPRMonitorMetrics.getEmailSendErrors().inc();
        }
    }

    /**
     * Sends email alert via SMTP (traditional method)
     */
    private void sendEmailAlertViaSMTP(MessageInfo messageData, String subject) {
        String host = emailProperties.getProperty("mail.smtp.host");
        String port = emailProperties.getProperty("mail.smtp.port", "25");
        String from = emailProperties.getProperty("mail.from");
        String globalTo = emailProperties.getProperty("mail.to");
        String globalBcc = emailProperties.getProperty("mail.bcc");
        final String username = emailProperties.getProperty("mail.smtp.username");
        final String password = CredentialProtector.resolve(emailProperties.getProperty("mail.smtp.password"));

        // Validate essential email configuration
        if (host == null || from == null) {
            LOGGER.warning("Essential email configuration (mail.smtp.host or mail.from) is missing. Skipping email.");
            return;
        }

        InternetAddress[] toAddresses = null;
        InternetAddress[] bccAddresses = null;

        try {
            // Parse TO addresses
            if (globalTo != null && !globalTo.trim().isEmpty()) {
                toAddresses = InternetAddress.parse(globalTo.replace(';', ','));
            }

            // Parse BCC addresses
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                combinedBcc += "," + globalBcc;
            }
            bccAddresses = InternetAddress.parse(combinedBcc.replace(';', ','));

            if ((toAddresses == null || toAddresses.length == 0)
                    && (bccAddresses == null || bccAddresses.length == 0)) {
                LOGGER.warning("No valid TO or BCC recipients configured for email. Skipping email.");
                return;
            }

            // Set up mail session properties
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            boolean authEnabled = Boolean.parseBoolean(emailProperties.getProperty("mail.smtp.auth", "false"));
            props.put("mail.smtp.auth", String.valueOf(authEnabled));
            props.put("mail.smtp.starttls.enable", emailProperties.getProperty("mail.smtp.starttls.enable", "false"));
            // props.put("mail.smtp.ssl.trust", host); // Trust the host, useful for
            // self-signed or internal certificates, uncomment if needed

            // Create mail session with or without authentication
            Session session;
            if (authEnabled && username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
                LOGGER.info("SMTP authentication enabled for email.");
            } else {
                // Corrected typo: Session.info(props) -> Session.getInstance(props)
                session = Session.getInstance(props);
                if (authEnabled) { // Auth enabled but credentials missing
                    LOGGER.warning(
                            "SMTP authentication is enabled but username/password are not configured. Email may fail.");
                } else { // Auth explicitly disabled
                    LOGGER.info("SMTP authentication is disabled for email.");
                }
            }

            // Create a new MimeMessage
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            // Set recipients
            if (toAddresses != null && toAddresses.length > 0) {
                message.setRecipients(Message.RecipientType.TO, toAddresses);
            }
            if (bccAddresses != null && bccAddresses.length > 0) {
                message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            }

            // Set email subject
            message.setSubject(subject);

            // Determine email importance: use globalEmailImportance from properties
            String finalImportance = this.globalEmailImportance;

            // Set email importance header
            if ("High".equalsIgnoreCase(finalImportance)) {
                message.setHeader("X-Priority", "1");
                message.setHeader("X-MSMail-Priority", "High");
                message.setHeader("Importance", "High");
                LOGGER.info("Email importance set to High.");
            } else if ("Low".equalsIgnoreCase(finalImportance)) {
                message.setHeader("X-Priority", "5");
                message.setHeader("X-MSMail-Priority", "Low");
                message.setHeader("Importance", "Low");
                LOGGER.info("Email importance set to Low.");
            } else { // Default to Normal
                message.setHeader("X-Priority", "3");
                message.setHeader("X-MSMail-Priority", "Normal");
                message.setHeader("Importance", "Normal");
                LOGGER.info("Email importance set to Normal (default).");
            }

            String htmlBody = buildEmailHtmlContent(messageData, subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(htmlBody, "text/html");

            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            // Send the email
            Transport.send(message);
            LOGGER.info(String.format("Email alert sent successfully via SMTP for message ID '%s'.",
                    messageData.getMessageId()));

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email via SMTP: " + e.getMessage(), e);
            QSYSOPRMonitorMetrics.getEmailSendErrors().inc();
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Base64 decoding error for logo: " + e.getMessage()
                    + ". Please check the DEFAULT_LOGO_BASE64 string.", e);
            QSYSOPRMonitorMetrics.getEmailSendErrors().inc();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during email sending via SMTP: " + e.getMessage(),
                    e);
            QSYSOPRMonitorMetrics.getEmailSendErrors().inc();
        }
    }

    private String buildEmailHtmlContent(MessageInfo messageData, String subject) {
        String logoDataUri = buildLogoDataUri();
        StringBuilder htmlBodyBuilder = new StringBuilder();

        // Start HTML structure with basic styling for a responsive and readable email
        htmlBodyBuilder.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("<meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>IBM i QSYSOPR Alert</title>") // Changed title
                .append("<style>")
                .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 14px; color: #333333; background-color: #f4f4f4; margin: 0; padding: 0; -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }")
                .append("table { border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; }")
                .append("img { border: 0; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; }")
                .append("a { text-decoration: none; color: #1a73e8; }")
                .append(".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); overflow: hidden; }")
                .append(".header { background-color: #ffffff; padding: 10px 25px; text-align: left; height: 60px; }")
                .append(".header img { display: block; max-width: 150px; height: 100%; object-fit: contain; object-position: left center; margin: 0; }")
                .append(".content-area { padding: 25px; line-height: 1.6; }")
                .append("h3 { font-size: 20px; color: #e74c3c; margin-top: 0; margin-bottom: 15px; font-weight: 600; }")
                .append("h4 { font-size: 16px; color: #34495e; margin-top: 20px; margin-bottom: 10px; font-weight: 600; border-bottom: 1px solid #eeeeee; padding-bottom: 5px; }")
                .append("p { font-size: 14px; color: #555555; margin-bottom: 10px; }")
                .append("ul { list-style-type: disc; margin-left: 25px; padding-left: 0; margin-top: 5px; margin-bottom: 15px; }")
                .append("li { margin-bottom: 5px; color: #555555; }")
                .append("strong { color: #333333; font-weight: 700; }")
                .append(".footer { background-color: #f9f9f9; padding: 20px 25px; text-align: center; font-size: 12px; color: #999999; border-top: 1px solid #eeeeee; }")
                .append(".button { display: inline-block; padding: 10px 20px; margin-top: 20px; background-color: #1a73e8; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: 600; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\">")
                .append("<tr>")
                .append("<td align=\"center\" valign=\"top\">")
                .append("<table class=\"container\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\">")
                .append("<tr>")
                .append("<td class=\"header\">");

        htmlBodyBuilder.append("<img src='").append(logoDataUri).append("' ");
        htmlBodyBuilder.append(
                "alt='Company Logo' width='150' height='60' style='display: block; max-width: 150px; height: 60px; object-fit: contain; object-position: left center; margin: 0;' />");

        htmlBodyBuilder.append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<td class=\"content-area\">")
                .append("<h3>IBM i QSYSOPR Job Failure Alert</h3>") // Specific heading for QSYSOPR monitor
                .append("<p>Hi Team,</p>")
                .append("<p>This is an automated alert from Island Pacific Operations Monitor.</p>")
                .append("<p>A job failure message has been detected in the QSYSOPR message queue:</p>")
                .append("<ul>")
                .append(String.format("<li><strong>Message ID:</strong> %s</li>", escapeHtml(messageData.getMessageId())))
                .append(String.format("<li><strong>Message Text:</strong> %s</li>", escapeHtml(messageData.getMessageText())))
                .append(String.format("<li><strong>Timestamp:</strong> %s</li>", escapeHtml(messageData.getMessageTimestamp() != null ? messageData.getMessageTimestamp().toString() : "")))
                .append(String.format("<li><strong>From Job:</strong> %s</li>", escapeHtml(messageData.getFromJob())))
                .append(String.format("<li><strong>From User:</strong> %s</li>", escapeHtml(messageData.getFromUser())))
                .append("</ul>")
                .append("<p>Please investigate this issue on your IBM i system as soon as possible.</p>")
                .append("<p>Thank you,</p>")
                .append("<p>Island Pacific Retail Systems</p>")
                .append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<td class=\"footer\">")
                .append("<p>&copy; ").append(java.time.Year.now().getValue())
                .append(" Island Pacific. All rights reserved.</p>")
                .append("</td>")
                .append("</tr>")
                .append("</table>")
                .append("</td>")
                .append("</tr>")
                .append("</table>")
                .append("</body>")
                .append("</html>");
        return htmlBodyBuilder.toString();
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
                    LOGGER.warning("Could not load logo from " + logoPath + ": " + e.getMessage());
                }
            }
        }
        return DEFAULT_LOGO_BASE64;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

