package com.mslipper.mailmop.gui;

import com.mslipper.mailmop.domain.HasMailmopGroup;
import com.mslipper.mailmop.domain.MailmopGroup;
import com.mslipper.mailmop.domain.MailmopMessage;
import com.mslipper.mailmop.domain.event.MessagesDeletedEvent;
import com.mslipper.mailmop.domain.event.ProgressEvent;
import com.mslipper.mailmop.domain.event.ProgressListener;
import com.mslipper.mailmop.gui.grouping.CountingMailmopGroupCell;
import com.mslipper.mailmop.gui.grouping.GroupingStrategy;
import com.mslipper.mailmop.gui.grouping.MailmopGroupCell;
import com.mslipper.mailmop.gui.grouping.StringGroupingStrategy.SenderDomainGroupingStrategy;
import com.mslipper.mailmop.gui.grouping.StringGroupingStrategy.SenderEmailGroupingStrategy;
import com.mslipper.mailmop.gui.grouping.StringGroupingStrategy.SenderFullGroupingStrategy;
import com.mslipper.mailmop.inject.ExecutorService;
import com.mslipper.mailmop.service.TaskExecutor;
import com.mslipper.mailmop.service.email.gmail.GmailService;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultsController implements Controller {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsController.class);

    private final AtomicInteger deletionJobPointer = new AtomicInteger(-1);

    private TaskExecutor taskExecutor;

    private GmailService gmailService;

    private ViewState state;

    @FXML
    private ChoiceBox groupingChoice;

    @FXML
    private ListView<MailmopGroup> groupListView;

    @FXML
    private ListView<DeletionJob> activityListView;

    @FXML
    private TableView<MailmopMessage> messageTableView;

    @FXML
    private ImageView exampleImage;

    @FXML
    private AnchorPane tableContainer;

    @FXML
    private Button deleteButton;

    @Inject
    public ResultsController(@ExecutorService TaskExecutor taskExecutor,
                             GmailService gmailService) {
        this.taskExecutor = taskExecutor;
        this.gmailService = gmailService;
        this.state = new ViewState();
    }

    public void setMessages(List<MailmopMessage> messages) {
        state.setMessageStore(MessageStore.withMessages(messages));
        onMessagesSet();
        taskExecutor.execute(this::pollDeletion);
    }

    @FXML
    public void initialize() {
        deleteButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                messageTableView.getSelectionModel().getSelectedCells().size() == 0,
            messageTableView.getSelectionModel().getSelectedCells()));
    }

    @FXML
    public void onClickSelectAll() {
        messageTableView.getSelectionModel().selectAll();
    }

    @FXML
    public void onClickDelete() {
        List<MailmopMessage> selections = new ArrayList<>(messageTableView.getSelectionModel().getSelectedItems());
        MailmopGroup group = state.selectedGroupProperty().get();
        DeletionJob deletingGroup = new DeletionJob(group, selections);
        state.deletionJobsProperty().add(deletingGroup);
        messageTableView.getSelectionModel().clearSelection();
        state.getMessageStore().softDelete(selections);
        ((RefreshableTableViewSkin) messageTableView.getSkin()).refresh();
    }

    private void pollDeletion() {
        try {
            while (true) {
                int size = state.deletionJobsProperty().size();

                if (size == 0 || deletionJobPointer.get() == size - 1) {
                    Thread.sleep(100);
                    continue;
                }

                performDeletion(deletionJobPointer.incrementAndGet());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void performDeletion(int idx) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        DeletionJob job = state.deletionJobsProperty().get(idx);

        gmailService.deleteMessages(job.getMessages(), new ProgressListener<MessagesDeletedEvent>() {
            @Override
            public void onProgress(ProgressEvent event) {
                Platform.runLater(() -> job.setPercentComplete(event.asPercentage()));
            }

            @Override
            public void onSuccess(MessagesDeletedEvent result) {
                Platform.runLater(() -> {
                    job.setPercentComplete(1);
                    latch.countDown();
                });
            }

            @Override
            public void onFailure(Throwable e) {
                LOGGER.error("Caught error deleting messages:", e);
                latch.countDown();
            }
        });

        latch.await();
    }

    private void onMessagesSet() {
        initializeGroupingChoice();

        groupListView.setCellFactory(CountingMailmopGroupCell.factory());
        groupListView.itemsProperty().bind(state.getMessageStore().groupsProperty());
        state.selectedGroupProperty().bind(groupListView.getSelectionModel().selectedItemProperty());

        activityListView.setSelectionModel(new NoSelectSelectionModel<>());
        activityListView.itemsProperty().bind(state.deletionJobsProperty());
        activityListView.setCellFactory(DeletionJobCell.factory());

        Binding<ObservableList<MailmopMessage>> messagesBinding = Bindings.createObjectBinding(() -> {
            MailmopGroup selected = state.selectedGroupProperty().get();

            if (selected == null) {
                return FXCollections.observableList(Collections.emptyList());
            }

            return FXCollections.observableList(state.selectedGroupProperty().get().getMessages())
                .filtered((message) -> !state.getMessageStore().isDeleted(message))
                .sorted(Comparator.comparingLong(MailmopMessage::getReceivedAt).reversed());
        }, state.selectedGroupProperty(), state.getMessageStore().deletedMessages());

        state.selectedGroupProperty().addListener((e) -> messageTableView.getSelectionModel().clearSelection());
        ObservableList<TableColumn<MailmopMessage, ?>> columns = messageTableView.getColumns();

        TableColumn<MailmopMessage, Boolean> checkboxColumn = (TableColumn<MailmopMessage, Boolean>) columns.get(0);
        TableColumn<MailmopMessage, ?> fromColumn = columns.get(1);
        TableColumn<MailmopMessage, ?> subjectColumn = columns.get(2);
        TableColumn<MailmopMessage, ?> dateColumn = columns.get(3);

        checkboxColumn.setCellFactory(new CheckboxCellFactory<>(true, false));
        fromColumn.setCellFactory(new PaddedCellFactory<>());
        subjectColumn.setCellFactory(new PaddedCellFactory<>());
        dateColumn.setCellFactory(new PaddedCellFactory<>(false, true));

        checkboxColumn.setCellValueFactory((value) -> Bindings.createBooleanBinding(() -> {
            return value.getTableView().getSelectionModel().getSelectedItems().contains(value.getValue());
        }, value.getTableView().getSelectionModel().getSelectedItems()));
        fromColumn.setCellValueFactory(new PropertyValueFactory<>("from"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        dateColumn.setCellValueFactory(new TimeAgoCellValueFactory<>());

        checkboxColumn.setMaxWidth(50);
        checkboxColumn.setPrefWidth(50);
        dateColumn.setMaxWidth(125);
        dateColumn.setPrefWidth(125);
        dateColumn.getStyleClass().add("right");
        resizeColumns();

        messageTableView.itemsProperty().bind(messagesBinding);
        messageTableView.setSkin(new RefreshableTableViewSkin<>(messageTableView));
        messageTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        messageTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        messageTableView.widthProperty().addListener((e) -> resizeColumns());

        BooleanBinding hasItemsBinding = Bindings.createBooleanBinding(
            () -> messagesBinding.getValue().size() > 0, messagesBinding);
        tableContainer.visibleProperty().bind(hasItemsBinding);
        exampleImage.visibleProperty().bind(hasItemsBinding.not());
    }

    @SuppressWarnings("unchecked")
    private void initializeGroupingChoice() {
        groupingChoice.getItems().addAll(GroupingChoice.getLabels());
        groupingChoice.setValue(GroupingChoice.SENDER_FULL.getLabel());
        state.getMessageStore().groupingStrategyProperty().bind(Bindings.createObjectBinding(() ->
                GroupingChoice.fromLabel((String) groupingChoice.getValue()).getGroupingStrategy(),
            groupingChoice.valueProperty()));
    }

    private void resizeColumns() {
        ObservableList<TableColumn<MailmopMessage, ?>> columns = messageTableView.getColumns();
        TableColumn<MailmopMessage, ?> fromColumn = columns.get(1);
        TableColumn<MailmopMessage, ?> subjectColumn = columns.get(2);
        double width = messageTableView.getWidth();
        double available = width - 175;
        double oneThird = available * 0.33;
        fromColumn.setPrefWidth(oneThird);
        subjectColumn.setPrefWidth(available - oneThird);
    }

    private class ViewState {
        private ObjectProperty<MailmopGroup> selectedGroup = new SimpleObjectProperty<>(null);

        private ListProperty<DeletionJob> deletionJobs = new SimpleListProperty<>(
            FXCollections.synchronizedObservableList(FXCollections.observableArrayList()));

        private MessageStore messageStore;

        public ObjectProperty<MailmopGroup> selectedGroupProperty() {
            return selectedGroup;
        }

        public ListProperty<DeletionJob> deletionJobsProperty() {
            return deletionJobs;
        }

        public void setMessageStore(MessageStore messageStore) {
            this.messageStore = messageStore;
        }

        public MessageStore getMessageStore() {
            return messageStore;
        }
    }

    private static class DeletionJob implements HasMailmopGroup {
        private final MailmopGroup group;

        private final List<MailmopMessage> messages;

        private DoubleProperty percentComplete = new SimpleDoubleProperty(0);

        public DeletionJob(MailmopGroup group, List<MailmopMessage> messages) {
            this.group = group;
            this.messages = messages;
        }

        @Override
        public MailmopGroup getGroup() {
            return group;
        }

        public List<MailmopMessage> getMessages() {
            return messages;
        }

        public DoubleProperty percentCompleteProperty() {
            return percentComplete;
        }

        public void setPercentComplete(double percentComplete) {
            this.percentComplete.set(percentComplete);
        }
    }

    private static class DeletionJobCell extends MailmopGroupCell<DeletionJob> {
        private DeletionIcon icon = new DeletionIcon();

        @Override
        public Node updateRightSide(DeletionJob wrapper) {
            icon.progressProperty().bind(wrapper.percentCompleteProperty());
            return icon;
        }

        public static Callback<ListView<DeletionJob>, ListCell<DeletionJob>> factory() {
            return (param) -> new DeletionJobCell();
        }
    }

    private enum GroupingChoice {
        SENDER_FULL("Sender Full Name", new SenderFullGroupingStrategy()),
        SENDER_EMAIL_ADDRESS("Sender E-Mail Address", new SenderEmailGroupingStrategy()),
        SENDER_DOMAIN("Sender Domain", new SenderDomainGroupingStrategy());

        private static String[] labels;

        private static Map<String, GroupingChoice> choicesMap = new HashMap<>(3);

        static {
            GroupingChoice[] choices = GroupingChoice.values();
            labels = new String[choices.length];

            for (int i = 0; i < choices.length; i++) {
                GroupingChoice choice = choices[i];
                String label = choice.getLabel();
                labels[i] = label;
                choicesMap.put(label, choice);
            }
        }

        private String label;

        private GroupingStrategy groupingStrategy;

        GroupingChoice(String label, GroupingStrategy groupingStrategy) {
            this.label = label;
            this.groupingStrategy = groupingStrategy;
        }

        public String getLabel() {
            return label;
        }

        public GroupingStrategy getGroupingStrategy() {
            return groupingStrategy;
        }

        public static String[] getLabels() {
            return labels;
        }

        public static GroupingChoice fromLabel(String label) {
            GroupingChoice choice = choicesMap.get(label);

            if (choice == null) {
                throw new IllegalArgumentException("Invalid label: " + label);
            }

            return choice;
        }
    }
}
