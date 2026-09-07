package com.islandpacific.monitoring.windowsmonitoring;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WinDailyReportService {

    private static final Logger logger = Logger.getLogger(WinDailyReportService.class.getName());

    private static final Color COLOR_CPU    = new Color(0x2196F3);
    private static final Color COLOR_MEM    = new Color(0xFF9800);
    private static final Color COLOR_DISK   = new Color(0x9C27B0);

    private final WinDailyMetricStore store;
    private final String clientName;
    private final String logoPath;

    public WinDailyReportService(WinDailyMetricStore store, String clientName, String logoPath) {
        this.store = store;
        this.clientName = clientName == null ? "" : clientName;
        this.logoPath = logoPath == null ? "" : logoPath;
    }

    public byte[] generatePdfReport() throws Exception {
        List<WinMetricRecord> records = store.readLast24Hours();
        if (records.isEmpty()) {
            logger.warning("No Windows metric data available for daily report.");
            return generateEmptyReportPdf();
        }

        Map<String, List<WinMetricRecord>> byHost = records.stream()
                .collect(Collectors.groupingBy(r -> r.host, LinkedHashMap::new, Collectors.toList()));

        StringBuilder html = new StringBuilder();
        buildHtmlHeader(html);

        String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String logoDataUri = loadLogoDataUri();
        if (logoDataUri != null) {
            html.append("<p class='logo'><img src='").append(logoDataUri).append("' width='180'/></p>");
        }
        html.append("<div class='report-title'>Windows Daily Performance Report</div>");
        if (!clientName.isEmpty()) {
            html.append("<div class='client-name'>").append(esc(clientName)).append("</div>");
        }
        html.append("<div class='report-date'>Report Date: ").append(reportDate).append("</div>");
        html.append("<div class='report-period'>Data covers the last 24 hours</div>");

        // Summary table
        html.append("<h2>Summary &mdash; All Servers</h2>");
        html.append("<table class='summary-table'>");
        html.append("<tr><th>Server</th><th>CPU% Avg</th><th>CPU% Peak</th>")
            .append("<th>Mem% Avg</th><th>Mem% Peak</th><th>Uptime (h)</th></tr>");
        for (Map.Entry<String, List<WinMetricRecord>> e : byHost.entrySet()) {
            List<WinMetricRecord> recs = e.getValue();
            html.append("<tr>")
                .append("<td>").append(esc(e.getKey())).append("</td>")
                .append("<td>").append(fmt2(avg(recs, "cpu"))).append("%</td>")
                .append("<td>").append(fmt2(max(recs, "cpu"))).append("%</td>")
                .append("<td>").append(fmt2(avg(recs, "mem"))).append("%</td>")
                .append("<td>").append(fmt2(max(recs, "mem"))).append("%</td>")
                .append("<td>").append(fmt2(max(recs, "uptime"))).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");

        // Per-host sections
        for (Map.Entry<String, List<WinMetricRecord>> e : byHost.entrySet()) {
            String host = e.getKey();
            List<WinMetricRecord> recs = e.getValue();
            recs.sort(Comparator.comparing(r -> r.timestamp));

            html.append("<div class='host-section'>");
            html.append("<h2>").append(esc(host)).append("</h2>");

            // Peak/trough table
            html.append("<h3>Peak &amp; Trough</h3>");
            html.append("<table class='detail-table'>");
            html.append("<tr><th>Metric</th><th>Minimum</th><th>At</th><th>Maximum</th><th>At</th></tr>");
            appendPeakRow(html, recs, "CPU Utilization (%)", "cpu");
            appendPeakRow(html, recs, "Memory Utilization (%)", "mem");
            appendPeakRow(html, recs, "Uptime (hours)", "uptime");
            html.append("</table>");

            // CPU chart
            html.append("<div class='chart-block'><h3>CPU Utilization &mdash; Last 24h</h3>")
                .append("<img src='data:image/png;base64,")
                .append(encodeChart(buildScalarChart(recs, "CPU Utilization (%)", "cpu", COLOR_CPU), 680, 220))
                .append("' width='680' height='220'/></div>");

            // Memory chart
            html.append("<div class='chart-block'><h3>Memory Utilization &mdash; Last 24h</h3>")
                .append("<img src='data:image/png;base64,")
                .append(encodeChart(buildScalarChart(recs, "Memory Utilization (%)", "mem", COLOR_MEM), 680, 220))
                .append("' width='680' height='220'/></div>");

            // Disk table (latest snapshot per drive)
            WinMetricRecord latest = recs.get(recs.size() - 1);
            if (!latest.diskInfo.isEmpty()) {
                html.append("<h3>Disk Usage (latest snapshot)</h3>");
                html.append("<table class='detail-table'>");
                html.append("<tr><th>Drive</th><th>Total GB</th><th>Used GB</th><th>Free GB</th><th>Usage %</th></tr>");
                latest.diskInfo.forEach((drive, s) ->
                    html.append("<tr><td>").append(esc(drive)).append("</td>")
                        .append("<td>").append(fmt2(s.totalGB)).append("</td>")
                        .append("<td>").append(fmt2(s.usedGB)).append("</td>")
                        .append("<td>").append(fmt2(s.freeGB)).append("</td>")
                        .append("<td>").append(fmt2(s.pct)).append("%</td></tr>"));
                html.append("</table>");

                // Disk trend chart for each drive
                Set<String> drives = collectDrives(recs);
                if (!drives.isEmpty()) {
                    html.append("<div class='chart-block'><h3>Disk Utilization &mdash; Last 24h</h3>")
                        .append("<img src='data:image/png;base64,")
                        .append(encodeChart(buildDiskChart(recs, drives), 680, 240))
                        .append("' width='680' height='240'/></div>");
                }
            }

            // Network table (latest snapshot)
            if (!latest.netBytesReceived.isEmpty()) {
                html.append("<h3>Network Adapters (latest snapshot)</h3>");
                html.append("<table class='detail-table'>");
                html.append("<tr><th>Adapter</th><th>Bytes Received</th><th>Bytes Sent</th></tr>");
                latest.netBytesReceived.forEach((adapter, rx) -> {
                    long tx = latest.netBytesSent.getOrDefault(adapter, 0L);
                    html.append("<tr><td>").append(esc(adapter)).append("</td>")
                        .append("<td>").append(fmtBytes(rx)).append("</td>")
                        .append("<td>").append(fmtBytes(tx)).append("</td></tr>");
                });
                html.append("</table>");
            }

            // Top processes table (latest snapshot)
            if (!latest.topProcesses.isEmpty()) {
                html.append("<h3>Top Processes by CPU (latest snapshot)</h3>");
                html.append("<table class='detail-table'>");
                html.append("<tr><th>Process</th><th>CPU %</th></tr>");
                latest.topProcesses.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(pe ->
                        html.append("<tr><td>").append(esc(pe.getKey())).append("</td>")
                            .append("<td>").append(fmt2(pe.getValue())).append("%</td></tr>"));
                html.append("</table>");
            }

            // Uptime
            html.append("<p style='font-size:10px;color:#555;margin-top:8px'>System uptime at report time: <strong>")
                .append(fmt2(latest.uptimeHours)).append(" hours</strong></p>");

            html.append("</div>"); // host-section
        }

        buildHtmlFooter(html);
        return renderToPdf(html.toString());
    }

    // --- Chart builders ---

    private JFreeChart buildScalarChart(List<WinMetricRecord> recs, String title, String metric, Color color) {
        TimeSeries series = new TimeSeries(title);
        for (WinMetricRecord r : recs) {
            series.addOrUpdate(new Millisecond(Date.from(r.timestamp)), getScalar(r, metric));
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Time",
                "uptime".equals(metric) ? "Hours" : "%",
                new TimeSeriesCollection(series), false, false, false);
        styleChart(chart, color);
        return chart;
    }

    private JFreeChart buildDiskChart(List<WinMetricRecord> recs, Set<String> drives) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        Color[] palette = {COLOR_DISK, new Color(0x00BCD4), new Color(0xFF5722), new Color(0x8BC34A), new Color(0x607D8B)};
        for (String drive : drives) {
            TimeSeries series = new TimeSeries(drive);
            for (WinMetricRecord r : recs) {
                WinMetricRecord.DiskSnapshot s = r.diskInfo.get(drive);
                if (s != null) series.addOrUpdate(new Millisecond(Date.from(r.timestamp)), s.pct);
            }
            dataset.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Disk Usage (%)", "Time", "%",
                dataset, drives.size() > 1, false, false);
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        for (int i = 0; i < drives.size(); i++) {
            renderer.setSeriesPaint(i, palette[i % palette.length]);
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesStroke(i, new BasicStroke(1.5f));
        }
        styleChartBase(chart);
        return chart;
    }

    private void styleChart(JFreeChart chart, Color color) {
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        styleChartBase(chart);
    }

    private void styleChartBase(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(new Color(0xFAFAFA));
        plot.setDomainGridlinePaint(new Color(0xE0E0E0));
        plot.setRangeGridlinePaint(new Color(0xE0E0E0));
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        axis.setLabelFont(new Font("Arial", Font.PLAIN, 9));
        axis.setTickLabelFont(new Font("Arial", Font.PLAIN, 8));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.PLAIN, 9));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 8));
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 11));
    }

    private String encodeChart(JFreeChart chart, int width, int height) throws IOException {
        BufferedImage img = chart.createBufferedImage(width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // --- HTML helpers ---

    private void buildHtmlHeader(StringBuilder html) {
        html.append("<?xml version='1.0' encoding='UTF-8'?>")
            .append("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>")
            .append("<html xmlns='http://www.w3.org/1999/xhtml'><head>")
            .append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;font-size:11px;color:#333;margin:20px;}")
            .append(".report-title{font-size:20px;font-weight:bold;color:#1a237e;margin-bottom:4px;}")
            .append(".client-name{font-size:14px;color:#555;margin-bottom:2px;}")
            .append(".report-date{font-size:12px;color:#777;}")
            .append(".report-period{font-size:11px;color:#999;margin-bottom:20px;}")
            .append("h2{font-size:14px;color:#1a237e;border-bottom:2px solid #1a237e;padding-bottom:3px;margin-top:24px;}")
            .append("h3{font-size:12px;color:#37474f;margin-top:14px;margin-bottom:4px;}")
            .append(".chart-block{page-break-inside:avoid;}")
            .append(".host-section{page-break-before:always;}")
            .append(".host-section:first-of-type{page-break-before:auto;}")
            .append("table{border-collapse:collapse;width:100%;margin-bottom:10px;}")
            .append("th{background:#1a237e;color:#fff;padding:5px 8px;font-size:10px;text-align:left;}")
            .append("td{padding:4px 8px;border-bottom:1px solid #e0e0e0;font-size:10px;}")
            .append("tr:nth-child(even) td{background:#f5f5f5;}")
            .append(".summary-table th,.summary-table td{text-align:center;}")
            .append(".summary-table td:first-child{text-align:left;font-weight:bold;}")
            .append("img{display:block;margin:6px 0;}")
            .append(".footer{font-size:9px;color:#aaa;text-align:center;margin-top:30px;border-top:1px solid #eee;padding-top:8px;}")
            .append(".logo{margin-bottom:12px;}")
            .append("</style></head><body>");
    }

    private void buildHtmlFooter(StringBuilder html) {
        html.append("<div class='footer'>Generated by Island Pacific Operations Monitor &copy; ")
            .append(java.time.Year.now().getValue())
            .append("</div></body></html>");
    }

    private void appendPeakRow(StringBuilder html, List<WinMetricRecord> recs, String label, String metric) {
        WinMetricRecord minRec = recs.stream().min(Comparator.comparingDouble(r -> getScalar(r, metric))).orElse(null);
        WinMetricRecord maxRec = recs.stream().max(Comparator.comparingDouble(r -> getScalar(r, metric))).orElse(null);
        if (minRec == null || maxRec == null) return;
        html.append("<tr>")
            .append("<td>").append(label).append("</td>")
            .append("<td>").append(fmt2(getScalar(minRec, metric))).append("</td>")
            .append("<td>").append(fmtTime(minRec.timestamp)).append("</td>")
            .append("<td><strong>").append(fmt2(getScalar(maxRec, metric))).append("</strong></td>")
            .append("<td>").append(fmtTime(maxRec.timestamp)).append("</td>")
            .append("</tr>");
    }

    // --- Metric accessors ---

    private double getScalar(WinMetricRecord r, String metric) {
        switch (metric) {
            case "cpu":    return r.cpu;
            case "mem":    return r.memoryPercent;
            case "uptime": return r.uptimeHours;
            default:       return 0;
        }
    }

    private double avg(List<WinMetricRecord> recs, String metric) {
        return recs.stream().mapToDouble(r -> getScalar(r, metric)).average().orElse(0);
    }

    private double max(List<WinMetricRecord> recs, String metric) {
        return recs.stream().mapToDouble(r -> getScalar(r, metric)).max().orElse(0);
    }

    private Set<String> collectDrives(List<WinMetricRecord> recs) {
        Set<String> drives = new LinkedHashSet<>();
        recs.forEach(r -> drives.addAll(r.diskInfo.keySet()));
        return drives;
    }

    // --- Formatting ---

    private String fmt2(double v) { return String.format("%.2f", v); }

    private String fmtTime(Instant ts) {
        return DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(ts);
    }

    private String fmtBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String loadLogoDataUri() {
        if (logoPath == null || logoPath.isEmpty()) return null;
        java.nio.file.Path p = java.nio.file.Paths.get(logoPath);
        if (!java.nio.file.Files.exists(p)) return null;
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(p);
            String mime = logoPath.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            logger.warning("Could not load logo: " + e.getMessage());
            return null;
        }
    }

    private byte[] renderToPdf(String html) throws Exception {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.createPDF(out);
        return out.toByteArray();
    }

    private byte[] generateEmptyReportPdf() throws Exception {
        String html = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>"
            + "<html xmlns='http://www.w3.org/1999/xhtml'><head>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/></head>"
            + "<body><p>No Windows metric data available for the last 24 hours.</p></body></html>";
        return renderToPdf(html);
    }
}
