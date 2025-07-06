package org.wowmr.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.wowmr.api.BlizzardApiClient;
import org.wowmr.db.DatabaseHelper;
import org.wowmr.db.Session;
import org.wowmr.model.*;
import org.wowmr.util.LootLoggerParser;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private static final File luaFile = new File("D:/WoW 10.2.7 - Firestorm/WoW 10.2.7 - Firestorm/WTF/Account/seriosfrate2002@gmail.com/SavedVariables/LootLogger.lua");
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
            private final Button b = new Button("ℹ️");
            { b.setOnAction(evt -> showSessionInfo(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : b);
            }
        });

        new Thread(() -> {
            try {
                List<Instance> insts = api.fetchInstances();
                Platform.runLater(() -> instanceBox.getItems().setAll(insts));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        instanceBox.setOnAction(e -> {
            Instance inst = instanceBox.getValue();
            if (inst == null) return;
            new Thread(() -> {
                try {
                    Instance details = api.fetchInstanceDetails(inst.id());
                    List<Encounter> encs = api.fetchEncounters(inst.id());
                    Platform.runLater(() -> {
                        updateInstanceInfo(details);
                        encounterBox.getItems().setAll(encs);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        encounterBox.setOnAction(e -> {
            Encounter enc = encounterBox.getValue();
            Instance inst = instanceBox.getValue();
            if (enc == null || inst == null) return;
            new Thread(() -> {
                try {
                    List<LootItem> raw = api.fetchLootFromInstance(inst.id(), enc.id());
                    List<LootItem> detailed = new ArrayList<>();
                    for (LootItem li : raw) {
                        try {
                            detailed.add(api.fetchItem(li.id()));
                        } catch (Exception ex) {
                            detailed.add(li); // fallback
                        }
                    }
                    Platform.runLater(() -> lootTable.setItems(FXCollections.observableArrayList(detailed)));
                    Platform.runLater(() -> updateEncounterInfo(enc));
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

    private void updateInstanceInfo(Instance inst) {
        instanceInfoBox.getChildren().clear();
        instanceInfoBox.getChildren().addAll(
                new Label("Name: " + inst.name()),
                new Label("Map: " + inst.map()),
                new Label("Description:"),
                new TextArea(inst.description()) {{
                    setWrapText(true);
                    setEditable(false);
                    setMaxHeight(100);
                }}
        );
        if (inst.image() != null) {
            ImageView iv = new ImageView(new Image(inst.image(), true));
            iv.setFitWidth(300);
            iv.setPreserveRatio(true);
            instanceInfoBox.getChildren().add(iv);
        }
    }

    private void updateEncounterInfo(Encounter enc) {
        encounterInfoBox.getChildren().clear();
        encounterInfoBox.getChildren().addAll(
                new Label("Name: " + enc.name()),
                new Label("ID:   " + enc.id())
        );

        new Thread(() -> {
            try {
                String imgUrl = api.fetchEncounterImage(enc.id());
                if (imgUrl != null) {
                    ImageView iv = new ImageView(new Image(imgUrl, true));
                    iv.setFitWidth(300);
                    iv.setPreserveRatio(true);
                    Platform.runLater(() -> encounterInfoBox.getChildren().add(iv));
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
    public void onSaveSession(ActionEvent e) {
        try {
            LootLoggerParser.ParsedData data = LootLoggerParser.parse();
            Session s = new Session(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    farmSeconds,
                    data.mobsKilled(),
                    data.totalCopper(),
                    data.loot()
            );
            DatabaseHelper.insertSession(s);
            loadAllSessions();
            new Alert(Alert.AlertType.INFORMATION, "Session saved!").showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to save session: " + ex.getMessage()).showAndWait();
        }
    }



    @FXML private void onStartStop() {
        if (!farmRunning) {
            farmRunning = true;
            farmSeconds = 0;
            startStopButton.setText("⏹ Stop");
            timerLabel.setText("00:00:00");
            farmTimer = new Timer(true);
            farmTimer.scheduleAtFixedRate(new TimerTask() {
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

    @FXML private void onSessionInfo(ActionEvent e) {
        Session s = sessionTable.getSelectionModel().getSelectedItem();
        if (s != null) showSessionInfo(s);
    }

    private void showSessionInfo(Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 Date: ").append(s.date()).append("\n");
        sb.append("⏱ Duration: ").append(formatHMS(s.durationSeconds())).append("\n");
        sb.append("💀 Mobs: ").append(s.mobsKilled()).append("\n");
        sb.append("💰 Copper: ").append(s.totalCopper()).append(" (")
                .append(s.totalCopper() / 10000).append("g ")
                .append((s.totalCopper() / 100) % 100).append("s ")
                .append(s.totalCopper() % 100).append("c)\n");
        if (!s.loot().isEmpty()) {
            sb.append("\n📦 Loot:\n");
            for (String item : s.loot()) {
                sb.append("• ").append(item).append("\n");
            }
        }

        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        Stage dlg = new Stage();
        dlg.initOwner(farmView.getScene().getWindow());
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Session Details");
        dlg.setScene(new Scene(ta, 400, 300));
        dlg.showAndWait();
    }



    private void loadAllSessions() {
        allSessions.setAll(DatabaseHelper.getAllSessions());
    }

    private void setupSearchFilter() {
        filteredSessions = new FilteredList<>(allSessions, p -> true);

        searchField.textProperty().addListener((obs, __, t) -> {
            String lower = t == null ? "" : t.toLowerCase();
            filteredSessions.setPredicate(s ->
                    s.date().toLowerCase().contains(lower) ||
                            Integer.toString(s.mobsKilled()).contains(lower) ||
                            Integer.toString(s.totalCopper()).contains(lower));
        });
        SortedList<Session> sorted = new SortedList<>(filteredSessions);
        sorted.comparatorProperty().bind(sessionTable.comparatorProperty());
        sessionTable.setItems(sorted);
    }

    private String formatHMS(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
