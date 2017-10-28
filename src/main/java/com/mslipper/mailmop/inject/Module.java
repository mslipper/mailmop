package com.mslipper.mailmop.inject;

import com.google.inject.AbstractModule;
import com.mslipper.mailmop.gui.SceneManager;
import com.mslipper.mailmop.service.ExecutorServiceTaskExecutor;
import com.mslipper.mailmop.service.ForkJoinPoolTaskExecutor;
import com.mslipper.mailmop.service.TaskExecutor;
import javafx.application.Application;
import javafx.stage.Stage;

public class Module extends AbstractModule {
    private Application application;

    private final Stage stage;

    public Module(Application application, Stage stage) {
        this.application = application;
        this.stage = stage;
    }

    @Override
    protected void configure() {
        bind(Stage.class)
            .toInstance(stage);
        bind(Application.class)
            .toInstance(application);
        bind(TaskExecutor.class)
            .annotatedWith(ExecutorService.class)
            .to(ExecutorServiceTaskExecutor.class)
            .asEagerSingleton();
        bind(TaskExecutor.class)
            .annotatedWith(ForkJoin.class)
            .to(ForkJoinPoolTaskExecutor.class)
            .asEagerSingleton();
        bind(SceneManager.class)
            .asEagerSingleton();
    }
}
