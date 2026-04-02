package com.islandpacific.monitoring.ibmssystemmatrix;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {
    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    private final String host;
    private final String port;
    private final String from;
    private final String to;
    private final String bcc;
    private final String username;
    private final String password;
    private final boolean authEnabled;
    private final boolean startTlsEnabled;
    private final String importance;
    private final String clientName;
    private final String authMethod; // "SMTP" or "OAUTH2"
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl; // Microsoft Graph API endpoint for sending mail
    private final String fromUser; // User email for Graph API

    // Keep prefix in the string (will be stripped later in code)
    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,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";

    // Full constructor with OAuth2 support
    public EmailService(String host, String port, String from, String to, String bcc,
            String username, String password, boolean authEnabled, boolean startTlsEnabled,
            String importance, String clientName, String authMethod, OAuth2TokenProvider oauth2TokenProvider,
            String graphMailUrl, String fromUser) {
        this.host = host;
        this.port = port;
        this.from = from;
        this.to = to;
        this.bcc = bcc;
        this.username = username;
        this.password = password;
        this.authEnabled = authEnabled;
        this.startTlsEnabled = startTlsEnabled;
        this.importance = importance;
        this.clientName = clientName;
        this.authMethod = authMethod != null ? authMethod.toUpperCase() : "SMTP";
        this.oauth2TokenProvider = oauth2TokenProvider;
        this.graphMailUrl = graphMailUrl;
        this.fromUser = fromUser;
    }

    // 11-param constructor (backward compatibility)
    public EmailService(String host, String port, String from, String to, String bcc,
            String username, String password, boolean authEnabled, boolean startTlsEnabled,
            String importance, String clientName) {
        this(host, port, from, to, bcc, username, password, authEnabled, startTlsEnabled, importance, clientName,
                "SMTP", null, null, null);
    }

    // 10-param constructor (backward compatibility)
    public EmailService(String host, String port, String from, String to, String bcc,
            String username, String password, boolean authEnabled, boolean startTlsEnabled,
            String importance) {
        this(host, port, from, to, bcc, username, password, authEnabled, startTlsEnabled, importance, "", "SMTP", null,
                null, null);
    }

    // ================== SYSTEM ALERT EMAIL ==================
    public void sendSystemAlert(IbmiSystemMonitorInfo info,
            double cpuT, double aspT, double poolT,
            long jobsT, long activeJobsT) {
        if (from == null || to == null) {
            logger.warning("Email configuration missing (from/to). Skipping alert.");
            return;
        }

        // Use Microsoft Graph API for OAuth2, SMTP for traditional auth
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendSystemAlertViaGraphAPI(info, cpuT, aspT, poolT, jobsT, activeJobsT);
        } else {
            sendSystemAlertViaSMTP(info, cpuT, aspT, poolT, jobsT, activeJobsT);
        }
    }

    /**
     * Sends system alert via Microsoft Graph API using OAuth2
     */
    private void sendSystemAlertViaGraphAPI(IbmiSystemMonitorInfo info,
            double cpuT, double aspT, double poolT,
            long jobsT, long activeJobsT) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String subjectPrefix = clientName.isEmpty() ? "" : "[" + clientName + "] ";
            String subject = subjectPrefix + "IBM i System Alert: " + info.getHost();
            // For Graph API, embed logo as data URI
            String htmlBody = buildSystemAlertHtmlContent(info, cpuT, aspT, poolT, jobsT, activeJobsT, true);

            // Build JSON payload for Graph API
            JsonObject message = new JsonObject();
            message.addProperty("subject", subject);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            // Build TO recipients
            JsonArray toRecipients = new JsonArray();
            if (to != null && !to.isEmpty()) {
                String[] toAddresses = to.split(",");
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

            if (bcc != null && !bcc.isEmpty()) {
                String[] bccAddresses = bcc.split(",");
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
                logger.info("System alert email sent successfully via Graph API for " + info.getHost());
            } else {
                String errorResponse = new String(conn.getErrorStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Failed to send system alert via Graph API for " + info.getHost() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Sends system alert via SMTP (traditional method)
     */
    private void sendSystemAlertViaSMTP(IbmiSystemMonitorInfo info,
            double cpuT, double aspT, double poolT,
            long jobsT, long activeJobsT) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", String.valueOf(authEnabled));
            props.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));
            props.put("mail.smtp.ssl.trust", host);

            Session session;
            if (authEnabled && !username.isEmpty() && !password.isEmpty()) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (bcc != null && !bcc.isEmpty()) {
                combinedBcc += "," + bcc;
            }
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(combinedBcc));

            // Subject
            String subjectPrefix = clientName.isEmpty() ? "" : "[" + clientName + "] ";
            message.setSubject(subjectPrefix + "IBM i System Alert: " + info.getHost());

            // Importance headers
            if ("High".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "1");
                message.setHeader("X-MSMail-Priority", "High");
                message.setHeader("Importance", "High");
            } else if ("Low".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "5");
                message.setHeader("X-MSMail-Priority", "Low");
                message.setHeader("Importance", "Low");
            } else {
                message.setHeader("X-Priority", "3");
                message.setHeader("X-MSMail-Priority", "Normal");
                message.setHeader("Importance", "Normal");
            }

            // For SMTP, use cid:logo reference
            String htmlBody = buildSystemAlertHtmlContent(info, cpuT, aspT, poolT, jobsT, activeJobsT, false);

            // Body part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(htmlBody, "text/html");

            // Inline logo (fix applied: strip prefix safely)
            MimeBodyPart imagePart = new MimeBodyPart();
            String rawBase64 = DEFAULT_LOGO_BASE64.replaceFirst("^data:image/[^;]+;base64,", "").trim();
            byte[] imageBytes = Base64.getDecoder().decode(rawBase64);
            imagePart.setDataHandler(new DataHandler(new ByteArrayDataSource(imageBytes, "image/jpeg")));
            imagePart.setHeader("Content-ID", "<logo>");
            imagePart.setDisposition(MimeBodyPart.INLINE);

            // Multipart
            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(imagePart);

            message.setContent(multipart);

            Transport.send(message);
            logger.info("System alert email sent successfully via SMTP for " + info.getHost());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send system alert email via SMTP: " + e.getMessage(), e);
        }
    }

    private String buildSystemAlertHtmlContent(IbmiSystemMonitorInfo info,
            double cpuT, double aspT, double poolT,
            long jobsT, long activeJobsT, boolean embedLogoAsDataUri) {

        // Decide colors
        String cpuColor = info.getCpuUtilization() > cpuT ? "#dc3545" : "#28a745";
        String aspColor = info.getAspUtilization() > aspT ? "#dc3545" : "#28a745";
        String poolColor = info.getSharedPoolUtilization() > poolT ? "#dc3545" : "#28a745";
        String jobsColor = info.getTotalJobs() > jobsT ? "#dc3545" : "#28a745";
        String activeJobsColor = info.getActiveJobs() > activeJobsT ? "#dc3545" : "#28a745";

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head>")
                .append("<meta charset='utf-8'>")
                .append("<style>")
                .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 14px; color: #333; background-color: #f4f4f4; margin: 0; padding: 0; }")
                .append(".container { max-width: 600px; margin: 20px auto; background: #fff; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); overflow: hidden; }")
                .append(".header { background: #fff; padding: 10px 25px; text-align: left; height: 60px; }")
                .append(".header img { max-width: 150px; height: 60px; object-fit: contain; }")
                .append(".content-area { padding: 25px; line-height: 1.6; }")
                .append("h3 { font-size: 20px; color: #e74c3c; margin: 0 0 15px; }")
                .append("h4 { font-size: 16px; color: #34495e; margin: 20px 0 10px; border-bottom: 1px solid #eee; padding-bottom: 5px; }")
                .append("ul { margin: 10px 0 20px 20px; }")
                .append("li { margin-bottom: 5px; }")
                .append("strong { font-weight: 600; }")
                .append(".footer { background: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #999; border-top: 1px solid #eee; }")
                .append("</style>")
                .append("</head><body>")
                .append("<table class='container'>")
                .append("<tr><td class='header'>");

        // Use data URI for Graph API, or cid:logo for SMTP with MIME attachment
        if (embedLogoAsDataUri) {
            html.append("<img src='").append(DEFAULT_LOGO_BASE64).append("' ");
        } else {
            html.append("<img src='cid:logo' ");
        }
        html.append(
                "alt='Company Logo' width='150' height='60' style='display: block; max-width: 150px; height: 60px; object-fit: contain; object-position: left center; margin: 0;' />")
                .append("</td></tr>")
                .append("<tr><td class='content-area'>")
                .append("<h3>IBM i System Resource Alert</h3>")
                .append("<p>Hi Team,</p>");

        if (!clientName.isEmpty()) {
            html.append("<p>Client: <strong>").append(clientName).append("</strong></p>");
        }

        html.append("<p>Automated alert from Island Pacific Operations Monitor. System metrics on <strong>")
                .append(info.getHost()).append("</strong> exceeded defined thresholds.</p>")
                .append("<h4>Resource Utilization:</h4>")
                .append("<ul>")
                .append("<li>CPU Utilization: <strong style='color:").append(cpuColor).append(";'>")
                .append(String.format("%.2f", info.getCpuUtilization())).append("%</strong> (Threshold ")
                .append(cpuT).append("%)</li>")
                .append("<li>ASP Utilization: <strong style='color:").append(aspColor).append(";'>")
                .append(String.format("%.2f", info.getAspUtilization())).append("%</strong> (Threshold ")
                .append(aspT).append("%)</li>")
                .append("<li>Shared Pool Utilization: <strong style='color:").append(poolColor).append(";'>")
                .append(String.format("%.2f", info.getSharedPoolUtilization())).append("%</strong> (Threshold ")
                .append(poolT).append("%)</li>")
                .append("<li>Total Jobs: <strong style='color:").append(jobsColor).append(";'>")
                .append(info.getTotalJobs()).append("</strong> (Threshold ").append(jobsT).append(")</li>")
                .append("<li>Active Jobs: <strong style='color:").append(activeJobsColor).append(";'>")
                .append(info.getActiveJobs()).append("</strong> (Threshold ").append(activeJobsT).append(")</li>")
                .append("</ul>")
                .append("<p>Please investigate the system promptly.</p>")
                .append("<p>Thank you,<br/>Island Pacific Retail Systems</p>")
                .append("</td></tr>")
                .append("<tr><td class='footer'>")
                .append("&copy; ").append(java.time.Year.now().getValue())
                .append(" Island Pacific. All rights reserved.")
                .append("</td></tr></table>")
                .append("</body></html>");

        return html.toString();
    }

    // ================== ERROR ALERT EMAIL ==================
    public void sendErrorAlert(String subject, String errorMessage) {
        if (from == null || to == null) {
            logger.warning("Email config missing (from/to). Skipping error alert.");
            return;
        }

        // Use Microsoft Graph API for OAuth2, SMTP for traditional auth
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendErrorViaGraphAPI(subject, errorMessage);
        } else {
            sendErrorViaSMTP(subject, errorMessage);
        }
    }

    /**
     * Sends error alert via Microsoft Graph API using OAuth2
     */
    private void sendErrorViaGraphAPI(String subject, String errorMessage) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String subjectPrefix = clientName.isEmpty() ? "" : "[" + clientName + "] ";
            String fullSubject = subjectPrefix + "IBM i System Monitor Error: " + subject;
            String htmlBody = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body>" +
                    "<p>An error occurred in the IBM i System Monitor:</p>" +
                    "<pre>" + errorMessage.replace("<", "&lt;").replace(">", "&gt;") + "</pre>" +
                    "<p>Host: " + (from != null ? from : "Unknown") + "</p>" +
                    "</body></html>";

            JsonObject message = new JsonObject();
            message.addProperty("subject", fullSubject);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            JsonArray toRecipients = new JsonArray();
            if (to != null && !to.isEmpty()) {
                String[] toAddresses = to.split(",");
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

            if (bcc != null && !bcc.isEmpty()) {
                String[] bccAddresses = bcc.split(",");
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
                logger.info("Error alert email sent successfully via Graph API for subject: " + subject);
            } else {
                String errorResponse = new String(conn.getErrorStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send error alert via Graph API: " + e.getMessage(), e);
        }
    }

    /**
     * Sends error alert via SMTP (traditional method)
     */
    private void sendErrorViaSMTP(String subject, String errorMessage) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", String.valueOf(authEnabled));
            props.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));
            props.put("mail.smtp.ssl.trust", host);

            Session session;
            if (authEnabled && !username.isEmpty() && !password.isEmpty()) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (bcc != null && !bcc.isEmpty()) {
                combinedBcc += "," + bcc;
            }
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(combinedBcc));

            String subjectPrefix = clientName.isEmpty() ? "" : "[" + clientName + "] ";
            message.setSubject(subjectPrefix + "IBM i System Monitor Error: " + subject);

            message.setText("An error occurred in the IBM i System Monitor:\n\n" +
                    errorMessage + "\n\nHost: " + (from != null ? from : "Unknown"));

            Transport.send(message);
            logger.info("Error alert email sent successfully via SMTP for subject: " + subject);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send error email alert via SMTP: " + e.getMessage(), e);
        }
    }
}
