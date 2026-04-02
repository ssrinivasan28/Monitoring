package com.islandpacific.monitoring.userprofilechecker;

import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class LoggerUtil {
    private static final String LOG_DIR = "logs";
    private static final String LOG_PREFIX = "userprofilelogs-";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static Logger logger;

    public static Logger getLogger() {
        if (logger == null) {
            try {
                // Ensure logs directory exists
                Path logPath = Paths.get(LOG_DIR);
                if (!Files.exists(logPath)) {
                    Files.createDirectories(logPath);
                }

                // Create log file with current date
                String logFileName = LOG_DIR + "/" + LOG_PREFIX + DATE_FORMAT.format(new Date()) + ".log";

                logger = Logger.getLogger("UserProfileLogger");
                logger.setUseParentHandlers(false); // disable default console

                // File Handler (append mode)
                FileHandler fileHandler = new FileHandler(logFileName, true);
                fileHandler.setFormatter(new SimpleFormatter() {
                    private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

                    @Override
                    public synchronized String format(LogRecord lr) {
                        return String.format(format,
                                new Date(lr.getMillis()),
                                lr.getLevel().getLocalizedName(),
                                lr.getMessage());
                    }
                });

                // Console output also
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setFormatter(new SimpleFormatter());

                logger.addHandler(fileHandler);
                logger.addHandler(consoleHandler);
                logger.setLevel(Level.INFO);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logger;
    }
}
