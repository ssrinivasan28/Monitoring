package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public class AppLogger {

    private static final Logger LOGGER = Logger.getLogger(AppLogger.class.getName());
    private static FileHandler fileHandler;

    public static void setupLogger(String logFolder, String logLevel) {
        // Disable default console handler to prevent duplicate console output
        LogManager.getLogManager().reset();

        LOGGER.setLevel(Level.parse(logLevel));
        LOGGER.setUseParentHandlers(false); // Prevent logs from going to root logger's handlers

        
        File logDir = new File(logFolder);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        try {
          

            String logFilePath = logFolder + File.separator + "qsysopr_monitor.log";
            fileHandler = new FileHandler(logFilePath, true); // true for append mode
            fileHandler.setFormatter(new LogFormatter());
            LOGGER.addHandler(fileHandler);

            // Console Handler (optional, for real-time console output)
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new LogFormatter());
            LOGGER.addHandler(consoleHandler);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not set up logger file handler: " + e.getMessage(), e);
        }
    }


    private static class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("[%1$tF %1$tT] [%2$-7s] %3$s %n",
                                 new Date(record.getMillis()),
                                 record.getLevel().getName(),
                                 formatMessage(record));
        }
    }


    public static Logger getLogger() {
        return LOGGER;
    }

 
    public static void closeLogger() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
