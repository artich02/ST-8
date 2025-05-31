package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.File;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    
    private static final long TIMEOUT_MS = 30000;
    private static final long POLL_INTERVAL_MS = 100;
    
    public static void main(String[] args) {
        initializeDriver();
    }
    
    private static void initializeDriver() {
        ChromeDriverService service = ChromeDriverService.createDefaultService();
        configureSystemProperties();
        
        Path tempDownloadPath = createTempDirectory();
        Path outputDirectory = setupOutputDirectory();
        Path inputDataFile = getInputDataPath();
        
        if (!validateInputFile(inputDataFile)) {
            return;
        }
        
        ChromeDriver driver = null;
        try {
            driver = new ChromeDriver(getBrowserOptions(tempDownloadPath));
            processCDCaseCreation(driver, inputDataFile, tempDownloadPath, outputDirectory);
        } catch (Exception e) {
            handleError("Ошибка в основном процессе", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
            cleanupTempFiles(tempDownloadPath);
        }
    }
    
    private static void configureSystemProperties() {
        System.setProperty("webdriver.chrome.driver", "D:\\WORK ARTEM\\QA_UNN\\chromedriver-win64\\chromedriver.exe");
        System.setProperty("webdriver.chrome.whitelistedIps", "");
        System.setProperty("webdriver.chrome.silentOutput", "true");
    }
    
    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("selenium_downloads_");
        } catch (Exception e) {
            handleError("Не удалось создать временную директорию", e);
            return null;
        }
    }
    
    private static Path setupOutputDirectory() {
        Path dir = Paths.get("result");
        try {
            return Files.createDirectories(dir);
        } catch (Exception e) {
            handleError("Не удалось создать выходную директорию", e);
            return null;
        }
    }
    
    private static Path getInputDataPath() {
        return Paths.get("data", "data.txt");
    }
    
    private static boolean validateInputFile(Path file) {
        if (file == null || !Files.exists(file)) {
            System.err.println("Файл с данными отсутствует: " + file);
            return false;
        }
        return true;
    }
    
    private static ChromeOptions getBrowserOptions(Path downloadPath) {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> preferences = new HashMap<>();
        
        preferences.put("download.default_directory", downloadPath.toString());
        preferences.put("download.prompt_for_download", Boolean.FALSE);
        preferences.put("plugins.always_open_pdf_externally", Boolean.TRUE);
        
        options.setExperimentalOption("prefs", preferences);
        return options;
    }
    
    private static void processCDCaseCreation(WebDriver driver, Path dataFile, 
                                            Path downloadDir, Path outputDir) {
        try {
            List<String> trackData = Files.readAllLines(dataFile);
            if (trackData.size() < 4) {
                System.err.println("Недостаточно данных в файле");
                return;
            }
            
            driver.get("https://www.papercdcase.com/index.php");
            
            fillFormFields(driver, trackData);
            submitForm(driver);
            
            Path pdfFile = waitForFileDownload(downloadDir);
            if (pdfFile != null) {
                saveResultFile(pdfFile, outputDir);
            }
        } catch (Exception e) {
            handleError("Ошибка при создании CD-обложки", e);
        }
    }
    
    private static void fillFormFields(WebDriver driver, List<String> data) {
        WebElement artistField = driver.findElement(
            By.xpath("//form//tr[1]//input"));
        artistField.sendKeys(data.get(0));
        
        WebElement albumField = driver.findElement(
            By.xpath("//form//tr[2]//input"));
        albumField.sendKeys(data.get(1));
        
        for (int i = 0; i < data.size() - 3; i++) {
            int column = i / 8 + 1;
            int row = i % 8 + 1;
            String xpath = String.format(
                "//form//tr[3]//td[%d]//tr[%d]//input", column, row);
            driver.findElement(By.xpath(xpath)).sendKeys(data.get(i + 3));
        }
        
        setRadioOption(driver, 4, 2);
        setRadioOption(driver, 5, 2);
    }
    
    private static void setRadioOption(WebDriver driver, int row, int option) {
        WebElement radio = driver.findElement(
            By.xpath(String.format("//form//tr[%d]//input[%d]", row, option)));
        if (!radio.isSelected()) {
            radio.click();
        }
    }
    
    private static void submitForm(WebDriver driver) {
        driver.findElement(By.xpath("//form//p//input")).click();
    }
    
    private static Path waitForFileDownload(Path dir) {
        long endTime = System.currentTimeMillis() + TIMEOUT_MS;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                Optional<Path> pdfFile = Files.list(dir)
                    .filter(p -> p.toString().endsWith(".pdf"))
                    .findFirst();
                
                if (pdfFile.isPresent()) {
                    return pdfFile.get();
                }
                
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            } catch (Exception e) {
                handleError("Ошибка при проверке файлов", e);
            }
        }
        
        System.err.println("PDF не был загружен за отведённое время");
        return null;
    }
    
    private static void saveResultFile(Path source, Path targetDir) {
        try {
            Path destination = targetDir.resolve("cd.pdf");
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            handleError("Ошибка при сохранении результата", e);
        }
    }
    
    private static void cleanupTempFiles(Path dir) {
        try {
            if (dir != null && Files.exists(dir)) {
                Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (Exception e) {
            handleError("Ошибка при очистке временных файлов", e);
        }
    }
    
    private static void handleError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }
}