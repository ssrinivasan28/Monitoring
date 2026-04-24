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
