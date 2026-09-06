package com.islandpacific.monitoring.ibmjobstatusmonitoring;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    private static final String HARDCODED_BCC_EMAIL = "ssrinivasan@islandpacific.com";

    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAAxAToDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9U6KK5Xxr8RNO8FxqkoNzfOMpaxnBx6sewrgx2Pw2W0JYrGVFCEd2/wCtX2S1ZvQoVcTUVKjG8mdVRXg1z8dtdkmLQ2tlDH2QozH8TuFdL4U+OFvqFwltrNstkznAuYiTHn/aB5X6818LhPELh/GV1h41nFvROUWk/n0+dj3a3D2YUaftHC/knd/16XPVKq3GqWVpOsE93BDM+NsckqqxzwMAmrKsHUMpBUjII71+VP8AwUSH/Gfnwo/64aN/6cZa/UaFL28uW/S58rVqeyjex+q9FFFc5sFeP/8ADV3w6/4Xt/wp/wDtO6/4Tnf5f2P7FL5W7yPP/wBbjb/q+evtXsFflt/zmV/7fP8A3D11UKUavPzdE2c9Wo6fLbq0j9SaKKK5ToCiiigAooooAKKKKACvKfjz+054B/ZstdGuPHeo3Onxau8qWhtrOS43GMKXzsBxw69a9Wr82P8AgtD/AMi58Kv+vrUv/RdvXTh6aq1VCWzMa03Tg5I/R201O2vNNgv0kC2s0SzJJJ8o2sAQTnp1FN/tnT/APn+tv8Av8v+NeMfHb/k0jWP+wJbf+0q+Fvgr8A9e+O93q0GhXml2j6akbyG+d1DBywG3arf3T1xX2+Q8M4fNMvq5hisV7GFOXKve266a3uursfKZtntfAYyng6FD2kpq61t38n2P1Ugv7a6YiG4imI7RuG/lU9fm14y/Y8+Jnwu0afxFbT2l9HYqZpX0a6kE8KryXAKqSB1O3J9q9w/Yu/aP1fx5d3HgvxTdtqGowQG4sNQlOZZkUgPG5/iYZBDdSM5zitMw4ThSwM8wyzFRxFOHxWVmvO13+mmpGD4inUxccFjsO6M5fDd3T/AAX6n1rRRRX52faBRRRQAUU13VFLMwVR1JOBQjrIoZWDKe4ORQBFfX1tplnNd3lxFa2sKl5Z53CIijqWY8Ae5rP8N+LtD8Y2j3WhaxY6xbI2x5bG4SZVb0JUnBrjf2h/hrqPxa+FGreHNJvEs7+do5YzMSI5SjhtjkdAcdcHnFeX/sf/ALO/in4M3uv6l4lnt4Hv4o4IrG1m80fKxPmORxnnAAz1NfSYfAYCplVXGVMTy1ouyp23Wmv4v0trueJWxmLhmFPDQoXpSV3Ps9f+B63PojxB4h03wpo11q2sXsOnabapvmuZ22og/wA8AdSeBXgF7+3x8MrS/a3jh128iDY+1QWSCM++GkVsf8Brz/8A4KJeLLuKPwl4aikZLKbzr+dAeJGUqkefXGXP410XwD/ZB8A6x8K9D1nxJp0usarq1ql48jXMkawq43KqBGHRSMk55z24r6jA5NlGCyinmucOcvatqMYW2V1d39O/bQ+fxeZ5lisynl+WKK9mk5OV+tu3r2Po3V/iD4Z8OtYpq+vadpM18oa3ivrpIXkB6YDEE+ldACGAIIIPIIr45/aa/ZL8Z/Ev4n/2/wCHZ7K5066t4bcxXdwYzaeWoXGMHKnG7jnJPFfVPgPw7N4R8E6Dodxdm+n06xhtHuWz+8ZEClufpXzGY4DAYbBYfEYbE89Sa96Nvh/4bbXfdaHv4LGYyviq1GvQ5IR+GX839b+WzN6ikJCgkkADuaSORJVyjq49VOa+aPcHUUUUAQ3t0tjZz3L/AHIY2kbHoBk18r6uc32v6hdalcK8kkzl2bBIX0HsAOK+ptRiSfT7qKSNpo3iZWjU4LAg5A+tcgbTVIZEjh1Gz0yNRiPTVjBjUf3WOOp71+UccZHVzx0KTqONON3aKTvJ6Ju8orRXtq27uyPq8jx0cDzy5U5O2rdtPkm/wsfPX2aT+7Tlsp3VmWJmUdSFJAr2y/8ACGjTXxkubO7spycyWtsAY2P+wewNaxstRtTFFbX1rokCj91YqgbA/wBs46nvX5BS8P6vNL21XRaLlSb+ak4KPo3zdl1Pr5cQQsuSGr7v/JO/yVvMrfBbXZtT8NSWdwxeSxcRox6mMjKj8OR9AK/On/goj/yf58KP+uGjf+nGWv028MabDY3N9ILL7FdTbDMsf+qcjOGT65NfmR/wUR/5P8+FH/XDRv8A04y1/UfCFCvhsvo4fET5pQi4311SbS31vayfmflWd1IVa86lNWTadvXf8T7X/bl+P+pfs4/s/wCp+JdDSM6/d3MWl6dLKoZIJpdxMpU8NtRHIB4JAzxmvjr4Wf8ABPf4gftGfD7SviP43+M+r2mua/bLqNpCUkuzHG43Rl3My4JBB2oAFBxmvuz9qH9n/T/2l/g9qngq9vDps8rpdWN+E3/Z7mPJRivGVOWUj0Y45r4N8O+Jv2v/ANh3Ro9BuvCkXj/wFpYIgkhha+hhhB/gliImiUdhIpC9hivp8PL91ak0p369V5Hl1l796ibj5H1V+xR8H/jb8HofE2j/ABQ8ZQ+JfD0Ewg0SJ5Wupyo5MwmY7kjIOBE2SCCRtH3vlf8A5zK/9vn/ALh6+uf2Rf27fCv7Vtre2NtYzeHPFunRCa70a4kEgaPIXzYZABvUMQDkAgkZHIJ+Rv8AnMr/ANvn/uHqqXP7Sr7RWfK/0JqcvJT5HdXR9L/8FA/2wrn9mPwTp2l+GRDL458QBxZvMgkWygXAe4KH7zZIVFPBOSchSD80eAv+Cffx3+OOkW/jL4ifFvU/Deq6gguYLO5luLu6iVuV8wCVFhOMfIudvQ4PAT9ry3j8Xf8ABT/4YaLrYEukI+jQpDL9xkM7yFcejOSDX6lVLm8NSh7Nay1bLUfbzlz7LSx+U/x18VfH79kP4U+Ivh7448UX3ifQNfgjTwz42064lW6srqOaORoHlJEiholcbWZv9liNwH1r/wAE+fFGs+K/2ONB1XW9WvtY1SR9RD3t/cvPM225lC5diScAADnjFS/8FKNG0/Vv2OfHEl8qGSxa0urVm6pMLmNQR7lXZfoxrH/4Jt/8mQ+Hf9/U/wD0qmonNVMNz2s+bX7hRi4V+W+lv1PiT9jz44/HPx7L4t+HngrxDqmreL/ETW7Jr+vX0lzBoNjF5v2idd5ba7GSJRgH2GcY9O+Jv/BNf42+H9FvPF+hfGLUPFviu1RrqS2aW5triZgMkQzGZsv6A7c+opf+CNNlC/iT4r3ZjU3EcGnxLJjkKz3BYfiVX8hX6hVricRKhWcaaS2vpvoRQpKrSTmz4f8A+CZX7V/iH45eG9f8HeNr19T8TeHVjmg1GcYmurVyVxL6ujAAt1Idc8gk8Z+3N+13471b4u2nwH+C0txD4gmeO31HUNPIFy88i7hbxP8A8sgqEM8nBHIyoVs8T/wTcgSy/bh+M9tAojgjt9TRY14AA1KMAfhR+wTbReJ/+Chfxg1jWAJNXtP7Xnt/M+8sjX6RsR7hGK/QmqlThCrOpbRK9vNkqcpU4wvu7XNfQv8Agl58X47OPXbj43z6b4wI83Fu93IqSdcG589WPPfZ+Br51/bc+IXxNm0Twv8ADH4w23m+NfCNzczRa3EQ0Wq2M6RiOXcANzBomG7AJ/iAYNn9vK/ND/gs9o9gNM+GGrBEXVGlv7UuPvPCFibB9g3/AKEfWpwuJlVrRVRX7eRdeioU24H178dv+TSNY/7Alt/7SrxH/gnN/wAhnx1/172f/oUtfZ+peHNL1jQ5NGvtPt7vSZIxC1lNGGiZBjClTxgYHHtWd4V+HfhfwNJcv4e0DTtFe5CrM1jbrEZAucBto5xk/nX0ODz2lhsixOUyg3KrJNPSys479fsni4nKalfNqGYqS5aaaa6v4v8AM6Kobu1iv7Sa2nQSQTI0ciHoykYI/I1NRXxqbTuj6Zq+jPzB1ew8U/shfHD7TbQllt5HNnJMD5OoWbH7pI9sA45VhnsK99vv+CimlHRSbHwffHWWXCxT3KeQr/7w+ZhnttGfavqnxT4N0Lxvpp0/X9JtNYsydwhvIRIFPqM9D7jmuU8Mfs8/Djwdqkeo6T4Q062vozujndDK0Z9V3k7T7jFfqlfifJs2pU6ub4SU68Fa8XZSt31TX3O3Tsfn9HIczy6pOnluJUaMnezV3H00f5o/Nfx7H4lb4nLfeLkMev6nNBqE8bDayCXayKV/hwpUbewwO1fen7YPwXvPi58OoZtGhNxr2iyNc20A+9PGRiSNf9ogKR6lcd69M134TeC/E+sNq2r+FtJ1LU227ru6tEeU7QAvzEZ4AGK6yuTNOMHi6uBxOEp8k8Onp9nXlVkv5bJr0OjAcNLD08XQxE+eFa3rpfV+d3f1Pzz/AGd/2u7j4MaI/hPxPpN1qOk2sr/Z2gIW5tCWJeMo+ARuJOMggk9e1/47ftq6n8RbO10PwLa6j4fiadHe88zbeTMDlI0EZO0ZxnBJbgcDOfsPxr8DPAXxEuzd+IPC1hf3h4a62GKZvq6FWP4mm+C/gR4A+Hl6Lzw/4WsLG9X7t0VaWVP913LMPwNejLiPhyeIeZSwMniHra65Obvv/wC2+drnGskzuNH6jHFr2O17e9btt/7d5bHj3jiHxrH+xZ4ifx/d/avEM1qsrK0SpJDGZo9iSbQAXA5Jx1OO2a43/gnJ/wAe3j3/AH7L+U1fYOt6Hp/iTS7jTdVsoNQ0+4AWa2uUDxyDIOCp4PIB/Cs7wp4A8N+BRcjw9oVhoouSpmFjbrF5m3O3dgc4yfzr59cRUpZRi8vlStKtPnXLZRjrF2S/7d0PZeS1FmWGxiqXjShyu93J6SV7/M+Mf+Cin/I6eD/+wfN/6MFfWXwH/wCSKeBf+wLaf+ilrX8VfDfwr44uIJ/EPh7TdamgUpFJfWyylFJyQCRwM1t6bptro+n21jY28dpZW0axQwQqFSNAMBVA6ADtXLj87pYzJsLlkYNSpNtvSzvfb7zfB5VUw2aYjHyknGokkuqtb/I/Pb/goB/yWyz/AAwLB/6Nmr6k+MXwkk+Mn7Pdho9nsGr21nbXtgXOA0yRAbCe25WZc+pB7V6L4m+Fng/xpqC3+veGdL1i9WMRC4vbVJXCAkhckdMk8e9dLb28dpBHBDGsUMShERBgKoGAAPTFdmK4m5sJl9LCxcamG1u9m9Pw01ObD5Fy4jGTryThX6Ldb/5n5yfs/ftJ6t+zld6n4X8RaLdXWlfaC8tkf3VzZz8Biobgg4GVOOmQeuey+N37dLeMPDE2h+CdP1DRXuwFn1O5kVJ0XPKxBGOCem7OQM4GeR9e+N/hB4L+JDrJ4k8N2OqzqNq3Uke2YD08xcNj2zWT4T/Z1+G3gjUY7/R/CNhb3sZ3Rzzbp3jPqpkZtp9xXvT4k4dxOIWZYnBSeI3aTXI5Lq9f/bfW55EcjzqhReBoYqPsdrte8l2Wn6+ljB/ZYb4g3Pw1ivfiDfS3N1cuHsYbqILcx2+ODKQASWPI3cgYyeePZKKK/M8divruJqYnkUOZ3tFWS8kv6vufdYTD/VaEKPM5cqtdu7fqFFFFcJ1hRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQB//Z";

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
            }
        }
        this.oauth2TokenProvider = provider;
        this.graphMailUrl = graphUrl;
        this.logoPath = logoPath != null ? logoPath : "";
    }

    public void sendMsgwAlert(List<JobStatusInfo> msgwJobs) {
        String from = emailProps.getProperty("mail.from");
        String to = emailProps.getProperty("mail.to");
        if (from == null || to == null) {
            logger.warning("Essential email configuration missing. Skipping alert.");
            return;
        }
        if ("OAUTH2".equals(authMethod) && oauth2TokenProvider != null && graphMailUrl != null) {
            sendViaGraphAPI(msgwJobs);
        } else {
            sendViaSMTP(msgwJobs);
        }
    }

    private String buildSubject(List<JobStatusInfo> jobs) {
        String prefix = isBlank(clientName) ? "" : "[" + clientName + "] ";
        return prefix + "IBM i MSGW Alert: " + jobs.size() + " job(s) waiting for reply on " + ibmiHost;
    }

    private void sendViaGraphAPI(List<JobStatusInfo> jobs) {
        try {
            String accessToken = oauth2TokenProvider.getAccessToken();
            String htmlBody = buildHtmlContent(jobs);

            JsonObject message = new JsonObject();
            message.addProperty("subject", buildSubject(jobs));
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

            URL url = new URL(graphMailUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("MSGW alert sent via Graph API for " + jobs.size() + " job(s).");
            } else {
                java.io.InputStream es = conn.getErrorStream();
                String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "(no error body)";
                throw new IOException("Graph API failed: " + responseCode + ": " + err);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send MSGW alert via Graph API: " + e.getMessage(), e);
        }
    }

    private void sendViaSMTP(List<JobStatusInfo> jobs) {
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
            logger.warning("Essential email configuration missing. Skipping alert.");
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
            message.setSubject(buildSubject(jobs));

            if ("High".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "1"); message.setHeader("X-MSMail-Priority", "High"); message.setHeader("Importance", "High");
            } else if ("Low".equalsIgnoreCase(importance)) {
                message.setHeader("X-Priority", "5"); message.setHeader("X-MSMail-Priority", "Low"); message.setHeader("Importance", "Low");
            } else {
                message.setHeader("X-Priority", "3"); message.setHeader("X-MSMail-Priority", "Normal"); message.setHeader("Importance", "Normal");
            }

            String htmlBody = buildHtmlContent(jobs);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html");

            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

            Transport.send(message);
            logger.info("MSGW alert sent via SMTP for " + jobs.size() + " job(s).");
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send MSGW alert via SMTP: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error sending MSGW alert: " + e.getMessage(), e);
        }
    }

    private String buildHtmlContent(List<JobStatusInfo> jobs) {
        String year = String.valueOf(java.time.Year.now().getValue());
        String logoSrc = buildLogoDataUri();
        String accent = "#c0392b";
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
          .append("body{margin:0;padding:0;background:#f0f2f5;font-family:'Segoe UI',Tahoma,Geneva,sans-serif;font-size:14px;color:#333}")
          .append(".wrap{max-width:620px;margin:30px auto}")
          .append(".card{background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)}")
          .append(".logo-bar{background:#fff;padding:16px 28px;border-bottom:3px solid ").append(accent).append("}")
          .append(".logo-bar img{display:block;max-width:150px;height:50px;object-fit:contain}")
          .append(".badge-bar{background:").append(accent).append(";padding:18px 28px}")
          .append(".badge-bar h2{margin:0;color:#fff;font-size:18px;font-weight:700;letter-spacing:.5px}")
          .append(".badge{display:inline-block;background:#fdecea;color:#c0392b;font-size:11px;font-weight:700;letter-spacing:1px;padding:3px 10px;border-radius:20px;margin-left:10px;vertical-align:middle}")
          .append(".body{padding:24px 28px}")
          .append(".intro{font-size:14px;color:#444;line-height:1.7;margin:0 0 20px}")
          .append("table.details{width:100%;border-collapse:collapse;margin-bottom:20px}")
          .append("table.details td,table.details th{padding:9px 12px;font-size:13px;border-bottom:1px solid #f0f0f0;vertical-align:top}")
          .append("table.details th{background:#f7f8fa;font-weight:600;color:#555;text-align:left}")
          .append("table.details td:first-child{font-weight:600;color:#555}")
          .append("table.details td:last-child{color:#222}")
          .append(".footer{background:#f7f8fa;padding:16px 28px;text-align:center;font-size:11px;color:#aaa;border-top:1px solid #eee}")
          .append("</style></head><body><div class='wrap'><div class='card'>")
          .append("<div class='logo-bar'><img src='").append(logoSrc).append("' alt='Island Pacific'/></div>")
          .append("<div class='badge-bar'><h2>IBM i MSGW Alert<span class='badge'>MSGW</span></h2></div>")
          .append("<div class='body'>")
          .append("<p class='intro'>The following ").append(jobs.size())
          .append(" job(s) on <strong>").append(escapeHtml(ibmiHost))
          .append("</strong> are in <strong>MSGW</strong> status and require attention.</p>");

        if (!isBlank(clientName)) {
            sb.append("<p style='font-size:13px;color:#555;margin-bottom:16px'>Client: <strong>")
              .append(escapeHtml(clientName)).append("</strong></p>");
        }

        sb.append("<table class='details'>")
          .append("<tr><th>Job</th><th>User</th><th>Subsystem</th><th>Elapsed (s)</th></tr>");

        for (JobStatusInfo job : jobs) {
            sb.append("<tr>")
              .append("<td>").append(escapeHtml(job.getJobNumber() + "/" + job.getJobName())).append("</td>")
              .append("<td>").append(escapeHtml(job.getJobUser())).append("</td>")
              .append("<td>").append(escapeHtml(job.getSubsystem())).append("</td>")
              .append("<td>").append(job.getElapsedSeconds()).append("</td>")
              .append("</tr>");
        }

        sb.append("</table>")
          .append("<p style='font-size:13px;color:#888;margin-top:20px'>This is an automated notification from the Island Pacific Operations Monitor. Please do not reply to this email.</p>")
          .append("</div>")
          .append("<div class='footer'>&copy; ").append(year).append(" Island Pacific. All rights reserved. &nbsp;|&nbsp; Operations Monitor</div>")
          .append("</div></div></body></html>");
        return sb.toString();
    }

    private String buildLogoDataUri() {
        if (!logoPath.isEmpty()) {
            java.nio.file.Path p = java.nio.file.Paths.get(logoPath);
            if (java.nio.file.Files.exists(p)) {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(p);
                    String mime = logoPath.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    return "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
                } catch (java.io.IOException e) {
                    logger.warning("Could not load logo from " + logoPath + ": " + e.getMessage());
                }
            }
        }
        return DEFAULT_LOGO_BASE64;
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
