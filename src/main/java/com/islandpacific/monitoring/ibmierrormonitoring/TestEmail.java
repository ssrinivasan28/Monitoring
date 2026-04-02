package com.islandpacific.monitoring.ibmierrormonitoring;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class TestEmail {

    public static void main(String[] args) {
        // SMTP configuration
        String smtpHost = "islandpacific-com.mail.protection.outlook.com";
        String smtpPort = "25";
        String from = "donotreply@islandpacific.com";
        String to = "ssrinivasan@islandpacific.com,sujitsrinivasan@gmail.com";
      String bccList = "sujitsrinivasan@gmail.com,ssrinivasan@islandpacific.com";

        // Set properties
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");

        // Create session (no authentication)
        Session session = Session.getInstance(props, null);

        try {
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccList));
            message.setSubject("Test Email from Java");
            message.setText("Hello,\n\nThis is a test email sent via Java using SMTP relay.\n\nRegards,\nTest");

            // Set importance header
            message.setHeader("Importance", "Normal");

            // Send email
            Transport.send(message);

            System.out.println("✅ Email sent successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
