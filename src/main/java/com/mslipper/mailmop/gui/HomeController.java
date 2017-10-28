package com.mslipper.mailmop.gui;

import com.mslipper.mailmop.domain.MailmopMessage;
import com.mslipper.mailmop.domain.event.MessagesFetchedEvent;
import com.mslipper.mailmop.domain.event.ProgressEvent;
import com.mslipper.mailmop.domain.event.ProgressListener;
import com.mslipper.mailmop.inject.ExecutorService;
import com.mslipper.mailmop.service.TaskExecutor;
import com.mslipper.mailmop.service.email.EmailService.AuthorizationListener;
import com.mslipper.mailmop.service.email.gmail.GmailService;
import com.mslipper.mailmop.service.email.gmail.event.AuthorizationEvent;
import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import javax.inject.Inject;
import java.util.List;

public class HomeController implements Controller {
    private final GmailService gmailService;

    private final TaskExecutor taskExecutor;

    private final SceneManager sceneManager;

    private final Application application;

    private final GmailViewState viewState;

    @FXML
    private VBox authContainer;

    @FXML
    private VBox progressContainer;

    @FXML
    private Button actionButton;

    @FXML
    private ProgressBar fetchProgress;

    @FXML
    private TextFlow statusText;

    @FXML
    private Label progressText;

    @Inject
    public HomeController(GmailService gmailService,
                          @ExecutorService TaskExecutor taskExecutor,
                          SceneManager sceneManager,
                          Application application) {
        this.gmailService = gmailService;
        this.taskExecutor = taskExecutor;
        this.sceneManager = sceneManager;
        this.application = application;
        this.viewState = new GmailViewState();
    }

    @FXML
    private void initialize() {
        statusText.setMaxWidth(340);
        statusText.setTextAlignment(TextAlignment.CENTER);

        fetchProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            if (!viewState.isDeterminate() || viewState.getTotalItems() == 0) {
                return ProgressBar.INDETERMINATE_PROGRESS;
            }

            return ((double) viewState.getFetchedItems()) / viewState.getTotalItems();
        }, viewState.fetchedItemsProperty()));

        progressText.textProperty().bind(Bindings.createStringBinding(() -> {
            if (!viewState.isDeterminate()) {
                return "Fetched page " + viewState.getFetchedItems() + "...";
            }

            return "Fetched message " + viewState.getFetchedItems() + " of " +
                viewState.getTotalItems() + "...";
        }, viewState.fetchedItemsProperty()));

        viewState.statusProperty().addListener(this::onStatusChange);
        actionButton.setOnAction(this::onClickActionButton);
        onStatusChange(viewState.statusProperty(), null, viewState.getStatus());
    }

    private void onStatusChange(ObservableValue<? extends Status> observable, Status oldValue, Status newValue) {
        statusText.getChildren().setAll(getAuthStatusText());
        authContainer.setVisible(!newValue.equals(Status.FETCHING));
        progressContainer.setVisible(newValue.equals(Status.FETCHING));
    }

    private void onClickActionButton(Event e) {
        if (viewState.statusProperty().getValue().equals(Status.START)) {
            authorize();
            return;
        }

        openBrowser();
    }

    private void openBrowser() {
        HostServicesDelegate delegate = HostServicesFactory.getInstance(application);
        delegate.showDocument(viewState.authUrlProperty().get());
    }

    private Text getAuthStatusText() {
        String label;

        switch (viewState.statusProperty().getValue()) {
            case START:
                label = "Ready to get started? Hit the button below to sign in to Gmail.";
                break;
            case AUTHORIZING:
                label = "We've tried to open your browser to Google's sign in screen. If it didn't open, " +
                    "click the link below.";
                break;
            case FAILED:
                label = "Something went wrong. Mind trying again?";
                break;
            default:
                label = "";
        }

        Text text = new Text(label);
        text.setFill(Color.BLACK);
        text.setFont(Font.font(20));
        return text;
    }

    private void authorize() {
        taskExecutor.execute(() -> gmailService.authorize(new AuthorizationListener() {
            @Override
            public void onOAuthUrl(String url) {
                authorizing(url);
            }

            @Override
            public void onSuccess(AuthorizationEvent result) {
                fetch();
            }

            @Override
            public void onFailure(Throwable e) {
                fail(e);
            }
        }));
    }

    private void authorizing(String url) {
        Platform.runLater(() -> {
            sceneManager.bringToFront();
            viewState.setStatus(Status.AUTHORIZING);
            viewState.setAuthUrl(url);
        });
    }

    private void fetch() {
        taskExecutor.execute(() -> {
            Platform.runLater(() -> viewState.setStatus(Status.FETCHING));

            ProgressListener<MessagesFetchedEvent> listener = new ProgressListener<MessagesFetchedEvent>() {
                @Override
                public void onProgress(ProgressEvent event) {
                    handleProgress(event);
                }

                @Override
                public void onSuccess(MessagesFetchedEvent result) {
                    done(result);
                }

                @Override
                public void onFailure(Throwable e) {
                    fail(e);
                }
            };

            gmailService.fetchMessages(listener);
        });
    }

    private void handleProgress(ProgressEvent e) {
        Platform.runLater(() -> viewState.mapProgressEvent(e));
    }

    private void done(MessagesFetchedEvent result) {
        Platform.runLater(() -> {
            List<MailmopMessage> messages = result.getMessages();
            sceneManager.present("results");
            ((ResultsController) sceneManager.getController()).setMessages(messages);
        });
    }

    private void fail(Throwable e) {
        e.printStackTrace();
        System.exit(1);
    }

    private class GmailViewState {
        private StringProperty authUrl = new SimpleStringProperty();

        private Property<Status> status = new SimpleObjectProperty<>();

        private BooleanProperty determinate = new SimpleBooleanProperty();

        private LongProperty totalItems = new SimpleLongProperty();

        private LongProperty fetchedItems = new SimpleLongProperty();

        private GmailViewState() {
            status.setValue(Status.START);
            determinate.setValue(false);
            totalItems.setValue(0);
            fetchedItems.setValue(0);
        }

        StringProperty authUrlProperty() {
            return authUrl;
        }

        void setAuthUrl(String url) {
            authUrl.setValue(url);
        }

        Property<Status> statusProperty() {
            return status;
        }

        void setStatus(Status newStatus) {
            status.setValue(newStatus);
        }

        LongProperty fetchedItemsProperty() {
            return fetchedItems;
        }

        void mapProgressEvent(ProgressEvent progressEvent) {
            totalItems.setValue(progressEvent.getTotalItems());
            fetchedItems.setValue(progressEvent.getProcessedItems());
            determinate.setValue(progressEvent.isDeterminate());
        }

        boolean isDeterminate() {
            return determinate.get();
        }

        long getTotalItems() {
            return totalItems.get();
        }

        long getFetchedItems() {
            return fetchedItems.get();
        }

        Status getStatus() {
            return status.getValue();
        }
    }

    private enum Status {
        START,
        AUTHORIZING,
        FETCHING,
        FAILED
    }
}
