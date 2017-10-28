package com.mslipper.mailmop.service;

import java.util.concurrent.Future;

@FunctionalInterface
public interface TaskExecutor {
    Future<?> execute(Runnable task);
}
