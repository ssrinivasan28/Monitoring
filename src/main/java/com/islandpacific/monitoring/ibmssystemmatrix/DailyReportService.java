package com.islandpacific.monitoring.ibmssystemmatrix;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DailyReportService {

    private static final Logger logger = Logger.getLogger(DailyReportService.class.getName());

    private static final Color COLOR_CPU   = new Color(0x2196F3);
    private static final Color COLOR_ASP   = new Color(0xFF9800);
    private static final Color COLOR_POOL  = new Color(0x9C27B0);
    private static final Color COLOR_JOBS  = new Color(0x4CAF50);
    private static final Color COLOR_ACTIVE= new Color(0xF44336);

    private final DailyMetricStore store;
    private final String clientName;
    private final String logoPath;

    public DailyReportService(DailyMetricStore store, String clientName, String logoPath) {
        this.store = store;
        this.clientName = clientName == null ? "" : clientName;
        this.logoPath = logoPath == null ? "" : logoPath;
    }

    public byte[] generatePdfReport() throws Exception {
        List<MetricRecord> records = store.readLast24Hours();
        if (records.isEmpty()) {
            logger.warning("No metric data available for daily report.");
            return generateEmptyReportPdf();
        }

        // Group by host
        Map<String, List<MetricRecord>> byHost = records.stream()
                .collect(Collectors.groupingBy(r -> r.host, LinkedHashMap::new, Collectors.toList()));

        StringBuilder html = new StringBuilder();
        buildHtmlHeader(html);

        String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String logoDataUri = loadLogoDataUri();
        if (logoDataUri != null) {
            html.append("<p class='logo'><img src='").append(logoDataUri).append("' width='180'/></p>");
        }
        html.append("<div class='report-title'>IBM i Daily Performance Report</div>");
        if (!clientName.isEmpty()) {
            html.append("<div class='client-name'>").append(escapeHtml(clientName)).append("</div>");
        }
        html.append("<div class='report-date'>Report Date: ").append(reportDate).append("</div>");
        html.append("<div class='report-period'>Data covers the last 24 hours</div>");

        // Overall summary table
        html.append("<h2>Summary &mdash; All Servers</h2>");
        html.append("<table class='summary-table'>");
        html.append("<tr><th>Server</th><th>CPU% Avg</th><th>CPU% Peak</th><th>ASP% Avg</th>")
            .append("<th>ASP% Peak</th><th>Pool% Avg</th><th>Pool% Peak</th>")
            .append("<th>Max Total Jobs</th><th>Max Active Jobs</th></tr>");

        for (Map.Entry<String, List<MetricRecord>> entry : byHost.entrySet()) {
            String host = entry.getKey();
            List<MetricRecord> recs = entry.getValue();
            html.append("<tr>")
                .append("<td>").append(escapeHtml(host)).append("</td>")
                .append("<td>").append(fmt2(avg(recs, "cpu"))).append("%</td>")
                .append("<td>").append(fmt2(max(recs, "cpu"))).append("%</td>")
                .append("<td>").append(fmt2(avg(recs, "asp"))).append("%</td>")
                .append("<td>").append(fmt2(max(recs, "asp"))).append("%</td>")
                .append("<td>").append(fmt2(avg(recs, "pool"))).append("%</td>")
                .append("<td>").append(fmt2(max(recs, "pool"))).append("%</td>")
                .append("<td>").append(maxLong(recs, "totalJobs")).append("</td>")
                .append("<td>").append(maxLong(recs, "activeJobs")).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");

        // Per-host sections
        for (Map.Entry<String, List<MetricRecord>> entry : byHost.entrySet()) {
            String host = entry.getKey();
            List<MetricRecord> recs = entry.getValue();
            recs.sort(Comparator.comparing(r -> r.timestamp));

            html.append("<div class='host-section'>");
            html.append("<h2>").append(escapeHtml(host)).append("</h2>");

            // Peak/trough table
            html.append("<h3>Peak &amp; Trough Values</h3>");
            html.append("<table class='detail-table'>");
            html.append("<tr><th>Metric</th><th>Minimum</th><th>At Time</th><th>Maximum</th><th>At Time</th></tr>");
            appendPeakRow(html, recs, "CPU Utilization (%)", "cpu");
            appendPeakRow(html, recs, "ASP Utilization (%)", "asp");
            appendPeakRow(html, recs, "Shared Pool Util (%)", "pool");
            appendPeakRowLong(html, recs, "Total Jobs", "totalJobs");
            appendPeakRowLong(html, recs, "Active Jobs", "activeJobs");
            html.append("</table>");

            // Charts
            String cpuChart   = encodeChart(buildDoubleChart(recs, "CPU Utilization (%)", "cpu", COLOR_CPU), 680, 220);
            String aspChart   = encodeChart(buildDoubleChart(recs, "ASP Utilization (%)", "asp", COLOR_ASP), 680, 220);
            String poolChart  = encodeChart(buildDoubleChart(recs, "Shared Pool Util (%)", "pool", COLOR_POOL), 680, 220);
            String totalChart = encodeChart(buildLongSingleChart(recs, "Total Jobs", "totalJobs", COLOR_JOBS), 680, 200);
            String activeChart= encodeChart(buildLongSingleChart(recs, "Active Jobs", "activeJobs", COLOR_ACTIVE), 680, 200);

            html.append("<div class='chart-block'><h3>CPU Utilization &mdash; Last 24h</h3>")
                .append("<img src='data:image/png;base64,").append(cpuChart).append("' width='680' height='220'/></div>");
            html.append("<div class='chart-block'><h3>ASP Utilization &mdash; Last 24h</h3>")
                .append("<img src='data:image/png;base64,").append(aspChart).append("' width='680' height='220'/></div>");
            html.append("<div class='chart-block'><h3>Shared Pool Utilization &mdash; Last 24h</h3>")
                .append("<img src='data:image/png;base64,").append(poolChart).append("' width='680' height='220'/></div>");
            html.append("<div class='chart-block'><h3>Total Jobs &mdash; Last 24h</h3>")
                .append("<img src='data:image/png;base64,").append(totalChart).append("' width='680' height='200'/></div>");
            html.append("<div class='chart-block'><h3>Active Jobs &mdash; Last 24h</h3>")
                .append("<img src='data:image/png;base64,").append(activeChart).append("' width='680' height='200'/></div>");

            html.append("</div>");
        }

        buildHtmlFooter(html);
        return renderToPdf(html.toString());
    }

    private void buildHtmlHeader(StringBuilder html) {
        html.append("<?xml version='1.0' encoding='UTF-8'?>")
            .append("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>")
            .append("<html xmlns='http://www.w3.org/1999/xhtml'><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>")
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

    private JFreeChart buildDoubleChart(List<MetricRecord> recs, String title, String metric, Color color) {
        TimeSeries series = new TimeSeries(metric);
        for (MetricRecord r : recs) {
            double val = getDouble(r, metric);
            series.addOrUpdate(new Millisecond(Date.from(r.timestamp)), val);
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Time", "%", dataset, false, false, false);
        styleChart(chart, color);
        return chart;
    }

    private JFreeChart buildLongSingleChart(List<MetricRecord> recs, String title, String metric, Color color) {
        TimeSeries series = new TimeSeries(title);
        for (MetricRecord r : recs) {
            series.addOrUpdate(new Millisecond(Date.from(r.timestamp)), getLong(r, metric));
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Time", "Count", dataset, false, false, false);
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        styleChartBase(chart);
        return chart;
    }

    private void styleChart(JFreeChart chart, Color lineColor) {
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, lineColor);
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
            + "<html xmlns='http://www.w3.org/1999/xhtml'><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/></head>"
            + "<body><p>No metric data available for the last 24 hours.</p></body></html>";
        return renderToPdf(html);
    }

    private void appendPeakRow(StringBuilder html, List<MetricRecord> recs, String label, String metric) {
        MetricRecord minRec = recs.stream().min(Comparator.comparingDouble(r -> getDouble(r, metric))).orElse(null);
        MetricRecord maxRec = recs.stream().max(Comparator.comparingDouble(r -> getDouble(r, metric))).orElse(null);
        if (minRec == null || maxRec == null) return;
        html.append("<tr>")
            .append("<td>").append(label).append("</td>")
            .append("<td>").append(fmt2(getDouble(minRec, metric))).append("%</td>")
            .append("<td>").append(fmtTime(minRec.timestamp)).append("</td>")
            .append("<td><strong>").append(fmt2(getDouble(maxRec, metric))).append("%</strong></td>")
            .append("<td>").append(fmtTime(maxRec.timestamp)).append("</td>")
            .append("</tr>");
    }

    private void appendPeakRowLong(StringBuilder html, List<MetricRecord> recs, String label, String metric) {
        MetricRecord minRec = recs.stream().min(Comparator.comparingLong(r -> getLong(r, metric))).orElse(null);
        MetricRecord maxRec = recs.stream().max(Comparator.comparingLong(r -> getLong(r, metric))).orElse(null);
        if (minRec == null || maxRec == null) return;
        html.append("<tr>")
            .append("<td>").append(label).append("</td>")
            .append("<td>").append(getLong(minRec, metric)).append("</td>")
            .append("<td>").append(fmtTime(minRec.timestamp)).append("</td>")
            .append("<td><strong>").append(getLong(maxRec, metric)).append("</strong></td>")
            .append("<td>").append(fmtTime(maxRec.timestamp)).append("</td>")
            .append("</tr>");
    }

    private double getDouble(MetricRecord r, String metric) {
        switch (metric) {
            case "cpu":  return r.cpu;
            case "asp":  return r.asp;
            case "pool": return r.pool;
            default:     return 0;
        }
    }

    private long getLong(MetricRecord r, String metric) {
        switch (metric) {
            case "totalJobs":  return r.totalJobs;
            case "activeJobs": return r.activeJobs;
            default:           return 0;
        }
    }

    private double avg(List<MetricRecord> recs, String metric) {
        return recs.stream().mapToDouble(r -> getDouble(r, metric)).average().orElse(0);
    }

    private double max(List<MetricRecord> recs, String metric) {
        return recs.stream().mapToDouble(r -> getDouble(r, metric)).max().orElse(0);
    }

    private long maxLong(List<MetricRecord> recs, String metric) {
        return recs.stream().mapToLong(r -> getLong(r, metric)).max().orElse(0);
    }

    private String fmt2(double v) {
        return String.format("%.2f", v);
    }

    private String fmtTime(Instant ts) {
        return DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(ts);
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

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
