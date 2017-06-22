package ru.smitdev;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;

public class CheckStatusTask implements Runnable {

    private final String[] players = new String[]{"stretch", "jessie", "wheezy", "squeeze",
            "lenny", "etch", "sarge", "woody", "potato", "slink", "hamm"};

    public static final String STATUS_AWAY = "AWAY";
    public static final String STATUS_BUSY = "BUSY";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_OFFLINE = "OFFLINE";

    public void run() {

        System.out.println(new Date()+ ": " + "Check players status...");

        Map<String, String> statusList = new HashMap();
        ExecutorService executor = Executors.newFixedThreadPool(11);
        List<Future<String[]>> list = new ArrayList<Future<String[]>>();

        for (int i = 0; i < players.length; i++) {
            Callable<String[]> callable = new PlayerStatusTester(players[i]);

            Future<String[]> future = executor.submit(callable);
            list.add(future);
        }

        for (Future<String[]> future : list) {
            try {
                String[] futureResult = future.get();
                String player = futureResult[0];
                String playerStatus = futureResult[1];
                statusList.put(player, playerStatus);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        if (checkIfAllPlayersAvailable(statusList)) {
            executor.shutdown();
            System.out.println("You can play now!");
            System.out.println();
        } else {
            System.out.println("Not all players are available...");
            System.out.println();
        }

    }

    private static boolean checkIfAllPlayersAvailable(Map<String, String> playersStatusMap) {
        boolean isAllPlayersAvailable = true;
        for (Map.Entry<String, String> playersStatusEntry :  playersStatusMap.entrySet()) {
            if (!playersStatusEntry.getValue().equals(CheckStatusTask.STATUS_READY)) {
                isAllPlayersAvailable = false;
                showPlayersStatus(playersStatusMap);
                break;
            }
        }
        return isAllPlayersAvailable;
    }

    private static void showPlayersStatus(Map<String, String> playersStatusMap) {
        for (Map.Entry<String, String> entry :  playersStatusMap.entrySet()) {
            System.out.println(entry.getKey() + " status: " + entry.getValue());
        }
        System.out.println();
    }

    private class PlayerStatusTester implements Callable<String[]> {

        private final String URL = "http://rd.iskrauraltel.ru:33388/simple-test/";
        private final int TIMEOUT = 5;
        private String player;

        public PlayerStatusTester(String player) {
            this.player = player;
        }

        public String[] call() throws Exception {
            RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(1000 * TIMEOUT).
                    setConnectTimeout(1000 * TIMEOUT).setSocketTimeout(1000 * TIMEOUT).build();
            HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
            CloseableHttpClient httpClient = builder.build();

            HttpGet request = new HttpGet(URL + player);
            HttpResponse response;
            try {
                response = httpClient.execute(request);
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    System.out.println(line);
                    if (line.equals(STATUS_AWAY)) {
                        return new String[] {player,STATUS_AWAY};
                    } else if (line.equals(STATUS_BUSY)) {
                        return new String[] {player, STATUS_BUSY};
                    } else if (line.equals(STATUS_READY)) {
                            return new String[] {player, STATUS_READY};
                    }
                }
            } catch (IOException e) {
                //timeout exception, status OFFLINE
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return new String[] {player,STATUS_OFFLINE};
        }
    }
}