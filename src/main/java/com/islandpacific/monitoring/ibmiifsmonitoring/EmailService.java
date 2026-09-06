package com.islandpacific.monitoring.ibmiifsmonitoring;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    private final Properties emailProperties;
    private final String globalEmailImportance;
    private final String authMethod;
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl;
    private final String logoPath;

    public EmailService(Properties emailProperties, String logoPath) {
        this.logoPath = logoPath != null ? logoPath : "";
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
                logger.info("OAuth2 authentication configured for email service.");
            }
        }

        this.oauth2TokenProvider = provider;
        this.graphMailUrl = graphUrl;
    }

    public void sendEmail(String locationName, String folderPath, String subject, String bodyContent,
            String emailImportance) {
        String from = emailProperties.getProperty("mail.from");

        // Validate essential email configuration
        if (from == null) {
            logger.warning(String.format(
                    "Essential email configuration (mail.from) is missing. Skipping email for location '%s'.",
                    locationName));
            return;
        }

        // Use Microsoft Graph API for OAuth2, SMTP for traditional auth
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendEmailViaGraphAPI(locationName, folderPath, subject, bodyContent, emailImportance);
        } else {
            sendEmailViaSMTP(locationName, folderPath, subject, bodyContent, emailImportance);
        }
    }

    /**
     * Sends email via Microsoft Graph API using OAuth2
     */
    private void sendEmailViaGraphAPI(String locationName, String folderPath, String subject, String bodyContent,
            String emailImportance) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = buildEmailHtmlContent(locationName, folderPath, subject, bodyContent);

            // Build JSON payload for Graph API
            String finalImportance = emailImportance != null && !emailImportance.trim().isEmpty()
                    ? emailImportance : this.globalEmailImportance;

            JsonObject message = new JsonObject();
            message.addProperty("subject", subject);
            message.addProperty("importance",
                    "High".equalsIgnoreCase(finalImportance) ? "high"
                    : "Low".equalsIgnoreCase(finalImportance) ? "low" : "normal");

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
                logger.info(String.format("Email sent successfully via Graph API for location '%s'.", locationName));
            } else {
                java.io.InputStream _es = conn.getErrorStream();
                String errorResponse = _es != null ? new String(_es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
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
    private void sendEmailViaSMTP(String locationName, String folderPath, String subject, String bodyContent,
            String emailImportance) {
        String host = emailProperties.getProperty("mail.smtp.host");
        String port = emailProperties.getProperty("mail.smtp.port", "25");
        String from = emailProperties.getProperty("mail.from");
        String globalTo = emailProperties.getProperty("mail.to");
        String globalBcc = emailProperties.getProperty("mail.bcc");
        final String username = emailProperties.getProperty("mail.smtp.username");
        final String password = CredentialProtector.resolve(emailProperties.getProperty("mail.smtp.password"));

        // Validate essential email configuration
        if (host == null || from == null) {
            logger.warning(String.format(
                    "Essential email configuration (mail.smtp.host or mail.from) is missing. Skipping email for location '%s'.",
                    locationName));
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
                logger.warning(String.format(
                        "No valid TO or BCC recipients configured for email for location '%s'. Skipping email.",
                        locationName));
                return;
            }

            // Set up mail session properties
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            boolean authEnabled = Boolean.parseBoolean(emailProperties.getProperty("mail.smtp.auth", "false"));
            props.put("mail.smtp.auth", String.valueOf(authEnabled));
            props.put("mail.smtp.starttls.enable", emailProperties.getProperty("mail.smtp.starttls.enable", "false"));
            props.put("mail.smtp.ssl.trust", host); // Trust the host, useful for self-signed or internal certificates

            // Create mail session with or without authentication
            Session session;
            if (authEnabled && username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
                logger.info(String.format("SMTP authentication enabled for email to location '%s'.", locationName));
            } else {
                session = Session.getInstance(props);
                if (authEnabled) { // Auth enabled but credentials missing
                    logger.warning(String.format(
                            "SMTP authentication is enabled but username/password are not configured for location '%s'. Email may fail.",
                            locationName));
                } else { // Auth explicitly disabled
                    logger.info(
                            String.format("SMTP authentication is disabled for email to location '%s'.", locationName));
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

            // Set email subject directly from the parameter (no appending here)
            message.setSubject(subject);

            // Determine email importance: use location-specific if provided, else global,
            // else Normal
            String finalImportance = emailImportance != null && !emailImportance.trim().isEmpty() ? emailImportance
                    : this.globalEmailImportance;

            // Set email importance header
            if ("High".equalsIgnoreCase(finalImportance)) {
                message.setHeader("X-Priority", "1");
                message.setHeader("X-MSMail-Priority", "High");
                message.setHeader("Importance", "High");
                logger.info(String.format("Email importance set to High for location '%s'.", locationName));
            } else if ("Low".equalsIgnoreCase(finalImportance)) {
                message.setHeader("X-Priority", "5");
                message.setHeader("X-MSMail-Priority", "Low");
                message.setHeader("Importance", "Low");
                logger.info(String.format("Email importance set to Low for location '%s'.", locationName));
            } else { // Default to Normal
                message.setHeader("X-Priority", "3");
                message.setHeader("X-MSMail-Priority", "Normal");
                message.setHeader("Importance", "Normal");
                logger.info(String.format("Email importance set to Normal (default) for location '%s'.", locationName));
            }

            MimeMultipart multipart = new MimeMultipart("related");
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(buildEmailHtmlContent(locationName, folderPath, subject, bodyContent), "text/html");
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            // Send the email
            Transport.send(message);
            logger.info(String.format("Email alert sent successfully for location '%s' to %s.", locationName,
                    (toAddresses != null ? Arrays.toString(toAddresses) : "N/A") + " (BCC: "
                            + (bccAddresses != null ? Arrays.toString(bccAddresses) : "N/A") + ")"));

        } catch (MessagingException e) {
            logger.log(Level.SEVERE,
                    String.format("Failed to send email for location '%s': %s", locationName, e.getMessage()), e);
        } catch (IllegalArgumentException e) {
            // This catch block specifically logs issues with Base64 decoding.
            logger.log(Level.SEVERE, String.format(
                    "Base64 decoding error for logo for location '%s': %s.",
                    locationName, e.getMessage()), e);
        } catch (Exception e) { // Catch any other unexpected exceptions during email sending
            logger.log(Level.SEVERE,
                    String.format("An unexpected error occurred during email sending for location '%s': %s",
                            locationName, e.getMessage()),
                    e);
        }
    }

    private String buildEmailHtmlContent(String locationName, String folderPath, String subject, String content) {
        StringBuilder htmlBodyBuilder = new StringBuilder();

        htmlBodyBuilder.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("<meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>IFS Monitor Alert</title>")
                .append("<style>")
                .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 14px; color: #333333; background-color: #f4f4f4; margin: 0; padding: 0; -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }")
                .append("table { border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; }")
                .append("img { border: 0; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; }")
                .append("a { text-decoration: none; color: #1a73e8; }")
                .append(".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); overflow: hidden; }")
                .append(".header { background-color: #ffffff; padding: 10px 25px; text-align: left; height: 60px; }") // Reverted
                                                                                                                      // header
                                                                                                                      // style
                                                                                                                      // for
                                                                                                                      // logo
                .append(".header img { display: block; max-width: 150px; height: 100%; object-fit: contain; object-position: left center; margin: 0; }") // Logo
                                                                                                                                                         // specific
                                                                                                                                                         // CSS
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

        htmlBodyBuilder.append("<img src='").append(buildLogoDataUri()).append("' ")
                .append("alt='Company Logo' width='150' height='60' style='display: block; max-width: 150px; height: 60px; object-fit: contain; object-position: left center; margin: 0;' />");

        htmlBodyBuilder.append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<td class=\"content-area\">")
                .append("<h3>IFS Folder Alert</h3>") // Generic heading for IFS monitor
                .append("<p>Hi Team,</p>")
                .append(String.format(
                        "<p>This is an automated alert from the Island Pacific IFS Folder Monitor regarding location: <strong>%s</strong>.</p>",
                        locationName))
                // Removed the redundant folder path line here
                .append("<p>").append(content).append("</p>") // Insert dynamic content here
                .append("<p>Please investigate this folder as soon as possible.</p>")
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
                } catch (IOException e) {
                    logger.warning("Could not load logo from " + logoPath + ": " + e.getMessage());
                }
            }
        }
        return DEFAULT_LOGO_BASE64;
    }
}
