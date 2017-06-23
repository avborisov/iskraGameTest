package ru.smitdev;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        String[] players = new String[]{"stretch", "jessie", "wheezy", "squeeze",
                "lenny", "etch", "sarge", "woody", "potato", "slink", "hamm"};

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        FindNotReadyPlayersTask findNotReadyPlayersTask = new FindNotReadyPlayersTask(players);
        executor.scheduleAtFixedRate(findNotReadyPlayersTask, 0,10, TimeUnit.SECONDS);

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            findNotReadyPlayersTask.shutdown();
        }
    }
}
