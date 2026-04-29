package com.islandpacific.monitoring.sharefilemonitoring;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShareFileMonitorService {

    private final int alertWindowSize;

    private final Logger logger;
    private final EmailService emailService;
    private final String ftpHost;
    private final String ftpUsername;
    private final String ftpPassword;

    private final ConcurrentHashMap<String, Integer> tooFewBreachCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> tooManyBreachCount = new ConcurrentHashMap<>();

    public ShareFileMonitorService(Logger logger, EmailService emailService,
                                   String ftpHost, String ftpUsername, String ftpPassword, int alertWindowSize) {
        this.logger = logger;
        this.emailService = emailService;
        this.ftpHost = ftpHost;
        this.ftpUsername = ftpUsername;
        this.ftpPassword = ftpPassword;
        this.alertWindowSize = alertWindowSize;
    }

    public void monitorFolder(ShareFileMonitorConfig.FolderConfig config) {
        String name = config.getName();
        String remotePath = config.getRemotePath();
        int min = config.getMinFiles();
        int max = config.getMaxFiles();

        logger.info("Checking ShareFile folder '" + name + "' at path '" + remotePath + "' min=" + min + " max=" + max);

        int count;
        try {
            count = listFileCount(remotePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "FTPS error for folder '" + name + "': " + e.getMessage(), e);
            ShareFileMetrics.setFileCount(name, -1);
            return;
        }

        ShareFileMetrics.setFileCount(name, count);
        logger.info("Folder '" + name + "' file count: " + count);

        evaluateThreshold(config, count, name, remotePath, min, max);
    }

    private int listFileCount(String remotePath) throws IOException {
        // Implicit FTPS (SSL from the start, port 990)
        FTPSClient ftps = new FTPSClient(true);
        ftps.setConnectTimeout(15000);
        try {
            ftps.connect(ftpHost, 990);
            int reply = ftps.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTPS server refused connection: " + reply);
            }

            ftps.execPBSZ(0);
            ftps.execPROT("P");

            if (!ftps.login(ftpUsername, ftpPassword)) {
                throw new IOException("FTPS login failed for user: " + ftpUsername);
            }

            ftps.setFileType(FTP.BINARY_FILE_TYPE);
            ftps.enterLocalPassiveMode();

            FTPFile[] files = ftps.listFiles(remotePath);
            if (files == null) {
                return 0;
            }
            int count = 0;
            for (FTPFile f : files) {
                if (f.isFile()) count++;
            }
            return count;
        } finally {
            try {
                if (ftps.isConnected()) {
                    ftps.logout();
                    ftps.disconnect();
                }
            } catch (IOException ignored) {}
        }
    }

    void evaluateThreshold(ShareFileMonitorConfig.FolderConfig config,
                                   int count, String name, String remotePath, int min, int max) {
        if (count < min) {
            if (count == 0 && config.isIgnoreZeroFileAlert()) {
                tooFewBreachCount.put(name, 0);
                logger.info("Folder '" + name + "' is empty but zero-file alert is suppressed.");
                return;
            }
            int n = tooFewBreachCount.getOrDefault(name, 0) + 1;
            tooFewBreachCount.put(name, n);
            tooManyBreachCount.put(name, 0);
            logger.warning("Folder '" + name + "' too few files: " + count + " < " + min + " (breach " + n + "/" + alertWindowSize + ")");
            if (n == alertWindowSize) {
                String subject = "[" + config.getClientName() + "] ShareFile Too Few Files - " + name;
                String status = count + " file(s) detected - below minimum threshold of " + min;
                emailService.sendEmail(name, remotePath, subject, status, "High");
                ShareFileMetrics.incrementTooFewAlert(name);
            }
        } else if (count > max) {
            int n = tooManyBreachCount.getOrDefault(name, 0) + 1;
            tooManyBreachCount.put(name, n);
            tooFewBreachCount.put(name, 0);
            logger.warning("Folder '" + name + "' too many files: " + count + " > " + max + " (breach " + n + "/" + alertWindowSize + ")");
            if (n == alertWindowSize) {
                String subject = "[" + config.getClientName() + "] ShareFile Too Many Files - " + name;
                String status = count + " file(s) detected - exceeds maximum threshold of " + max;
                emailService.sendEmail(name, remotePath, subject, status, "Normal");
                ShareFileMetrics.incrementTooManyAlert(name);
            }
        } else {
            tooFewBreachCount.put(name, 0);
            tooManyBreachCount.put(name, 0);
            logger.info("Folder '" + name + "' file count " + count + " is within range [" + min + ", " + max + "].");
        }
    }
}
