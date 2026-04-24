package com.islandpacific.monitoring.servicescheduler;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class ScreenshotService {

    private static final Logger logger = AppLogger.getLogger();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String screenshotFolder;

    public ScreenshotService(String screenshotFolder) {
        this.screenshotFolder = screenshotFolder;
        // Setup deferred to first capture — avoids blocking startup if Chrome isn't needed
    }

    /**
     * Opens url in headless Chrome, takes a full-page screenshot, saves it to disk.
     *
     * @return Path to the saved screenshot file, or null on failure
     */
    public Path capture(String jobLabel, String url) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(30));
            logger.info("Capturing screenshot of: " + url);
            driver.get(url);

            // Brief wait for page JS to settle
            Thread.sleep(3000);

            File tmpFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            Path folder = Paths.get(screenshotFolder);
            Files.createDirectories(folder);

            String filename = sanitize(jobLabel) + "_" + LocalDateTime.now().format(TIMESTAMP) + ".png";
            Path dest = folder.resolve(filename);
            Files.copy(tmpFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Screenshot saved: " + dest.toAbsolutePath());
            return dest;

        } catch (IOException | InterruptedException e) {
            logger.severe("Screenshot failed for [" + jobLabel + "]: " + e.getMessage());
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return null;
        } finally {
            driver.quit();
        }
    }

    private static String sanitize(String label) {
        return label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
