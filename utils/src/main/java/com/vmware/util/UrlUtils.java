package com.vmware.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Util methods for url functionality.
 */
public class UrlUtils {

    private static final Logger log = LoggerFactory.getLogger(UrlUtils.class);

    public static String addTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url : url + "/";
    }

    public static String removeTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String addRelativePaths(String url, Object... paths) {
        if (paths.length == 0) {
            return url;
        }
        String urlWithoutTrailingSlash = removeTrailingSlash(url);
        StringBuilder urlBuilder = new StringBuilder(urlWithoutTrailingSlash);
        boolean lastPathEndedWithSlash = false;
        for (Object path : paths) {
            String pathAsString = String.valueOf(path);
            if (pathAsString.startsWith("/") || lastPathEndedWithSlash) {
                urlBuilder.append(pathAsString);
            } else {
                urlBuilder.append("/").append(pathAsString);
            }
            lastPathEndedWithSlash = pathAsString.endsWith("/");
        }
        return urlBuilder.toString();
    }

    public static boolean isUrlReachable(String urlString) {
        try {
            log.debug("Checking that url {} is reachable", urlString);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // Use HEAD request for efficiency
            connection.setConnectTimeout(5000); // Set connection timeout (5 seconds)
            connection.setReadTimeout(5000);    // Set read timeout (5 seconds)

            int responseCode = connection.getResponseCode();

            // Consider 2xx (Success), 3xx (Redirection) as reachable
            // You might want to refine this based on your specific needs
            return (responseCode >= 200 && responseCode < 400);

        } catch (IOException e) {
            log.debug("Error checking for url " + urlString, e);
            return false;
        }
    }
}
