package com.microsoft.jenkins.function.integration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Utils {
    /**
     * Extract a resource file to destination path
     *
     * @param clazz        Class with resource
     * @param resourcePath Resource path
     * @param dstPath      Destination path
     * @throws IOException
     */
    public static void extractResourceFile(Class clazz, String resourcePath, String dstPath) throws IOException {
        InputStream resource = clazz.getResourceAsStream(resourcePath);
        File dstFile = new File(dstPath);
        dstFile.delete();
        File parentDir = dstFile.getParentFile();
        parentDir.mkdirs();
        Files.copy(resource, dstFile.toPath());
        dstFile.deleteOnExit();
    }

    /**
     * Extract all the resource files under folderPath to destination path
     *
     * @param clazz      Class with resource
     * @param folderPath Resource folder path
     * @param dstPath    Destination path
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void extractResourceFolder(Class clazz, String folderPath, String dstPath) throws IOException, URISyntaxException {
        URL resource = clazz.getResource(folderPath);
        File file = Paths.get(resource.toURI()).toFile();
        File dst = new File(dstPath);
        FileUtils.copyDirectory(file, dst);
    }

    /**
     * Check if a web app is ready to respond with certain content
     *
     * @param url           Web app URL
     * @param expectContent Expected response content
     * @param timeout       Seconds before timeout
     * @throws IllegalStateException
     * @throws InterruptedException
     */
    public static void waitForAppReady(URL url, String expectContent, int timeout) throws IllegalStateException, InterruptedException {
        int elapsed = 0;

        while (elapsed < timeout) {
            URLConnection conn = null;
            try {
                conn = url.openConnection();
                InputStream in = conn.getInputStream();
                String content = IOUtils.toString(in, "UTF-8");

                if (content.indexOf(expectContent) >= 0) {
                    return;
                }
            } catch (IOException e) {
                // Ignore and continue waiting
            }

            elapsed++;
            TimeUnit.SECONDS.sleep(1);
        }

        throw new IllegalStateException("App failed to start in timeout");
    }

    private Utils() {
        // Hide
    }
}
