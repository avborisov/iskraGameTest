package ru.smitdev;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        String[] players = new String[]{"stretch", "jessie", "wheezy", "squeeze",
                "lenny", "etch", "sarge", "woody", "potato", "slink", "hamm"};

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(new CheckStatusTask(players), 0,5, TimeUnit.SECONDS);
    }
}
