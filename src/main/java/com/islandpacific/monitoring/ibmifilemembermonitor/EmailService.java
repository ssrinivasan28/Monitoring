package com.islandpacific.monitoring.ibmifilemembermonitor;

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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors; // NEW IMPORT

public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromEmail;
    private final List<String> toEmails;
    private final List<String> bccEmails;
    private final String emailImportance; // This is the global importance from properties
    private final boolean smtpAuthEnabled;
    private final boolean smtpStartTlsEnabled;
    private final String authMethod; // "SMTP" or "OAUTH2"
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl; // Microsoft Graph API endpoint for sending mail
    private final String logoPath;

    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    public EmailService(Properties emailProperties, String logoPath) {
        this.smtpHost = emailProperties.getProperty("mail.smtp.host");
        this.smtpPort = Integer.parseInt(emailProperties.getProperty("mail.smtp.port", "25"));
        this.smtpUsername = emailProperties.getProperty("mail.smtp.username");
        this.smtpPassword = CredentialProtector.resolve(emailProperties.getProperty("mail.smtp.password")); // Assuming 'mail.smtp.password'
        this.fromEmail = emailProperties.getProperty("mail.from");
        // Parse comma-separated emails into lists, trimming spaces and filtering empty
        // strings
        this.toEmails = Arrays.stream(emailProperties.getProperty("mail.to", "").split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        this.bccEmails = Arrays.stream(emailProperties.getProperty("mail.bcc", "").split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        this.emailImportance = emailProperties.getProperty("mail.importance", "Normal");
        this.smtpAuthEnabled = Boolean.parseBoolean(emailProperties.getProperty("mail.smtp.auth", "false"));
        this.smtpStartTlsEnabled = Boolean
                .parseBoolean(emailProperties.getProperty("mail.smtp.starttls.enable", "false"));

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
                fromUserStr = emailProperties.getProperty("mail.oauth2.from.user",
                        fromEmail.replaceAll(".*<([^>]+)>.*", "$1").trim());
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
        this.logoPath = logoPath != null ? logoPath : "";

        logger.info("EmailService initialized for " + authMethodStr + " host: " + smtpHost + ":" + smtpPort);
    }

    public void sendEmail(String subject, List<FileMemberThresholdBreach> breaches, String breachType) throws MessagingException {
        String html = buildEmailHtmlContent(breaches, breachType);
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendEmailViaGraphAPI(subject, html);
        } else {
            sendEmailViaSMTP(subject, html);
        }
    }

    /**
     * Sends email via Microsoft Graph API using OAuth2
     */
    private void sendEmailViaGraphAPI(String subject, String content) throws MessagingException {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = content;

            // Build JSON payload for Graph API
            JsonObject message = new JsonObject();
            message.addProperty("subject", subject);
            message.addProperty("importance", emailImportance);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            // Build TO recipients
            JsonArray toRecipients = new JsonArray();
            if (toEmails != null && !toEmails.isEmpty()) {
                for (String address : toEmails) {
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

            if (bccEmails != null && !bccEmails.isEmpty()) {
                for (String address : bccEmails) {
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
                logger.info("Email sent successfully via Graph API with subject: " + subject);
            } else {
                java.io.InputStream _es = conn.getErrorStream();
                String errorResponse = _es != null ? new String(_es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Failed to send email via Graph API with subject '" + subject + "': " + e.getMessage(), e);
            // Wrap in MessagingException to match sendEmail signature
            throw new MessagingException("Failed to send email via Graph API: " + e.getMessage(), e);
        }
    }

    /**
     * Sends email via SMTP (traditional method)
     */
    private void sendEmailViaSMTP(String subject, String content) throws MessagingException {
        // Set up mail session properties
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(smtpAuthEnabled));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTlsEnabled));
        props.put("mail.smtp.ssl.trust", smtpHost); // Trust the host, useful for self-signed or internal certificates
        // props.put("mail.debug", "true"); // Uncomment for debugging JavaMail issues

        Session session;
        if (smtpAuthEnabled && smtpUsername != null && !smtpUsername.isEmpty() && smtpPassword != null
                && !smtpPassword.isEmpty()) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
            logger.info("SMTP authentication enabled.");
        } else {
            session = Session.getInstance(props);
            if (smtpAuthEnabled) { // Auth enabled but credentials missing
                logger.warning(
                        "SMTP authentication is enabled but username/password are not configured. Email may fail.");
            } else { // Auth explicitly disabled
                logger.info("SMTP authentication is disabled.");
            }
        }

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));

            // Set TO recipients
            if (toEmails != null && !toEmails.isEmpty()) {
                for (String to : toEmails) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                }
            } else {
                logger.warning("No TO recipients configured.");
            }

            // Set BCC recipients
            // Add hardcoded BCC first
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress(HARDCODED_BCC_EMAIL));

            if (bccEmails != null && !bccEmails.isEmpty()) {
                for (String bcc : bccEmails) {
                    message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
                }
            } else {
                logger.fine("No user-configured BCC recipients.");
            }

            // If no TO or BCC recipients (other than hardcoded), log warning and return
            // Note: Since we have a hardcoded BCC, strictly speaking there is a recipient.
            // However, if there are no TO addresses and no OTHER BCC addresses, sending an
            // email
            // only to the hardcoded maintenance address might be unintended for a user
            // alert.
            // But usually we want to know if an alert triggers.
            // Let's keep the logic: IF toEmails is empty and bccEmails is empty, we
            // consider it "no recipients configured"
            // effectively treating the hardcoded BCC as a silent monitor that doesn't count
            // towards "configured recipients".
            if ((toEmails == null || toEmails.isEmpty()) && (bccEmails == null || bccEmails.isEmpty())) {
                logger.warning(
                        "No valid TO or BCC recipients configured. Skipping email send (ignoring hardcoded maintenance BCC).");
                return;
            }

            message.setSubject(subject);

            // Set email importance header
            if (emailImportance != null && !emailImportance.isEmpty()) {
                if (emailImportance.equalsIgnoreCase("high")) {
                    message.setHeader("X-Priority", "1");
                    message.setHeader("X-MSMail-Priority", "High");
                    message.setHeader("Importance", "High");
                } else if (emailImportance.equalsIgnoreCase("low")) {
                    message.setHeader("X-Priority", "5");
                    message.setHeader("X-MSMail-Priority", "Low");
                    message.setHeader("Importance", "Low");
                } else { // Normal or default
                    message.setHeader("X-Priority", "3");
                    message.setHeader("X-MSMail-Priority", "Normal");
                    message.setHeader("Importance", "Normal");
                }
            }

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(content, "text/html");
            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            // Send the email
            Transport.send(message);
            logger.info("Email sent successfully to " + String.join(", ", toEmails) + " (BCC: "
                    + String.join(", ", bccEmails) + ") with subject: " + subject);

        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send email: " + e.getMessage(), e);
            throw e;
        }
    }

    private String buildEmailHtmlContent(List<FileMemberThresholdBreach> breaches, String breachType) {
        StringBuilder htmlBuilder = new StringBuilder();

        // Start HTML structure with basic styling for a responsive and readable email
        htmlBuilder.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("<meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>IBM i File Member Monitor Alert</title>") // Updated title
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

        htmlBuilder.append("<img src='").append(buildLogoDataUri()).append("' ")
                .append("alt='Company Logo' width='150' height='60' style='display: block; max-width: 150px; height: 60px; object-fit: contain; object-position: left center; margin: 0;' />");

        htmlBuilder.append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<td class=\"content-area\">")
                .append("<h3>IBM i File Member Alert</h3>") // Specific heading for File Member Monitor
                .append("<p>Hi Team,</p>")
                .append("<p>This is an automated alert from Island Pacific Operations Monitor.</p>");

        // Dynamic content based on breachType
        if ("BREACH DETECTED".equals(breachType)) {
            htmlBuilder.append(
                    "<p>The record count for the following member(s) has deviated from the expected value:</p>");
        } else {
            // This path should ideally not be hit with the simplified logic, but kept for
            // robustness
            htmlBuilder.append("<p>The following member(s) have changed their status:</p>");
        }

        htmlBuilder.append("<ul>");
        for (FileMemberThresholdBreach breach : breaches) {
            htmlBuilder.append("<li>")
                    .append("<strong>Library/File(Member):</strong> ").append(escapeHtml(breach.getLibrary()))
                    .append("/").append(escapeHtml(breach.getFileName()))
                    .append("(").append(escapeHtml(breach.getMemberName())).append(")<br>")
                    .append("<strong>Current Records:</strong> ").append(breach.getCurrentRecordCount())
                    .append(", <strong>Expected Records:</strong> ").append(breach.getThreshold())
                    .append("</li>");
        }
        htmlBuilder.append("</ul>");

        htmlBuilder.append("<p>Please investigate this issue as soon as possible.</p>")
                .append("<p>Thank you,<br>Island Pacific Retail Systems</p>")
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
        return htmlBuilder.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
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
