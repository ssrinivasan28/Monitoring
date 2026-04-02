package com.islandpacific.monitoring.ibmnetworkenabler;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class LogManager {

    // The logger instance for the application.
    private static final Logger logger = Logger.getLogger(MainNetworkEnabler.class.getName());

    // Directory where log files will be stored.
    private static final String LOG_DIRECTORY = "logs";

    // Static initializer block to set up the logger when the class is loaded.
    static {
        setupLogger();
    }


    private static void setupLogger() {
        try {
            // Create the log directory if it doesn't exist.
            File logDir = new File(LOG_DIRECTORY);
            if (!logDir.exists()) {
                logDir.mkdirs(); // Create the directory and any necessary but nonexistent parent directories.
            }

            // Define the daily log file name format (e.g., application_2025-07-03.log).
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String logFileName = LOG_DIRECTORY + File.separator + "application_" + dateFormat.format(new Date()) + ".log";

            // Remove any existing handlers to prevent duplicate logging.
            // This is important if the logger is configured elsewhere or if this method
            // is called multiple times.
            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }

            // Create a FileHandler to write logs to the daily file.
            // 'true' means append to the file if it exists, 'false' would overwrite.
            // For daily logs, we typically want to append if the app restarts on the same day.
            FileHandler fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new SimpleFormatter()); // Use SimpleFormatter for human-readable output.
            logger.addHandler(fileHandler); // Add the file handler to the logger.

            // Create a ConsoleHandler to output logs to the console as well.
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter()); // Use SimpleFormatter for console output.
            logger.addHandler(consoleHandler); // Add the console handler to the logger.

            // Set the logging level for the logger. FINEST will log everything.
            // INFO will log INFO, WARNING, SEVERE.
            logger.setLevel(Level.INFO); // Set the default logging level.

            // Prevent the console handler from propagating logs to its parent handler (which is usually the root logger)
            // to avoid duplicate console output if the root logger also has a console handler.
            logger.setUseParentHandlers(false);

            logger.info("Logger initialized. Logs will be written to: " + logFileName);

        } catch (IOException e) {
            // If there's an error setting up the logger, print to standard error.
            System.err.println("Error setting up logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logs an informational message.
     * @param message The message to log.
     */
    public static void info(String message) {
        logger.info(message);
    }

    /**
     * Logs a warning message.
     * @param message The message to log.
     */
    public static void warning(String message) {
        logger.warning(message);
    }

  
    public static void severe(String message) {
        logger.severe(message);
    }


    public static void severe(String message, Throwable thrown) {
        logger.log(Level.SEVERE, message, thrown);
    }


    public static void fine(String message) {
        logger.fine(message);
    }

  
}
