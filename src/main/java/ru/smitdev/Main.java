package ru.smitdev;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        String[] players = new String[]{"stretch", "jessie", "wheezy", "squeeze",
                "lenny", "etch", "sarge", "woody", "potato", "slink", "hamm"};

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final FindNotReadyPlayersTask task = new FindNotReadyPlayersTask(players);
        executor.scheduleAtFixedRate(task, 0,10, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                task.shutdownNow();
                executor.shutdownNow();
            }
        });
    }
}
