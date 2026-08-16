package org.wowmr.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.wowmr.api.BlizzardApiClient;
import org.wowmr.db.DatabaseHelper;
import org.wowmr.db.Session;
import org.wowmr.model.Encounter;
import org.wowmr.model.Instance;
import org.wowmr.model.LootItem;
import org.wowmr.util.LootLoggerParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainController {

    @FXML private ToggleButton homeToggle, farmToggle;
    @FXML private ComboBox<String> themeSelector;
    @FXML private StackPane contentStack;
    @FXML private BorderPane homeView, farmView;

    @FXML private ComboBox<Instance> instanceBox;
    @FXML private ComboBox<Encounter> encounterBox;
    @FXML private Label statusLabel;
    @FXML private VBox instanceInfoBox, encounterInfoBox;
    @FXML private Button saveSessionButton;

    @FXML private Button startStopButton, infoButton;
    @FXML private Label timerLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Session> sessionTable;
    @FXML private TableColumn<Session, String> dateCol, durationCol;
    @FXML private TableColumn<Session, Integer> mobsCol, copperCol;
    @FXML private TableColumn<Session, Void> lootInfoCol;
    @FXML private TableView<LootItem> lootTable;
    @FXML private TableColumn<LootItem, String> itemCol;
    @FXML private TableColumn<LootItem, Integer> qtyCol;
    @FXML private TableColumn<LootItem, Double> rateCol;

    private final BlizzardApiClient api = new BlizzardApiClient();
    private boolean farmRunning = false;
    private Timer farmTimer;
    private int farmSeconds = 0;
    private final ObservableList<Session> allSessions = FXCollections.observableArrayList();
    private FilteredList<Session> filteredSessions;

    @FXML
    public void initialize() {
        DatabaseHelper.initDatabase();
        homeToggle.setOnAction(e -> showHome());
        farmToggle.setOnAction(e -> showFarm());
        showHome();

        themeSelector.setItems(FXCollections.observableArrayList("horde", "alliance"));
        themeSelector.setValue("horde");
        themeSelector.setOnAction(e -> changeTheme());

        saveSessionButton.setOnAction(this::onSaveSession);

        itemCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().name()));
        qtyCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().quantity()));
        rateCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().dropRate()));

        dateCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().date()));
        durationCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatHMS(d.getValue().durationSeconds())));
        mobsCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().mobsKilled()));
        copperCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().totalCopper()));
        lootInfoCol.setCellFactory(tc -> new TableCell<>() {
            private final Button button = new Button("ℹ️");
            {
                button.setOnAction(evt -> showSessionInfo(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : button);
            }
        });

        new Thread(() -> {
            try {
                List<Instance> instances = api.fetchInstances();
                Platform.runLater(() -> instanceBox.getItems().setAll(instances));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        instanceBox.setOnAction(e -> {
            Instance instance = instanceBox.getValue();
            if (instance == null) return;

            new Thread(() -> {
                try {
                    Instance details = api.fetchInstanceDetails(instance.id());
                    List<Encounter> encounters = api.fetchEncounters(instance.id());
                    Platform.runLater(() -> {
                        updateInstanceInfo(details);
                        encounterBox.getItems().setAll(encounters);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        encounterBox.setOnAction(e -> {
            Encounter encounter = encounterBox.getValue();
            Instance instance = instanceBox.getValue();
            if (encounter == null || instance == null) return;

            new Thread(() -> {
                try {
                    List<LootItem> raw = api.fetchLootFromInstance(instance.id(), encounter.id());
                    List<LootItem> detailed = new ArrayList<>();
                    for (LootItem item : raw) {
                        try {
                            detailed.add(api.fetchItem(item.id()));
                        } catch (Exception ex) {
                            detailed.add(item);
                        }
                    }
                    Platform.runLater(() -> {
                        lootTable.setItems(FXCollections.observableArrayList(detailed));
                        updateEncounterInfo(encounter);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        startStopButton.setOnAction(e -> onStartStop());
        infoButton.setOnAction(this::onSessionInfo);
        loadAllSessions();
        setupSearchFilter();
    }

    private void updateInstanceInfo(Instance instance) {
        instanceInfoBox.getChildren().clear();
        instanceInfoBox.getChildren().addAll(
                new Label("Name: " + instance.name()),
                new Label("Map: " + instance.map()),
                new Label("Description:"),
                new TextArea(instance.description()) {{
                    setWrapText(true);
                    setEditable(false);
                    setMaxHeight(100);
                }}
        );

        if (instance.image() != null) {
            ImageView imageView = new ImageView(new Image(instance.image(), true));
            imageView.setFitWidth(300);
            imageView.setPreserveRatio(true);
            instanceInfoBox.getChildren().add(imageView);
        }
    }

    private void updateEncounterInfo(Encounter encounter) {
        encounterInfoBox.getChildren().clear();
        encounterInfoBox.getChildren().addAll(
                new Label("Name: " + encounter.name()),
                new Label("ID:   " + encounter.id())
        );

        new Thread(() -> {
            try {
                String imageUrl = api.fetchEncounterImage(encounter.id());
                if (imageUrl != null) {
                    ImageView imageView = new ImageView(new Image(imageUrl, true));
                    imageView.setFitWidth(300);
                    imageView.setPreserveRatio(true);
                    Platform.runLater(() -> encounterInfoBox.getChildren().add(imageView));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void changeTheme() {
        String selected = themeSelector.getValue();
        if (selected == null || contentStack.getScene() == null) return;

        Scene scene = contentStack.getScene();
        scene.getStylesheets().clear();
        String path = "/styles/theme-" + selected.toLowerCase() + ".css";
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(path)).toExternalForm());

        Platform.runLater(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
        });
    }

    private void showHome() {
        homeView.setVisible(true);
        farmView.setVisible(false);
    }

    private void showFarm() {
        homeView.setVisible(false);
        farmView.setVisible(true);
    }

    @FXML
    public void onSaveSession(ActionEvent event) {
        try {
            LootLoggerParser.ParsedData data = LootLoggerParser.parse();
            Session session = new Session(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    farmSeconds,
                    data.mobsKilled(),
                    data.totalCopper(),
                    data.loot()
            );
            DatabaseHelper.insertSession(session);
            loadAllSessions();
            new Alert(Alert.AlertType.INFORMATION, "Session saved!").showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to save session: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onStartStop() {
        if (!farmRunning) {
            farmRunning = true;
            farmSeconds = 0;
            startStopButton.setText("⏹ Stop");
            timerLabel.setText("00:00:00");
            farmTimer = new Timer(true);
            farmTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    farmSeconds++;
                    Platform.runLater(() -> timerLabel.setText(formatHMS(farmSeconds)));
                }
            }, 1000, 1000);
        } else {
            farmRunning = false;
            startStopButton.setText("▶️ Start");
            if (farmTimer != null) farmTimer.cancel();
        }
    }

    @FXML
    private void onSessionInfo(ActionEvent event) {
        Session session = sessionTable.getSelectionModel().getSelectedItem();
        if (session != null) showSessionInfo(session);
    }

    private void showSessionInfo(Session session) {
        StringBuilder text = new StringBuilder();
        text.append("📅 Date: ").append(session.date()).append("\n");
        text.append("⏱ Duration: ").append(formatHMS(session.durationSeconds())).append("\n");
        text.append("💀 Mobs: ").append(session.mobsKilled()).append("\n");
        text.append("💰 Copper: ").append(session.totalCopper()).append(" (")
                .append(session.totalCopper() / 10_000).append("g ")
                .append((session.totalCopper() / 100) % 100).append("s ")
                .append(session.totalCopper() % 100).append("c)\n");

        if (!session.loot().isEmpty()) {
            text.append("\n📦 Loot:\n");
            for (String item : session.loot()) {
                text.append("• ").append(item).append("\n");
            }
        }

        TextArea textArea = new TextArea(text.toString());
        textArea.setEditable(false);

        Stage dialog = new Stage();
        dialog.initOwner(farmView.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Session Details");
        dialog.setScene(new Scene(textArea, 400, 300));
        dialog.showAndWait();
    }

    private void loadAllSessions() {
        allSessions.setAll(DatabaseHelper.getAllSessions());
    }

    private void setupSearchFilter() {
        filteredSessions = new FilteredList<>(allSessions, session -> true);

        searchField.textProperty().addListener((obs, previous, text) -> {
            String lower = text == null ? "" : text.toLowerCase();
            filteredSessions.setPredicate(session ->
                    session.date().toLowerCase().contains(lower)
                            || Integer.toString(session.mobsKilled()).contains(lower)
                            || Integer.toString(session.totalCopper()).contains(lower));
        });

        SortedList<Session> sorted = new SortedList<>(filteredSessions);
        sorted.comparatorProperty().bind(sessionTable.comparatorProperty());
        sessionTable.setItems(sorted);
    }

    private String formatHMS(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
