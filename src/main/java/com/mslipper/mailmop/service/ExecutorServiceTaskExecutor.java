package com.mslipper.mailmop.service;

import com.mslipper.mailmop.ConcurrencyUtil;

import java.util.concurrent.Future;

public class ExecutorServiceTaskExecutor implements TaskExecutor {
    @Override
    public Future<?> execute(Runnable task) {
        return ConcurrencyUtil.fixedPool().submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
