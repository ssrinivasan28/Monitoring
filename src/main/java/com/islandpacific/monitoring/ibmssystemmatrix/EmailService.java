package com.islandpacific.monitoring.ibmssystemmatrix;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
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
    private final String authMethod;
    private final OAuth2TokenProvider oauth2TokenProvider;
    private final String graphMailUrl;

    private final String logoPath;

    public EmailService(Properties emailProps, String ibmiHost, String clientName, String logoPath) {
        this.from = emailProps.getProperty("mail.from", "");
        this.to = emailProps.getProperty("mail.to", "");
        this.bcc = emailProps.getProperty("mail.bcc", "");
        this.importance = emailProps.getProperty("mail.importance", "Normal");
        this.clientName = clientName != null ? clientName : "";
        this.authMethod = emailProps.getProperty("mail.auth.method", "SMTP").toUpperCase();
        this.logoPath = logoPath != null ? logoPath : "";

        if ("OAUTH2".equals(this.authMethod)) {
            String tenantId = emailProps.getProperty("mail.oauth2.tenant.id", "");
            String clientId = emailProps.getProperty("mail.oauth2.client.id", "");
            String clientSecret = CredentialProtector.resolve(emailProps.getProperty("mail.oauth2.client.secret", ""));
            String scope = emailProps.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
            String tokenUrl = emailProps.getProperty("mail.oauth2.token.url", "");
            this.oauth2TokenProvider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
            String fromUser = emailProps.getProperty("mail.oauth2.from.user",
                    this.from.replaceAll(".*<([^>]+)>.*", "$1").trim());
            String providedGraphUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");
            this.graphMailUrl = (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty())
                    ? providedGraphUrl.trim()
                    : "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail";
            this.host = "";
            this.port = "25";
            this.username = "";
            this.password = "";
            this.authEnabled = false;
            this.startTlsEnabled = false;
        } else {
            this.host = emailProps.getProperty("mail.smtp.host", "");
            this.port = emailProps.getProperty("mail.smtp.port", "25");
            this.username = emailProps.getProperty("mail.smtp.username", "");
            this.password = CredentialProtector.resolve(emailProps.getProperty("mail.smtp.password", ""));
            this.authEnabled = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
            this.startTlsEnabled = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "false"));
            this.oauth2TokenProvider = null;
            this.graphMailUrl = null;
        }
    }

    private String buildSubject(String ibmiHost) {
        String prefix = clientName.isEmpty() ? "" : "[" + clientName + "] ";
        return prefix + "IBM i System Alert: " + ibmiHost;
    }

    private String buildErrorSubject(String subject) {
        String prefix = clientName.isEmpty() ? "" : "[" + clientName + "] ";
        return prefix + "IBM i System Monitor Error: " + subject;
    }

    // ================== SYSTEM ALERT EMAIL ==================
    public void sendSystemAlert(IbmiSystemMonitorInfo info,
            double cpuT, double aspT, double poolT,
            long jobsT, long activeJobsT) {
        if (from == null || to == null) {
            logger.warning("Email configuration missing (from/to). Skipping alert.");
            return;
        }
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendSystemAlertViaGraphAPI(info, cpuT, aspT, poolT, jobsT, activeJobsT);
        } else {
            sendSystemAlertViaSMTP(info, cpuT, aspT, poolT, jobsT, activeJobsT);
        }
    }

    private void sendSystemAlertViaGraphAPI(IbmiSystemMonitorInfo info,
            double cpuT, double aspT, double poolT,
            long jobsT, long activeJobsT) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = buildSystemAlertHtmlContent(info, cpuT, aspT, poolT, jobsT, activeJobsT);

            JsonObject message = new JsonObject();
            message.addProperty("subject", buildSubject(info.getHost()));
            message.addProperty("importance", importance);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            JsonArray toRecipients = new JsonArray();
            if (to != null && !to.isEmpty()) {
                for (String address : to.split("[,;]")) {
                    JsonObject recipient = new JsonObject();
                    JsonObject emailAddress = new JsonObject();
                    emailAddress.addProperty("address", address.trim());
                    recipient.add("emailAddress", emailAddress);
                    toRecipients.add(recipient);
                }
            }
            message.add("toRecipients", toRecipients);

            JsonArray bccRecipients = new JsonArray();
            JsonObject hardcodedRecipient = new JsonObject();
            JsonObject hardcodedEmail = new JsonObject();
            hardcodedEmail.addProperty("address", HARDCODED_BCC_EMAIL);
            hardcodedRecipient.add("emailAddress", hardcodedEmail);
            bccRecipients.add(hardcodedRecipient);
            if (bcc != null && !bcc.isEmpty()) {
                for (String address : bcc.split("[,;]")) {
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
                logger.info("System alert email sent successfully via Graph API for " + info.getHost());
            } else {
                java.io.InputStream es = conn.getErrorStream();
                String errorResponse = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send system alert via Graph API for " + info.getHost() + ": " + e.getMessage(), e);
        }
    }

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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.replace(';', ',')));
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (bcc != null && !bcc.isEmpty()) {
                combinedBcc += "," + bcc;
            }
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(combinedBcc.replace(';', ',')));
            message.setSubject(buildSubject(info.getHost()));

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

            String htmlBody = buildSystemAlertHtmlContent(info, cpuT, aspT, poolT, jobsT, activeJobsT);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(htmlBody, "text/html");

            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            Transport.send(message);
            logger.info("System alert email sent successfully via SMTP for " + info.getHost());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send system alert email via SMTP: " + e.getMessage(), e);
        }
    }

    private String buildSystemAlertHtmlContent(IbmiSystemMonitorInfo info,
            double cpuT, double aspT, double poolT,
            long jobsT, long activeJobsT) {
        String logoDataUri = buildLogoDataUri();

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

        html.append("<img src='").append(logoDataUri).append("' alt='Company Logo' width='180' style='display:block;max-width:180px;margin:0;' />");
        html.append("")
                .append("</td></tr>")
                .append("<tr><td class='content-area'>")
                .append("<h3>IBM i System Resource Alert</h3>")
                .append("<p>Hi Team,</p>");

        if (!clientName.isEmpty()) {
            html.append("<p>Client: <strong>").append(escapeHtml(clientName)).append("</strong></p>");
        }

        html.append("<p>Automated alert from Island Pacific Operations Monitor. System metrics on <strong>")
                .append(escapeHtml(info.getHost())).append("</strong> exceeded defined thresholds.</p>")
                .append("<h4>Resource Utilization:</h4>")
                .append("<ul>")
                .append("<li>CPU Utilization: <strong style='color:").append(cpuColor).append(";'>")
                .append(String.format("%.2f", info.getCpuUtilization())).append("%</strong> (Threshold ").append(cpuT).append("%)</li>")
                .append("<li>ASP Utilization: <strong style='color:").append(aspColor).append(";'>")
                .append(String.format("%.2f", info.getAspUtilization())).append("%</strong> (Threshold ").append(aspT).append("%)</li>")
                .append("<li>Shared Pool Utilization: <strong style='color:").append(poolColor).append(";'>")
                .append(String.format("%.2f", info.getSharedPoolUtilization())).append("%</strong> (Threshold ").append(poolT).append("%)</li>")
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

    // ================== DAILY REPORT EMAIL ==================
    public void sendDailyReport(byte[] pdfBytes, String reportDate) {
        if (from == null || to == null) {
            logger.warning("Email config missing (from/to). Skipping daily report.");
            return;
        }
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendDailyReportViaGraphAPI(pdfBytes, reportDate);
        } else {
            sendDailyReportViaSMTP(pdfBytes, reportDate);
        }
    }

    private String buildReportSubject(String reportDate) {
        String prefix = clientName.isEmpty() ? "" : "[" + clientName + "] ";
        return prefix + "IBM i Daily Performance Report - " + reportDate;
    }

    private String buildReportEmailHtml(String reportDate) {
        String logoDataUri = buildLogoDataUri();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
            .append("<style>")
            .append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;font-size:14px;color:#333;background-color:#f4f4f4;margin:0;padding:0;}")
            .append(".container{max-width:600px;margin:20px auto;background:#fff;border-radius:8px;box-shadow:0 4px 10px rgba(0,0,0,0.05);overflow:hidden;}")
            .append(".header{background:#fff;padding:10px 25px;text-align:left;}")
            .append(".content-area{padding:25px;line-height:1.6;}")
            .append("h3{font-size:20px;color:#1a237e;margin:0 0 15px;}")
            .append(".footer{background:#f9f9f9;padding:15px;text-align:center;font-size:12px;color:#999;border-top:1px solid #eee;}")
            .append("</style></head><body>")
            .append("<table class='container' width='600' cellpadding='0' cellspacing='0'>")
            .append("<tr><td class='header'>");

        html.append("<img src='").append(logoDataUri).append("' alt='Island Pacific' width='180' style='display:block;max-width:180px;margin:0;'/>");

        html.append("</td></tr>")
            .append("<tr><td class='content-area'>")
            .append("<h3>IBM i Daily Performance Report</h3>")
            .append("<p>Hi Team,</p>");

        if (!clientName.isEmpty()) {
            html.append("<p>Client: <strong>").append(escapeHtml(clientName)).append("</strong></p>");
        }

        html.append("<p>Please find attached the IBM i Daily Performance Report for <strong>")
            .append(escapeHtml(reportDate)).append("</strong>.</p>")
            .append("<p>The report includes:</p>")
            .append("<ul>")
            .append("<li>CPU, ASP and Shared Pool utilization trends per server</li>")
            .append("<li>Total and Active Job counts per server</li>")
            .append("<li>Peak and trough values with timestamps</li>")
            .append("<li>Summary table across all monitored servers</li>")
            .append("</ul>")
            .append("<p>Thank you,<br/>Island Pacific Retail Systems</p>")
            .append("</td></tr>")
            .append("<tr><td class='footer'>")
            .append("&copy; ").append(java.time.Year.now().getValue())
            .append(" Island Pacific. All rights reserved.")
            .append("</td></tr></table>")
            .append("</body></html>");
        return html.toString();
    }

    private void sendDailyReportViaSMTP(byte[] pdfBytes, String reportDate) {
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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.replace(';', ',')));
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (bcc != null && !bcc.isEmpty()) combinedBcc += "," + bcc;
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(combinedBcc.replace(';', ',')));
            message.setSubject(buildReportSubject(reportDate));

            MimeBodyPart htmlPart = new MimeBodyPart();
            MimeMultipart related = new MimeMultipart("related");
            htmlPart.setContent(buildReportEmailHtml(reportDate), "text/html; charset=UTF-8");
            related.addBodyPart(htmlPart);

            MimeBodyPart relatedWrapper = new MimeBodyPart();
            relatedWrapper.setContent(related);

            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.setDataHandler(new DataHandler(new ByteArrayDataSource(pdfBytes, "application/pdf")));
            attachPart.setFileName("IBMi_DailyReport_" + reportDate.replace(" ", "_") + ".pdf");

            MimeMultipart multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(relatedWrapper);
            multipart.addBodyPart(attachPart);
            message.setContent(multipart);

            Transport.send(message);
            logger.info("Daily report sent via SMTP for " + reportDate);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send daily report via SMTP: " + e.getMessage(), e);
        }
    }

    private void sendDailyReportViaGraphAPI(byte[] pdfBytes, String reportDate) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String fileName = "IBMi_DailyReport_" + reportDate.replace(" ", "_") + ".pdf";
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

            JsonObject message = new JsonObject();
            message.addProperty("subject", buildReportSubject(reportDate));
            message.addProperty("importance", importance);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", buildReportEmailHtml(reportDate));
            message.add("body", body);

            JsonArray toRecipients = new JsonArray();
            for (String address : to.split("[,;]")) {
                JsonObject r = new JsonObject();
                JsonObject ea = new JsonObject();
                ea.addProperty("address", address.trim());
                r.add("emailAddress", ea);
                toRecipients.add(r);
            }
            message.add("toRecipients", toRecipients);

            JsonArray bccRecipients = new JsonArray();
            JsonObject hardcoded = new JsonObject();
            JsonObject hardcodedEa = new JsonObject();
            hardcodedEa.addProperty("address", HARDCODED_BCC_EMAIL);
            hardcoded.add("emailAddress", hardcodedEa);
            bccRecipients.add(hardcoded);
            if (bcc != null && !bcc.isEmpty()) {
                for (String address : bcc.split("[,;]")) {
                    JsonObject r = new JsonObject();
                    JsonObject ea = new JsonObject();
                    ea.addProperty("address", address.trim());
                    r.add("emailAddress", ea);
                    bccRecipients.add(r);
                }
            }
            message.add("bccRecipients", bccRecipients);

            JsonArray attachments = new JsonArray();
            JsonObject attachment = new JsonObject();
            attachment.addProperty("@odata.type", "#microsoft.graph.fileAttachment");
            attachment.addProperty("name", fileName);
            attachment.addProperty("contentType", "application/pdf");
            attachment.addProperty("contentBytes", pdfBase64);
            attachments.add(attachment);
            message.add("attachments", attachments);

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
                logger.info("Daily report sent via Graph API for " + reportDate);
            } else {
                java.io.InputStream es = conn.getErrorStream();
                String errorResponse = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no body)";
                throw new IOException("Graph API failed: " + responseCode + " " + errorResponse);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send daily report via Graph API: " + e.getMessage(), e);
        }
    }

    // ================== ERROR ALERT EMAIL ==================
    public void sendErrorAlert(String subject, String errorMessage) {
        if (from == null || to == null) {
            logger.warning("Email config missing (from/to). Skipping error alert.");
            return;
        }
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendErrorViaGraphAPI(subject, errorMessage);
        } else {
            sendErrorViaSMTP(subject, errorMessage);
        }
    }

    private void sendErrorViaGraphAPI(String subject, String errorMessage) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body>" +
                    "<p>An error occurred in the IBM i System Monitor:</p>" +
                    "<pre>" + escapeHtml(errorMessage) + "</pre>" +
                    "</body></html>";

            JsonObject message = new JsonObject();
            message.addProperty("subject", buildErrorSubject(subject));
            message.addProperty("importance", importance);

            JsonObject body = new JsonObject();
            body.addProperty("contentType", "HTML");
            body.addProperty("content", htmlBody);
            message.add("body", body);

            JsonArray toRecipients = new JsonArray();
            if (to != null && !to.isEmpty()) {
                for (String address : to.split("[,;]")) {
                    JsonObject recipient = new JsonObject();
                    JsonObject emailAddress = new JsonObject();
                    emailAddress.addProperty("address", address.trim());
                    recipient.add("emailAddress", emailAddress);
                    toRecipients.add(recipient);
                }
            }
            message.add("toRecipients", toRecipients);

            JsonArray bccRecipients = new JsonArray();
            JsonObject hardcodedRecipient = new JsonObject();
            JsonObject hardcodedEmail = new JsonObject();
            hardcodedEmail.addProperty("address", HARDCODED_BCC_EMAIL);
            hardcodedRecipient.add("emailAddress", hardcodedEmail);
            bccRecipients.add(hardcodedRecipient);
            if (bcc != null && !bcc.isEmpty()) {
                for (String address : bcc.split("[,;]")) {
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
                java.io.InputStream es = conn.getErrorStream();
                String errorResponse = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API request failed with code " + responseCode + ": " + errorResponse);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send error alert via Graph API: " + e.getMessage(), e);
        }
    }

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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.replace(';', ',')));
            String combinedBcc = HARDCODED_BCC_EMAIL;
            if (bcc != null && !bcc.isEmpty()) {
                combinedBcc += "," + bcc;
            }
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(combinedBcc.replace(';', ',')));
            message.setSubject(buildErrorSubject(subject));
            message.setText("An error occurred in the IBM i System Monitor:\n\n" + errorMessage);

            Transport.send(message);
            logger.info("Error alert email sent successfully via SMTP for subject: " + subject);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send error email alert via SMTP: " + e.getMessage(), e);
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
                } catch (IOException e) {
                    logger.warning("Could not load logo from " + logoPath + ": " + e.getMessage());
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

