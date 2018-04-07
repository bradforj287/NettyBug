package com.bradforj287.nettybug;
import com.google.common.base.Stopwatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class TestClient {
    private final static String URL = "dd";
    private final static int iterations = 100000;

    private static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    public static void main(String args[]) throws Exception {

        Stopwatch sw = Stopwatch.createStarted();

        final String first = getHTML(URL);
        System.out.println("Finished iteration 0");
        for (int i = 1; i < iterations; i++) {
            final String str = getHTML(URL);
            System.out.println("finished iteration " + i);
            if (!str.equals(first)) {
                System.out.println("Values do not match, exiting");
                break;
            }
        }

        double seconds = sw.elapsed(TimeUnit.SECONDS);
        double ips = iterations / seconds;

        System.out.println(String.format("Requests per second = %s", ips));
    }

}