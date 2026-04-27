package com.islandpacific.monitoring.ibmsubsystemmonitoring;

import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.*;

public class SubsystemMonitorTest {

    @Test
    public void config_trimsCriticalSubsystemNamesAndDefaultsPorts() {
        Properties appProps = new Properties();
        appProps.setProperty("ibmi.host", "host-a");
        appProps.setProperty("ibmi.user", "user-a");
        appProps.setProperty("ibmi.password", "secret");
        appProps.setProperty("ibmi.system.context.library", " QSYS ");
        appProps.setProperty("subsystem.critical.names", " QBASE, ,QBATCH ");

        Properties emailProps = new Properties();
        emailProps.setProperty("mail.smtp.host", "smtp.example.com");
        emailProps.setProperty("mail.from", "from@example.com");
        emailProps.setProperty("mail.to", "to@example.com");

        SubsystemMonitorConfig config = SubsystemMonitorConfig.fromProperties(appProps, emailProps);

        assertEquals(config.getCriticalSubsystemNames(), List.of("QBASE", "QBATCH"));
        assertEquals(config.getIbmiSystemContextLibrary(), "QSYS");
        assertEquals(config.getMonitorIntervalMs(), 60000);
        assertEquals(config.getMetricsPort(), 9400);
    }

    @Test
    public void metrics_escapesPrometheusLabelValues() throws Exception {
        SubsystemMetricsExporter exporter = new SubsystemMetricsExporter(0);
        exporter.updateMetrics(List.of(new SubsystemInfo(
                "Q\"BASE",
                "Description with \\ slash\nnext",
                "IN\"ACTIVE",
                "QSYS")));

        String metrics = invokeGenerateMetrics(exporter);

        assertTrue(metrics.contains("subsystem_name=\"Q\\\"BASE\""));
        assertTrue(metrics.contains("status_text=\"IN\\\"ACTIVE\""));
        assertTrue(metrics.contains("description=\"Description with \\\\ slash\\nnext\""));
    }

    @Test
    public void emailHtml_escapesSubsystemValues() throws Exception {
        EmailService service = new EmailService("smtp.example.com", "25", "from@example.com", "to@example.com", "",
                "", "", false, false, "Normal", "SYS&1", "Client<script>", "SMTP", null, null, null);
        SubsystemInfo info = new SubsystemInfo("Q<script>", "A&B <desc>", "END<ING>", "QSYS\"LIB");

        String html = invokeBuildSubsystemAlertHtmlContent(service, info, false);

        assertTrue(html.contains("Client&lt;script&gt;"));
        assertTrue(html.contains("SYS&amp;1"));
        assertTrue(html.contains("Q&lt;script&gt;"));
        assertTrue(html.contains("A&amp;B &lt;desc&gt;"));
        assertTrue(html.contains("QSYS&quot;LIB"));
        assertTrue(html.contains("END&lt;ING&gt;"));
        assertFalse(html.contains("Q<script>"));
    }

    private String invokeGenerateMetrics(SubsystemMetricsExporter exporter) throws Exception {
        Method method = SubsystemMetricsExporter.class.getDeclaredMethod("generateMetrics");
        method.setAccessible(true);
        return (String) method.invoke(exporter);
    }

    private String invokeBuildSubsystemAlertHtmlContent(EmailService service, SubsystemInfo info,
            boolean embedLogoAsDataUri) throws Exception {
        Method method = EmailService.class.getDeclaredMethod("buildSubsystemAlertHtmlContent",
                SubsystemInfo.class, boolean.class);
        method.setAccessible(true);
        return (String) method.invoke(service, info, embedLogoAsDataUri);
    }
}
