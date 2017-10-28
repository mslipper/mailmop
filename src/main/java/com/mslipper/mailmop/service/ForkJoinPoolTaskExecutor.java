package com.mslipper.mailmop.service;

import com.mslipper.mailmop.ConcurrencyUtil;

import java.util.concurrent.Future;

public class ForkJoinPoolTaskExecutor implements TaskExecutor {
    @Override
    public Future<?> execute(Runnable task) {
        return ConcurrencyUtil.forkJoinPool().submit(task);
    }
}
