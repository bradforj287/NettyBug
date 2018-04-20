package com.bradforj287.nettybug;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestClient {
    private static Logger logger = LoggerFactory.getLogger(ServerMainBroken.class);

    private final static String URL = "<ENTER URL HERE>";
    private final static int NUM_THREADS = 10;
    private final static int iterations = 2000;
    private final static boolean validateAllSameResponse = false;

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
        logger.info(String.format("Hitting URL: %s", URL));
        logger.info(String.format("Executing %s iterations", iterations));

        List<Thread> tlist = new ArrayList<>();

        Stopwatch sw = Stopwatch.createStarted();
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadNum = i;
            Thread t = new Thread(() -> {
                try {
                    doTest(threadNum);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            tlist.add(t);
            t.start();
        }

        tlist.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


        double seconds = sw.elapsed(TimeUnit.SECONDS);
        double ips = iterations * NUM_THREADS / seconds;

        logger.info(String.format("Requests per second = %s", ips));
    }

    private static void doTest(int threadNum) throws Exception {
        final String first = getHTML(URL);
        logger.info("Finished iteration 0");
        for (int i = 1; i < iterations; i++) {
            final String str = getHTML(URL);
            logger.info(String.format("tNum = %s finished iteration %s", threadNum, i));
            if (validateAllSameResponse && !str.equals(first)) {
                logger.info("Values do not match, exiting");
                break;
            }
        }
    }
}