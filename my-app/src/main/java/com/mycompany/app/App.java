package com.mycompany.app;

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
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "D:\\WORK ARTEM\\QA_UNN\\chromedriver-win64\\chromedriver.exe");
        System.setProperty("webdriver.chrome.whitelistedIps", "");
        System.setProperty("webdriver.chrome.silentOutput", "true");

        Path downloadDir = Paths.get(System.getProperty("java.io.tmpdir"), "selenium_download_" + System.currentTimeMillis());
        Path resultDir = Paths.get(System.getProperty("user.dir"), "result");
        Path dataPath = Paths.get(System.getProperty("user.dir"), "data", "data.txt");

        try {
            if (!Files.exists(resultDir)) {
                Files.createDirectories(resultDir);
            }
            Files.createDirectories(downloadDir);

            if (!Files.exists(dataPath)) {
                System.err.println("Ошибка: Файл данных не найден: " + dataPath);
                return;
            }

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir.toString());
            prefs.put("download.prompt_for_download", false);
            prefs.put("plugins.always_open_pdf_externally", true);

            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", prefs);

            WebDriver driver = new ChromeDriver(options);
            try {
                driver.get("https://www.papercdcase.com/index.php");

                List<String> data = Files.readAllLines(dataPath);
                if (data.size() < 4) {
                    System.err.println("Ошибка: Файл данных должен содержать как минимум 4 строки (исполнитель, альбом, пустая строка и хотя бы один трек)");
                    return;
                }

                driver.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")
                ).sendKeys(data.get(0));

                driver.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")
                ).sendKeys(data.get(1));

                for (int i = 0; i < data.size() - 3; i++) {
                    String xpath = String.format("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%d]/table/tbody/tr[%d]/td[2]/input",
                            i / 8 + 1, i % 8 + 1);
                    driver.findElement(By.xpath(xpath)).sendKeys(data.get(i + 3));
                }

                WebElement radio = driver.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
                if (!radio.isSelected()) {
                    radio.click();
                }

                radio = driver.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
                if (!radio.isSelected()) {
                    radio.click();
                }

                driver.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")
                ).click();

                boolean pdfFound = false;
                long startTime = System.currentTimeMillis();
                long timeout = 30000;

                Path[] pdfFiles = null;
                while (!pdfFound && System.currentTimeMillis() - startTime < timeout) {
                    try {
                        pdfFiles = Files.list(downloadDir)
                                .filter(file -> file.toString().endsWith(".pdf"))
                                .toArray(Path[]::new);

                        if (pdfFiles.length > 0) {
                            pdfFound = true;
                        } else {
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        Thread.sleep(100);
                    }
                }

                if (!pdfFound) {
                    System.err.println("Ошибка: PDF файл не был создан в течение отведенного времени");
                    return;
                }

                Path resultFile = resultDir.resolve("cd.pdf");
                Files.copy(pdfFiles[0], resultFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении: " + e.getMessage());
                e.printStackTrace();
            } finally {
                driver.quit();
                try {
                    Files.walk(downloadDir)
                            .sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(java.io.File::delete);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при инициализации: " + e.getMessage());
            e.printStackTrace();
        }
    }
}