package com.islandpacific.monitoring.ibmierrormonitoring;

import javax.activation.DataHandler;
import javax.activation.DataSource;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set; // Explicitly import Set

public class EmailService {

    private final Properties emailProps;
    private final Logger logger;
    private final String clientName; // Field to store the client name
    private final String authMethod; // "SMTP" or "OAUTH2"
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl; // Microsoft Graph API endpoint for sending mail
    private final String fromUser; // User email for Graph API
    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    public EmailService(Properties emailProps, String clientName, Logger logger) {
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
            String clientSecret = emailProps.getProperty("mail.oauth2.client.secret");
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
        this.fromUser = fromUserStr;
    }

    public void sendEmail(IFSErrorMonitorConfig.MonitoringConfig config,
            Map<String, List<String>> newFilesForLocation) {
        String locationName = config.getName();
        String folderPath = config.getPathString();
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
    private void sendEmailViaGraphAPI(IFSErrorMonitorConfig.MonitoringConfig config,
            Map<String, List<String>> newFilesForLocation) {
        String locationName = config.getName();
        String folderPath = config.getPathString();

        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String subject = String.format("[%s] New Files Detected in %s", clientName, locationName);
            // For Graph API, embed logo as data URI
            String htmlBody = buildEmailHtmlContent(locationName, folderPath, newFilesForLocation, true);

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
                for (String recipient : globalTo.split(",")) {
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
                for (String address : globalBcc.split(",")) {
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
                String errorResponse = new String(conn.getErrorStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Failed to send email via Graph API for location '%s': %s",
                    locationName, e.getMessage()), e);
        }
    }

    /**
     * Sends email via SMTP (traditional method)
     */
    private void sendEmailViaSMTP(IFSErrorMonitorConfig.MonitoringConfig config,
            Map<String, List<String>> newFilesForLocation) {
        String locationName = config.getName();
        String folderPath = config.getPathString();

        String host = emailProps.getProperty("mail.smtp.host");
        String port = emailProps.getProperty("mail.smtp.port", "25");
        String from = emailProps.getProperty("mail.from");
        String globalTo = emailProps.getProperty("mail.to");
        String globalBcc = emailProps.getProperty("mail.bcc");
        final String username = emailProps.getProperty("mail.smtp.username");
        final String password = emailProps.getProperty("mail.smtp.password");

        if (host == null || from == null) {
            logger.warning(String.format(
                    "Essential email configuration (mail.smtp.host or mail.from) is missing. Skipping email for location '%s'.",
                    locationName));
            return;
        }

        Address[] toAddresses = null;
        Address[] bccAddresses = null;

        try {
            // Determine TO recipients, respecting skipToEmails configuration
            Set<String> skipToEmailsForLocation = config.getSkipToEmails();
            String actualToRecipients;
            if (skipToEmailsForLocation != null && !skipToEmailsForLocation.isEmpty()) {

                if (globalTo != null && !globalTo.trim().isEmpty()) {
                    List<String> effectiveToRecipients = new java.util.ArrayList<>();
                    for (String recipient : globalTo.split(",")) {
                        String trimmedRecipient = recipient.trim();
                        if (!trimmedRecipient.isEmpty() && !skipToEmailsForLocation.contains(trimmedRecipient)) {
                            effectiveToRecipients.add(trimmedRecipient);
                        } else if (skipToEmailsForLocation.contains(trimmedRecipient)) {
                            logger.info(String.format(
                                    "Primary TO recipient '%s' is skipped for location '%s' as per configuration.",
                                    trimmedRecipient, locationName));
                        }
                    }
                    if (!effectiveToRecipients.isEmpty()) {
                        toAddresses = InternetAddress.parse(String.join(",", effectiveToRecipients));
                    }
                }
            } else {
                // No skip emails configured for this location, use globalTo as is
                if (globalTo != null && !globalTo.trim().isEmpty()) {
                    toAddresses = InternetAddress.parse(globalTo);
                }
            }

            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (globalBcc != null && !globalBcc.trim().isEmpty()) {
                combinedBcc += "," + globalBcc;
            }
            bccAddresses = InternetAddress.parse(combinedBcc);

            if ((toAddresses == null || toAddresses.length == 0)
                    && (bccAddresses == null || bccAddresses.length == 0)) {
                logger.warning(String.format(
                        "No valid TO or BCC recipients configured for email for location '%s'. Skipping email.",
                        locationName));
                return;
            }

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            boolean authEnabled = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
            props.put("mail.smtp.auth", String.valueOf(authEnabled));
            props.put("mail.smtp.starttls.enable", emailProps.getProperty("mail.smtp.starttls.enable", "false"));
            props.put("mail.smtp.ssl.trust", host); // Trust the host for SSL/TLS

            Session mailSession;
            if (authEnabled && username != null && password != null) {
                mailSession = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
                logger.info(String.format("SMTP authentication enabled for email to location '%s'.", locationName));
            } else {
                mailSession = Session.getInstance(props);
                if (authEnabled) {
                    logger.warning(String.format(
                            "SMTP authentication is enabled but username/password are not configured for location '%s'. Email may fail.",
                            locationName));
                } else {
                    logger.info(
                            String.format("SMTP authentication is disabled for email to location '%s'.", locationName));
                }
            }

            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));

            if (toAddresses != null && toAddresses.length > 0) {
                message.setRecipients(Message.RecipientType.TO, toAddresses);
            }
            if (bccAddresses != null && bccAddresses.length > 0) {
                message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            }

            // Construct the email subject with clientName at the beginning in square
            // brackets
            String subject = String.format("[%s] Island Pacific Error Monitor Alert: New Files Detected in %s",
                    this.clientName, locationName);
            message.setSubject(subject);

            String importance = config.getEmailImportance();
            if ("High".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "1");
                message.setHeader("X-MSMail-Priority", "High");
                message.setHeader("Importance", "High");
                logger.info(String.format("Email importance set to High for location '%s'.", locationName));
            } else if ("Low".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "5");
                message.setHeader("X-MSMail-Priority", "Low");
                message.setHeader("Importance", "Low");
                logger.info(String.format("Email importance set to Low for location '%s'.", locationName));
            } else {
                message.setHeader("X-Priority", "3");
                message.setHeader("X-MSMail-Priority", "Normal");
                message.setHeader("Importance", "Normal");
                logger.info(String.format("Email importance set to Normal (default) for location '%s'.", locationName));
            }

            // For SMTP, use cid:logo reference
            String htmlBody = buildEmailHtmlContent(locationName, folderPath, newFilesForLocation, false);

            MimeMultipart multipart = new MimeMultipart("related");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html");
            multipart.addBodyPart(htmlPart);

            String logoBase64 = emailProps.getProperty("mail.logo.base64", DEFAULT_LOGO_BASE64);
            if (logoBase64 != null && !logoBase64.isEmpty()) {
                try {
                    String[] parts = logoBase64.split(",", 2);
                    if (parts.length == 2) {
                        String mimeType = parts[0].substring(parts[0].indexOf(":") + 1, parts[0].indexOf(";"));
                        byte[] imageBytes = Base64.getDecoder().decode(parts[1]);

                        MimeBodyPart imagePart = new MimeBodyPart();
                        DataSource ds = new ByteArrayDataSource(imageBytes, mimeType);
                        imagePart.setDataHandler(new DataHandler(ds));
                        imagePart.setHeader("Content-ID", "<logo>");
                        imagePart.setFileName("logo.jpg"); // Or derive from mimeType
                        imagePart.setDisposition(MimeBodyPart.INLINE);
                        multipart.addBodyPart(imagePart);
                    } else {
                        logger.warning("Invalid base64 logo format. Expected 'data:image/jpeg;base64,...'");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to embed logo in email: " + e.getMessage(), e);
                }
            } else {
                logger.info("No logo specified in email.properties or it's empty. Using default or no logo.");
            }

            message.setContent(multipart);

            Transport.send(message);
            logger.info(String.format("Email alert sent successfully for location '%s'.", locationName));

        } catch (MessagingException e) {
            logger.log(Level.SEVERE, String.format("Failed to send email alert for location '%s'. Error: %s",
                    locationName, e.getMessage()), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    String.format(
                            "An unexpected error occurred while preparing/sending email for location '%s'. Error: %s",
                            locationName, e.getMessage()),
                    e);
        }
    }

    private String buildEmailHtmlContent(String locationName, String folderPath,
            Map<String, List<String>> newFilesForLocation, boolean embedLogoAsDataUri) {
        StringBuilder htmlBodyBuilder = new StringBuilder();

        htmlBodyBuilder.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("<meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>File Monitor Alert</title>")
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

        // Use data URI for Graph API, or cid:logo for SMTP with MIME attachment
        if (embedLogoAsDataUri) {
            htmlBodyBuilder.append("<img src='").append(DEFAULT_LOGO_BASE64).append("' ");
        } else {
            htmlBodyBuilder.append("<img src='cid:logo' ");
        }
        htmlBodyBuilder.append(
                "alt='Company Logo' width='150' height='60' style='display: block; max-width: 150px; height: 60px; object-fit: contain; object-position: left center; margin: 0;' />");

        htmlBodyBuilder.append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<td class=\"content-area\">")
                .append("<h3>New Error Files Detected</h3>")
                .append("<p>Hi Team,</p>")
                .append("<p>This is an automated alert from Island Pacific Operations Monitor. New Error files have been detected in the following location:</p>")
                .append("<h4>Location: <strong>").append(locationName).append("</strong></h4>")
                .append("<p>Folder path: <strong>").append(folderPath).append("</strong></p>");

        for (Map.Entry<String, List<String>> entry : newFilesForLocation.entrySet()) {
            String fileExtension = entry.getKey();
            List<String> files = entry.getValue();
            htmlBodyBuilder.append("<h4>New ").append(fileExtension.substring(1).toUpperCase()).append(" Files:</h4>")
                    .append("<ul>");
            for (String file : files) {
                htmlBodyBuilder.append("<li>").append(file).append("</li>");
            }
            htmlBodyBuilder.append("</ul>");
        }

        htmlBodyBuilder.append("<p>Please investigate these files as soon as possible.</p>")
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
}
