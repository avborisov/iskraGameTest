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

/**
 * {@link CheckStatusTask} проверяет доступность игроков и сообщает о результате проверки.
 * Запускает дочерние потоки {@link PlayerStatusTester} для ассинхронной проверки статусов.
 * Список игроков должен быть передан объекту при его инициализации.
 */
public class CheckStatusTask implements Runnable {

    public static final String STATUS_AWAY = "AWAY";
    public static final String STATUS_BUSY = "BUSY";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_OFFLINE = "OFFLINE";

    private String[] playersList;

    /**
     * @param playersList список игроков для проверки доступности
     */
    public CheckStatusTask(String[] playersList) {
        this.playersList = playersList;
    }

    public void run() {
        System.out.println(new Date() + ": " + "Check players status...");

        ExecutorService executor = Executors.newFixedThreadPool(playersList.length);
        try {
            List<Future<String[]>> list = new ArrayList<Future<String[]>>();

            for (int i = 0; i < playersList.length; i++) {
                Callable<String[]> callable = new PlayerStatusTester(playersList[i]);

                Future<String[]> future = executor.submit(callable);
                list.add(future);
            }

            Map<String, String> playersStatusList = new HashMap();
            for (Future<String[]> future : list) {
                try {
                    String[] futureResult = future.get();
                    String player = futureResult[0];
                    String playerStatus = futureResult[1];
                    playersStatusList.put(player, playerStatus);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            if (checkIfAllPlayersAvailable(playersStatusList)) {
                System.out.println("All players are available! You can play now!");
                System.out.println();
            } else {
                System.out.println("Not all players are available...");
                System.out.println();
            }
        } finally {
            executor.shutdown();
        }

    }

    /**
     * Метод проверяет, все ли игроки доступны для игры
     *
     * @param playersStatusMap - Map, в котором содержатся пары "Игрок - текущий статус".
     * @return true, если все игроки имеют статус READY. false, елси хотя бы один игрок имеет другой статус
     */
    private static boolean checkIfAllPlayersAvailable(Map<String, String> playersStatusMap) {
        boolean isAllPlayersAvailable = true;
        for (Map.Entry<String, String> playersStatusEntry : playersStatusMap.entrySet()) {
            if (!playersStatusEntry.getValue().equals(CheckStatusTask.STATUS_READY)) {
                isAllPlayersAvailable = false;
                showPlayersStatus(playersStatusMap);
                break;
            }
        }
        return isAllPlayersAvailable;
    }

    /**
     * Вывод статуса всех игроков
     *
     * @param playersStatusMap - Map, в котором содержатся пары "Игрок - текущий статус".
     */
    private static void showPlayersStatus(Map<String, String> playersStatusMap) {
        for (Map.Entry<String, String> entry : playersStatusMap.entrySet()) {
            System.out.println(entry.getKey() + " status: " + entry.getValue());
        }
        System.out.println();
    }

    /**
     * Поток для проверки статуса одного конкретного игрока.
     */
    private class PlayerStatusTester implements Callable<String[]> {

        private final String URL = "http://rd.iskrauraltel.ru:33388/simple-test/";
        private final int TIMEOUT = 5;
        private String player;

        /**
         * @param player игрок, чей статус будет проверен
         */
        public PlayerStatusTester(String player) {
            this.player = player;
        }

        public String[] call() throws Exception {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(1000 * TIMEOUT)
                    .setConnectTimeout(1000 * TIMEOUT)
                    .setSocketTimeout(1000 * TIMEOUT)
                    .build();
            HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
            CloseableHttpClient httpClient = builder.build();

            HttpGet request = new HttpGet(URL + player);
            HttpResponse response;
            try {
                response = httpClient.execute(request);
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    if (line.equals(STATUS_AWAY)) {
                        return new String[]{player, STATUS_AWAY};
                    } else if (line.equals(STATUS_BUSY)) {
                        return new String[]{player, STATUS_BUSY};
                    } else if (line.equals(STATUS_READY)) {
                        return new String[]{player, STATUS_READY};
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
            return new String[]{player, STATUS_OFFLINE};
        }
    }
}
