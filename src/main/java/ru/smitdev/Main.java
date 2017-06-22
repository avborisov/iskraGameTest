package ru.smitdev;

import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(new CheckStatusTask(), 0,5, TimeUnit.SECONDS);
    }
}
