package com.mslipper.mailmop;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class ConcurrencyUtil {
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(2);

    private static final ExecutorService fixedPool = Executors.newFixedThreadPool(2);

    public static ForkJoinPool forkJoinPool() {
        return forkJoinPool;
    }

    public static ExecutorService fixedPool() {
        return fixedPool;
    }
}
