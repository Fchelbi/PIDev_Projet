package controllers;

import entities.Formation;
import entities.Quiz;
import entities.Question;
import entities.Reponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import services.FormationService;
import services.QuizService;
import services.QuestionService;
import services.ReponseService;
import services.QuizGeneratorAPI;
import services.YouTubeAPIService;
import utils.VideoPlayerUtil;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class FormationController implements Initializable {

    // ═══════════════════════════════════
    //  FXML FIELDS
    // ═══════════════════════════════════
    @FXML private TableView<Formation> tvFormations;
    @FXML private TableColumn<Formation, String> colTitle;
    @FXML private TableColumn<Formation, String> colDescription;
    @FXML private TableColumn<Formation, String> colCategory;
    @FXML private TableColumn<Formation, Void> colVideo;
    @FXML private TableColumn<Formation, Void> colQuiz;
    @FXML private TableColumn<Formation, Void> colActions;
    @FXML private TextField tfSearch;
    @FXML private Label lblTotal;

    @FXML private StackPane formationContent;
    @FXML private VBox tableSection;
    @FXML private VBox formSection;
    @FXML private VBox quizSection;
    @FXML private VBox videoSection;
    @FXML private ScrollPane formScrollPane;
    @FXML private ScrollPane quizScrollPane;

    @FXML private TextField tfTitle;
    @FXML private TextArea taDescription;
    @FXML private TextField tfVideoUrl;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbCoach;       // NEW: Coach selector
    @FXML private Label lblError;
    @FXML private Label lblFormTitle;
    @FXML private Button btnSave;

    @FXML private TextField tfQuizTitle;
    @FXML private TextField tfPassingScore;
    @FXML private Label lblQuizTitle;
    @FXML private Label lblQuizError;
    @FXML private Button btnSaveQuiz;
    @FXML private VBox quizFormArea;
    @FXML private VBox questionsArea;
    @FXML private VBox questionsList;

    // ═══════════════════════════════════
    //  PRIVATE FIELDS
    // ═══════════════════════════════════
    private FormationService formationService;
    private QuizService quizService;
    private QuestionService questionService;
    private ReponseService reponseService;
    private YouTubeAPIService youtubeService;
    private QuizGeneratorAPI quizGenerator;

    private ObservableList<Formation> formationList;
    private FilteredList<Formation> filteredList;
    private Formation editingFormation = null;
    private boolean isEditMode = false;
    private Formation currentQuizFormation = null;
    private VBox currentVideoContainer = null;

    // ═══════════════════════════════════
    //  WHO IS USING THIS CONTROLLER
    //  0 = admin (sees all), >0 = coach (sees only his)
    // ═══════════════════════════════════
    private int currentCoachId = 0; // 0 = admin mode

    public void setCoachMode(int coachId) {
        this.currentCoachId = coachId;
        loadFormations();
    }

    // ═══════════════════════════════════
    //  VALIDATION CONSTANTS
    // ═══════════════════════════════════
    private static final int TITLE_MIN_LENGTH = 3;
    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESC_MIN_LENGTH = 10;
    private static final int DESC_MAX_LENGTH = 500;
    private static final int QUESTION_MIN_LENGTH = 5;
    private static final int MIN_PASSING_SCORE = 0;
    private static final int MAX_PASSING_SCORE = 100;
    private static final int MIN_POINTS = 1;
    private static final int MAX_POINTS = 100;

    private final String[] CATEGORIES = {
            "Communication", "Leadership", "Teamwork",
            "Self-Improvement", "Public Speaking",
            "Conflict Resolution", "Emotional Intelligence",
            "Time Management"
    };

    // Temporary coach list until User module is integrated
    private final String[] COACHES = {
            "Non assigné",
            "Coach #1",
            "Coach #2",
            "Coach #3"
    };

    // ═══════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();
        quizService = new QuizService();
        questionService = new QuestionService();
        reponseService = new ReponseService();
        youtubeService = new YouTubeAPIService();
        quizGenerator = new QuizGeneratorAPI();

        if (cbCategory != null) {
            cbCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
        }
        if (cbCoach != null) {
            cbCoach.setItems(FXCollections.observableArrayList(COACHES));
            cbCoach.setValue("Non assigné");
        }

        if (tvFormations != null) {
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
            setupVideoColumn();
            setupQuizColumn();
            setupActionsColumn();
            loadFormations();
        }

        setupValidationListeners();
    }

    // ═══════════════════════════════════
    //  LOAD DATA (filtered by coach if needed)
    // ═══════════════════════════════════
    private void loadFormations() {
        try {
            List<Formation> all;
            if (currentCoachId > 0) {
                // Coach mode: only his formations
                all = formationService.selectByCoach(currentCoachId);
            } else {
                // Admin mode: all formations
                all = formationService.selectALL();
            }
            formationList = FXCollections.observableArrayList(all);
            filteredList = new FilteredList<>(formationList, p -> true);
            tvFormations.setItems(filteredList);
            lblTotal.setText("Total: " + formationList.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ═══════════════════════════════════
    //  REAL-TIME VALIDATION
    // ═══════════════════════════════════
    private void setupValidationListeners() {
        if (tfTitle != null) {
            tfTitle.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > TITLE_MAX_LENGTH) tfTitle.setText(oldVal);
                validateFieldRealTime(tfTitle, validateTitle(newVal));
            });
        }
        if (taDescription != null) {
            taDescription.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > DESC_MAX_LENGTH) taDescription.setText(oldVal);
                validateFieldRealTime(taDescription, validateDescription(newVal));
            });
        }
        if (tfVideoUrl != null) {
            tfVideoUrl.textProperty().addListener((obs, oldVal, newVal) ->
                    validateFieldRealTime(tfVideoUrl, validateVideoUrl(newVal)));
        }
        if (tfPassingScore != null) {
            tfPassingScore.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*"))
                    tfPassingScore.setText(newVal.replaceAll("[^\\d]", ""));
            });
        }
    }

    private void validateFieldRealTime(Control field, String error) {
        if (error == null) {
            field.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #00b894; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        } else {
            field.setStyle("-fx-background-color: #fff5f5; -fx-border-color: #d63031; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        }
    }

    // ═══════════════════════════════════
    //  FIELD VALIDATORS
    // ═══════════════════════════════════
    private String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) return "Le titre est obligatoire";
        if (title.trim().length() < TITLE_MIN_LENGTH)
            return "Le titre doit contenir au moins " + TITLE_MIN_LENGTH + " caractères";
        if (title.trim().length() > TITLE_MAX_LENGTH)
            return "Le titre ne peut pas dépasser " + TITLE_MAX_LENGTH + " caractères";
        return null;
    }

    private String validateDescription(String desc) {
        if (desc == null || desc.trim().isEmpty()) return "La description est obligatoire";
        if (desc.trim().length() < DESC_MIN_LENGTH)
            return "La description doit contenir au moins " + DESC_MIN_LENGTH + " caractères";
        if (desc.trim().length() > DESC_MAX_LENGTH)
            return "La description ne peut pas dépasser " + DESC_MAX_LENGTH + " caractères";
        return null;
    }

    private String validateVideoUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "L'URL vidéo est obligatoire";
        String trimmed = url.trim();
        if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) return null;
        File file = new File(trimmed);
        if (!file.exists()) return "Fichier non trouvé: " + trimmed;
        String lower = trimmed.toLowerCase();
        if (!lower.endsWith(".mp4") && !lower.endsWith(".avi") &&
                !lower.endsWith(".mkv") && !lower.endsWith(".mov") &&
                !lower.endsWith(".webm"))
            return "Format non supporté. Utilisez MP4, AVI, MKV, MOV ou WEBM";
        return null;
    }

    private String validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) return "La catégorie est obligatoire";
        return null;
    }

    // ═══════════════════════════════════
    //  TABLE COLUMNS
    // ═══════════════════════════════════
    private void setupVideoColumn() {
        colVideo.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("▶️");
            {
                btn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-background-radius: 5;");
                btn.setTooltip(new Tooltip("Voir la vidéo"));
                btn.setOnAction(e -> showVideoPlayer(
                        getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                if (!empty) setAlignment(Pos.CENTER);
            }
        });
    }

    private void setupQuizColumn() {
        colQuiz.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("📝");
            {
                btn.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-background-radius: 5;");
                btn.setTooltip(new Tooltip("Gérer le quiz"));
                btn.setOnAction(e -> showQuizManager(
                        getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                if (!empty) setAlignment(Pos.CENTER);
            }
        });
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑️");
            private final HBox buttons = new HBox(8, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color: #fdcb6e; -fx-cursor: hand; " +
                        "-fx-background-radius: 5;");
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setStyle("-fx-background-color: #d63031; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-background-radius: 5;");
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                buttons.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> showEditForm(
                        getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(
                        getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    // ═══════════════════════════════════
    //  SECTION SWITCHING
    // ═══════════════════════════════════
    private void hideAllSections() {
        tableSection.setVisible(false);
        tableSection.setManaged(false);
        if (formScrollPane != null) {
            formScrollPane.setVisible(false);
            formScrollPane.setManaged(false);
        }
        if (videoSection != null) {
            videoSection.setVisible(false);
            videoSection.setManaged(false);
        }
        if (quizScrollPane != null) {
            quizScrollPane.setVisible(false);
            quizScrollPane.setManaged(false);
        }
    }

    @FXML
    public void showTable() {
        hideAllSections();
        tableSection.setVisible(true);
        tableSection.setManaged(true);
        clearForm();
        loadFormations();
        if (currentVideoContainer != null) {
            VideoPlayerUtil.stopMedia(currentVideoContainer);
            currentVideoContainer = null;
        }
    }

    @FXML
    private void showAddForm() {
        hideAllSections();
        isEditMode = false;
        editingFormation = null;
        lblFormTitle.setText("➕ Ajouter Formation");
        btnSave.setText("✅ Enregistrer");
        clearForm();

        // If coach mode, hide coach selector and auto-assign
        if (cbCoach != null) {
            if (currentCoachId > 0) {
                cbCoach.setVisible(false);
                cbCoach.setManaged(false);
            } else {
                cbCoach.setVisible(true);
                cbCoach.setManaged(true);
                cbCoach.setValue("Non assigné");
            }
        }

        formScrollPane.setVisible(true);
        formScrollPane.setManaged(true);
    }

    private void showEditForm(Formation f) {
        hideAllSections();
        isEditMode = true;
        editingFormation = f;
        lblFormTitle.setText("✏️ Modifier Formation");
        btnSave.setText("💾 Mettre à jour");
        tfTitle.setText(f.getTitle());
        taDescription.setText(f.getDescription());
        tfVideoUrl.setText(f.getVideoUrl());
        cbCategory.setValue(f.getCategory());
        if (cbCoach != null) {
            if (currentCoachId > 0) {
                cbCoach.setVisible(false);
                cbCoach.setManaged(false);
            } else {
                cbCoach.setVisible(true);
                cbCoach.setManaged(true);
                cbCoach.setValue(f.getCoachId() > 0
                        ? "Coach #" + f.getCoachId() : "Non assigné");
            }
        }
        lblError.setText("");
        resetFieldStyles();
        formScrollPane.setVisible(true);
        formScrollPane.setManaged(true);
    }

    private void resetFieldStyles() {
        String defaultStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #dfe6e9; " +
                "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;";
        if (tfTitle != null) tfTitle.setStyle(defaultStyle);
        if (taDescription != null) taDescription.setStyle(defaultStyle);
        if (tfVideoUrl != null) tfVideoUrl.setStyle(defaultStyle);
        if (cbCategory != null) cbCategory.setStyle(defaultStyle);
    }

    // ═══════════════════════════════════
    //  VIDEO PLAYER
    // ═══════════════════════════════════
    private void showVideoPlayer(Formation f) {
        hideAllSections();
        if (currentVideoContainer != null) {
            VideoPlayerUtil.stopMedia(currentVideoContainer);
            currentVideoContainer = null;
        }

        String videoUrl = f.getVideoUrl();
        videoSection.getChildren().clear();

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Button btnBack = new Button("↩️ Retour");
        btnBack.getStyleClass().add("btn-back");
        btnBack.setOnAction(e -> {
            if (currentVideoContainer != null) {
                VideoPlayerUtil.stopMedia(currentVideoContainer);
                currentVideoContainer = null;
            }
            showTable();
        });

        Label title = new Label("🎬 " + f.getTitle());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        header.getChildren().addAll(btnBack, title);

        VBox playerContainer;
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            playerContainer = VideoPlayerUtil.createNoVideoMessage();
        } else if (VideoPlayerUtil.isYouTubeUrl(videoUrl)) {
            playerContainer = VideoPlayerUtil.createYouTubePlayer(videoUrl);
        } else {
            File file = new File(videoUrl);
            if (!file.exists()) {
                playerContainer = VideoPlayerUtil.createErrorMessage(
                        "Fichier non trouvé", videoUrl);
            } else if (videoUrl.toLowerCase().endsWith(".mp4")) {
                playerContainer = VideoPlayerUtil.createLocalPlayer(videoUrl);
            } else {
                playerContainer = VideoPlayerUtil.createWebViewLocalPlayer(videoUrl);
            }
        }

        currentVideoContainer = playerContainer;
        VBox.setVgrow(playerContainer, Priority.ALWAYS);

        Label desc = new Label(f.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #636e72; -fx-padding: 10 0 0 0;");

        videoSection.getChildren().addAll(header, playerContainer, desc);
        videoSection.setVisible(true);
        videoSection.setManaged(true);
    }

    // ═══════════════════════════════════
    //  CHOOSE LOCAL VIDEO
    // ═══════════════════════════════════
    @FXML
    private void handleChooseVideo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une vidéo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Vidéos MP4 (Recommandé)", "*.mp4"),
                new FileChooser.ExtensionFilter("Toutes les vidéos",
                        "*.mp4", "*.avi", "*.mkv", "*.mov", "*.webm"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        fc.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fc.showOpenDialog(tfVideoUrl.getScene().getWindow());
        if (file != null) tfVideoUrl.setText(file.getAbsolutePath());
    }

    // ═══════════════════════════════════
    //  YOUTUBE SEARCH API
    // ═══════════════════════════════════
    @FXML
    private void handleSearchYouTube() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🔍 Rechercher sur YouTube");
        dialog.setHeaderText("Rechercher des vidéos de soft skills");

        ButtonType selectBtn = new ButtonType("Sélectionner", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectBtn, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(500);

        TextField tfSearchQuery = new TextField();
        tfSearchQuery.setPromptText("Ex: communication skills, leadership...");
        tfSearchQuery.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label lblSearchStatus = new Label("");
        lblSearchStatus.setStyle("-fx-text-fill: #636e72;");

        ListView<YouTubeAPIService.YouTubeVideo> listResults = new ListView<>();
        listResults.setPrefHeight(350);
        listResults.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(YouTubeAPIService.YouTubeVideo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    VBox cell = new VBox(3);
                    cell.setPadding(new Insets(5));
                    Label tl = new Label("🎬 " + item.title);
                    tl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                    tl.setWrapText(true);
                    Label cl = new Label("📺 " + item.channelTitle);
                    cl.setStyle("-fx-text-fill: #636e72; -fx-font-size: 11px;");
                    cell.getChildren().addAll(tl, cl);
                    setGraphic(cell);
                }
            }
        });

        Button btnDoSearch = new Button("🔍 Chercher");
        btnDoSearch.setStyle("-fx-background-color: #d63031; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; " +
                "-fx-cursor: hand;");
        btnDoSearch.setOnAction(e -> {
            String query = tfSearchQuery.getText().trim();
            if (query.isEmpty()) { lblSearchStatus.setText("❌ Entrez un mot-clé"); return; }
            lblSearchStatus.setText("⏳ Recherche en cours...");
            listResults.getItems().clear();
            new Thread(() -> {
                try {
                    List<YouTubeAPIService.YouTubeVideo> results =
                            youtubeService.searchVideos(query, 10);
                    javafx.application.Platform.runLater(() -> {
                        listResults.getItems().addAll(results);
                        lblSearchStatus.setText("✅ " + results.size() + " résultats");
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() ->
                            lblSearchStatus.setText("❌ Erreur: " + ex.getMessage()));
                }
            }).start();
        });
        tfSearchQuery.setOnAction(e -> btnDoSearch.fire());

        HBox searchRow = new HBox(10, tfSearchQuery, btnDoSearch);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tfSearchQuery, Priority.ALWAYS);

        content.getChildren().addAll(searchRow, lblSearchStatus, listResults);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == selectBtn) {
                YouTubeAPIService.YouTubeVideo selected =
                        listResults.getSelectionModel().getSelectedItem();
                return selected != null ? selected.youtubeUrl : null;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(youtubeUrl -> {
            if (tfVideoUrl != null) tfVideoUrl.setText(youtubeUrl);
        });
    }

    // ═══════════════════════════════════
    //  SAVE FORMATION (with coach_id)
    // ═══════════════════════════════════
    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        // Determine coach_id
        int coachId = currentCoachId; // If coach mode, use his ID
        if (currentCoachId == 0 && cbCoach != null) {
            // Admin mode: get from dropdown
            String selected = cbCoach.getValue();
            if (selected != null && selected.startsWith("Coach #")) {
                try {
                    coachId = Integer.parseInt(selected.replace("Coach #", ""));
                } catch (NumberFormatException e) {
                    coachId = 0;
                }
            }
        }

        if (isEditMode) {
            editingFormation.setTitle(tfTitle.getText().trim());
            editingFormation.setDescription(taDescription.getText().trim());
            editingFormation.setVideoUrl(tfVideoUrl.getText().trim());
            editingFormation.setCategory(cbCategory.getValue());
            editingFormation.setCoachId(coachId);
            try {
                formationService.updateOne(editingFormation);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Formation modifiée avec succès! ✅");
                showTable();
            } catch (SQLException e) {
                lblError.setText("❌ Erreur: " + e.getMessage());
            }
        } else {
            Formation f = new Formation();
            f.setTitle(tfTitle.getText().trim());
            f.setDescription(taDescription.getText().trim());
            f.setVideoUrl(tfVideoUrl.getText().trim());
            f.setCategory(cbCategory.getValue());
            f.setCoachId(coachId);
            try {
                formationService.insertOne(f);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Formation ajoutée avec succès! ✅");
                showTable();
            } catch (SQLException e) {
                lblError.setText("❌ Erreur: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════
    //  DELETE FORMATION
    // ═══════════════════════════════════
    private void handleDelete(Formation f) {
        Optional<ButtonType> r = showConfirm(
                "⚠️ Supprimer la formation \"" + f.getTitle() +
                        "\"?\n\nCette action est irréversible!");
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                Quiz quiz = quizService.selectByFormation(f.getId());
                if (quiz != null) {
                    List<Question> questions = questionService.selectByQuiz(quiz.getId());
                    for (Question q : questions) {
                        for (Reponse rep : reponseService.selectByQuestion(q.getId()))
                            reponseService.deleteOne(rep);
                        questionService.deleteOne(q);
                    }
                    quizService.deleteOne(quiz);
                }
                formationService.deleteOne(f);
                showAlert(Alert.AlertType.INFORMATION, "Supprimé",
                        "Formation supprimée! 🗑️");
                loadFormations();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════
    //  QUIZ MANAGER
    // ═══════════════════════════════════
    private void showQuizManager(Formation f) {
        hideAllSections();
        currentQuizFormation = f;
        lblQuizTitle.setText("📝 Quiz — " + f.getTitle());
        lblQuizError.setText("");
        try {
            Quiz existing = quizService.selectByFormation(f.getId());
            if (existing != null) {
                tfQuizTitle.setText(existing.getTitle());
                tfPassingScore.setText(String.valueOf(existing.getPassingScore()));
                btnSaveQuiz.setText("💾 Mettre à jour");
                loadQuestions(existing.getId());
                questionsArea.setVisible(true);
                questionsArea.setManaged(true);
            } else {
                tfQuizTitle.clear();
                tfPassingScore.setText("70");
                btnSaveQuiz.setText("✅ Créer Quiz");
                questionsArea.setVisible(false);
                questionsArea.setManaged(false);
            }
        } catch (SQLException e) {
            lblQuizError.setText("❌ " + e.getMessage());
        }
        quizScrollPane.setVisible(true);
        quizScrollPane.setManaged(true);
    }

    @FXML
    private void handleSaveQuiz() {
        String quizTitle = tfQuizTitle.getText().trim();
        if (quizTitle.isEmpty()) {
            lblQuizError.setText("❌ Le titre du quiz est obligatoire");
            return;
        }
        if (quizTitle.length() < TITLE_MIN_LENGTH) {
            lblQuizError.setText("❌ Le titre doit contenir au moins " +
                    TITLE_MIN_LENGTH + " caractères");
            return;
        }
        int score;
        try {
            score = Integer.parseInt(tfPassingScore.getText().trim());
            if (score < MIN_PASSING_SCORE || score > MAX_PASSING_SCORE) {
                lblQuizError.setText("❌ Le score doit être entre " +
                        MIN_PASSING_SCORE + " et " + MAX_PASSING_SCORE);
                return;
            }
        } catch (NumberFormatException e) {
            lblQuizError.setText("❌ Le score doit être un nombre entier");
            return;
        }
        try {
            Quiz existing = quizService.selectByFormation(currentQuizFormation.getId());
            if (existing != null) {
                existing.setTitle(quizTitle);
                existing.setPassingScore(score);
                quizService.updateOne(existing);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz mis à jour! ✅");
            } else {
                Quiz q = new Quiz();
                q.setFormationId(currentQuizFormation.getId());
                q.setTitle(quizTitle);
                q.setPassingScore(score);
                quizService.insertOne(q);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz créé! ✅");
            }
            showQuizManager(currentQuizFormation);
        } catch (SQLException e) {
            lblQuizError.setText("❌ " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteQuiz() {
        Optional<ButtonType> r = showConfirm("⚠️ Supprimer ce quiz?");
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                Quiz quiz = quizService.selectByFormation(currentQuizFormation.getId());
                if (quiz != null) {
                    for (Question q : questionService.selectByQuiz(quiz.getId())) {
                        for (Reponse rep : reponseService.selectByQuestion(q.getId()))
                            reponseService.deleteOne(rep);
                        questionService.deleteOne(q);
                    }
                    quizService.deleteOne(quiz);
                    showAlert(Alert.AlertType.INFORMATION, "Supprimé", "Quiz supprimé! 🗑️");
                }
                showTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════
    //  QUESTIONS
    // ═══════════════════════════════════
    private void loadQuestions(int quizId) {
        questionsList.getChildren().clear();
        try {
            List<Question> questions = questionService.selectByQuiz(quizId);
            int num = 1;
            for (Question q : questions)
                questionsList.getChildren().add(createQuestionBox(q, num++));
            if (questions.isEmpty()) {
                Label emptyLabel = new Label("Aucune question. Cliquez '➕' ou '🤖'");
                emptyLabel.setStyle("-fx-text-fill: #636e72; -fx-font-style: italic;");
                questionsList.getChildren().add(emptyLabel);
            }
        } catch (SQLException e) {
            questionsList.getChildren().add(new Label("❌ Erreur: " + e.getMessage()));
        }
    }

    private VBox createQuestionBox(Question q, int num) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; " +
                "-fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblQ = new Label("Q" + num + ": " + q.getQuestionText());
        lblQ.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        lblQ.setWrapText(true);
        lblQ.setMaxWidth(400);
        Label lblPts = new Label("(" + q.getPoints() + " pts)");
        lblPts.setStyle("-fx-text-fill: #636e72;");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnEditQ = new Button("✏️");
        btnEditQ.setStyle("-fx-background-color: #fdcb6e; -fx-cursor: hand; -fx-background-radius: 5;");
        btnEditQ.setOnAction(e -> editQuestion(q));
        Button btnDelQ = new Button("🗑️");
        btnDelQ.setStyle("-fx-background-color: #d63031; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnDelQ.setOnAction(e -> deleteQuestion(q));

        header.getChildren().addAll(lblQ, lblPts, spacer, btnEditQ, btnDelQ);
        box.getChildren().add(header);

        try {
            for (Reponse r : reponseService.selectByQuestion(q.getId())) {
                HBox respRow = new HBox(8);
                respRow.setAlignment(Pos.CENTER_LEFT);
                Label lblResp = new Label((r.isCorrect() ? "✅ " : "❌ ") + r.getOptionText());
                lblResp.setStyle(r.isCorrect() ?
                        "-fx-text-fill: #00b894; -fx-font-weight: bold;" :
                        "-fx-text-fill: #636e72;");
                lblResp.setMaxWidth(350);
                lblResp.setWrapText(true);

                Button btnEditR = new Button("✏️");
                btnEditR.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 10px;");
                btnEditR.setOnAction(e -> editReponse(r, q));
                Button btnDelR = new Button("✕");
                btnDelR.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-cursor: hand;");
                btnDelR.setOnAction(e -> deleteReponse(r));

                respRow.getChildren().addAll(lblResp, btnEditR, btnDelR);
                box.getChildren().add(respRow);
            }
        } catch (SQLException e) {
            box.getChildren().add(new Label("❌ Erreur réponses"));
        }

        Button btnAddR = new Button("+ Ajouter réponse");
        btnAddR.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a90d9; -fx-cursor: hand;");
        btnAddR.setOnAction(e -> addReponse(q));
        box.getChildren().add(btnAddR);
        return box;
    }

    @FXML
    private void handleAddQuestion() {
        try {
            Quiz quiz = quizService.selectByFormation(currentQuizFormation.getId());
            if (quiz == null) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Créez le quiz d'abord!");
                return;
            }
            Dialog<Question> dialog = new Dialog<>();
            dialog.setTitle("Nouvelle Question");
            ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            content.setPrefWidth(400);
            TextArea taQ = new TextArea();
            taQ.setPromptText("Question (min " + QUESTION_MIN_LENGTH + " car.)...");
            taQ.setPrefRowCount(3);
            taQ.setWrapText(true);
            TextField tfPts = new TextField("5");
            tfPts.setPromptText("Points (1-100)");
            tfPts.textProperty().addListener((obs, o, n) -> {
                if (!n.matches("\\d*")) tfPts.setText(n.replaceAll("[^\\d]", ""));
            });
            Label lblDialogError = new Label();
            lblDialogError.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
            content.getChildren().addAll(new Label("Question:"), taQ,
                    new Label("Points:"), tfPts, lblDialogError);
            dialog.getDialogPane().setContent(content);

            Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                StringBuilder err = new StringBuilder();
                if (taQ.getText().trim().length() < QUESTION_MIN_LENGTH)
                    err.append("• Min ").append(QUESTION_MIN_LENGTH).append(" caractères\n");
                try {
                    int pts = Integer.parseInt(tfPts.getText().trim());
                    if (pts < MIN_POINTS || pts > MAX_POINTS)
                        err.append("• Points entre ").append(MIN_POINTS).append("-").append(MAX_POINTS).append("\n");
                } catch (NumberFormatException ex) { err.append("• Points invalides\n"); }
                if (err.length() > 0) { lblDialogError.setText(err.toString()); event.consume(); }
            });

            dialog.setResultConverter(btn -> {
                if (btn == saveBtn) {
                    Question q = new Question();
                    q.setQuizId(quiz.getId());
                    q.setQuestionText(taQ.getText().trim());
                    q.setPoints(Integer.parseInt(tfPts.getText().trim()));
                    return q;
                }
                return null;
            });

            dialog.showAndWait().ifPresent(q -> {
                try { questionService.insertOne(q); showQuizManager(currentQuizFormation); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ═══════════════════════════════════
    //  AI QUIZ GENERATOR
    // ═══════════════════════════════════
    @FXML
    private void handleAutoGenerateQuestions() {
        try {
            Quiz quiz = quizService.selectByFormation(currentQuizFormation.getId());
            if (quiz == null) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Créez le quiz d'abord!");
                return;
            }
            TextInputDialog countDialog = new TextInputDialog("5");
            countDialog.setTitle("Génération Automatique");
            countDialog.setHeaderText("🤖 Générer des questions avec l'IA");
            countDialog.setContentText("Nombre de questions (1-20):");
            Optional<String> countResult = countDialog.showAndWait();
            if (countResult.isEmpty()) return;

            int count;
            try {
                count = Integer.parseInt(countResult.get().trim());
                if (count < 1 || count > 20) {
                    showAlert(Alert.AlertType.WARNING, "Attention", "Choisissez 1-20");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Nombre invalide");
                return;
            }

            lblQuizError.setText("⏳ Génération IA en cours...");
            lblQuizError.setStyle("-fx-text-fill: #fdcb6e;");

            int finalCount = count;
            new Thread(() -> {
                try {
                    List<QuizGeneratorAPI.GeneratedQuestion> generated =
                            quizGenerator.generateQuestions(
                                    currentQuizFormation.getTitle(),
                                    currentQuizFormation.getDescription(), finalCount);
                    javafx.application.Platform.runLater(() -> {
                        if (generated.isEmpty()) {
                            lblQuizError.setText("❌ Aucune question générée");
                            lblQuizError.setStyle("-fx-text-fill: #d63031;");
                            return;
                        }
                        showGeneratedQuestionsPreview(quiz, generated);
                        lblQuizError.setText("");
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        lblQuizError.setText("❌ Erreur API: " + e.getMessage());
                        lblQuizError.setStyle("-fx-text-fill: #d63031;");
                    });
                }
            }).start();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showGeneratedQuestionsPreview(Quiz quiz,
                                               List<QuizGeneratorAPI.GeneratedQuestion> generated) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("🤖 Questions Générées");
        ButtonType addBtn = new ButtonType("✅ Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(600);
        List<CheckBox> checkBoxes = new ArrayList<>();
        int num = 1;
        for (QuizGeneratorAPI.GeneratedQuestion gq : generated) {
            VBox qBox = new VBox(5);
            qBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 8;");
            CheckBox cb = new CheckBox("Q" + num + ": " + gq.questionText + " (" + gq.points + " pts)");
            cb.setSelected(true);
            cb.setWrapText(true);
            cb.setStyle("-fx-font-weight: bold;");
            checkBoxes.add(cb);
            VBox optionsBox = new VBox(2);
            optionsBox.setPadding(new Insets(0, 0, 0, 25));
            for (int i = 0; i < gq.options.size(); i++) {
                String prefix = (i == gq.correctIndex) ? "  ✅ " : "  ❌ ";
                Label opt = new Label(prefix + (char) ('A' + i) + ") " + gq.options.get(i));
                opt.setStyle(i == gq.correctIndex ? "-fx-text-fill: #00b894; -fx-font-weight: bold;" : "-fx-text-fill: #636e72;");
                opt.setWrapText(true);
                optionsBox.getChildren().add(opt);
            }
            qBox.getChildren().addAll(cb, optionsBox);
            content.getChildren().add(qBox);
            num++;
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        dialog.getDialogPane().setContent(scroll);
        dialog.setResultConverter(btn -> btn == addBtn);

        dialog.showAndWait().ifPresent(ok -> {
            if (!ok) return;
            int added = 0;
            for (int i = 0; i < generated.size(); i++) {
                if (!checkBoxes.get(i).isSelected()) continue;
                QuizGeneratorAPI.GeneratedQuestion gq = generated.get(i);
                try {
                    Question q = new Question();
                    q.setQuizId(quiz.getId());
                    q.setQuestionText(gq.questionText);
                    q.setPoints(gq.points);
                    questionService.insertOne(q);
                    List<Question> allQ = questionService.selectByQuiz(quiz.getId());
                    int questionId = allQ.get(allQ.size() - 1).getId();
                    for (int j = 0; j < gq.options.size(); j++) {
                        Reponse rep = new Reponse();
                        rep.setQuestionId(questionId);
                        rep.setOptionText(gq.options.get(j));
                        rep.setCorrect(j == gq.correctIndex);
                        reponseService.insertOne(rep);
                    }
                    added++;
                } catch (SQLException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
            showAlert(Alert.AlertType.INFORMATION, "Succès", added + " questions ajoutées! 🤖✅");
            showQuizManager(currentQuizFormation);
        });
    }

    // ═══════════════════════════════════
    //  EDIT/DELETE QUESTION & RESPONSE
    // ═══════════════════════════════════
    private void editQuestion(Question q) {
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle("Modifier Question");
        ButtonType saveBtn = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        TextArea taQ = new TextArea(q.getQuestionText());
        taQ.setPrefRowCount(3);
        taQ.setWrapText(true);
        TextField tfPts = new TextField(String.valueOf(q.getPoints()));
        tfPts.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) tfPts.setText(n.replaceAll("[^\\d]", ""));
        });
        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: red;");
        content.getChildren().addAll(new Label("Question:"), taQ, new Label("Points:"), tfPts, lblErr);
        dialog.getDialogPane().setContent(content);

        Button sb = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        sb.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (taQ.getText().trim().length() < QUESTION_MIN_LENGTH) {
                lblErr.setText("Min " + QUESTION_MIN_LENGTH + " caractères");
                event.consume();
            }
        });
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                q.setQuestionText(taQ.getText().trim());
                try { q.setPoints(Integer.parseInt(tfPts.getText().trim())); }
                catch (NumberFormatException e) { q.setPoints(5); }
                return q;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(updated -> {
            try { questionService.updateOne(updated); showQuizManager(currentQuizFormation); }
            catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        });
    }

    private void deleteQuestion(Question q) {
        Optional<ButtonType> r = showConfirm("Supprimer cette question?");
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                for (Reponse rep : reponseService.selectByQuestion(q.getId()))
                    reponseService.deleteOne(rep);
                questionService.deleteOne(q);
                showQuizManager(currentQuizFormation);
            } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        }
    }

    private void addReponse(Question q) {
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Réponse");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        TextField tfOpt = new TextField();
        tfOpt.setPromptText("Réponse...");
        CheckBox cbCorrect = new CheckBox("Réponse correcte ✅");
        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: red;");
        content.getChildren().addAll(new Label("Réponse:"), tfOpt, cbCorrect, lblErr);
        dialog.getDialogPane().setContent(content);

        Button sb = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        sb.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (tfOpt.getText().trim().isEmpty()) { lblErr.setText("❌ Obligatoire"); event.consume(); }
        });
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                Reponse rep = new Reponse();
                rep.setQuestionId(q.getId());
                rep.setOptionText(tfOpt.getText().trim());
                rep.setCorrect(cbCorrect.isSelected());
                return rep;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(rep -> {
            try { reponseService.insertOne(rep); showQuizManager(currentQuizFormation); }
            catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        });
    }

    private void editReponse(Reponse r, Question q) {
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Modifier Réponse");
        ButtonType saveBtn = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        TextField tfOpt = new TextField(r.getOptionText());
        CheckBox cbCorrect = new CheckBox("Réponse correcte ✅");
        cbCorrect.setSelected(r.isCorrect());
        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: red;");
        content.getChildren().addAll(new Label("Réponse:"), tfOpt, cbCorrect, lblErr);
        dialog.getDialogPane().setContent(content);

        Button sb = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        sb.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (tfOpt.getText().trim().isEmpty()) { lblErr.setText("❌ Obligatoire"); event.consume(); }
        });
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                r.setOptionText(tfOpt.getText().trim());
                r.setCorrect(cbCorrect.isSelected());
                return r;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(updated -> {
            try { reponseService.updateOne(updated); showQuizManager(currentQuizFormation); }
            catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        });
    }

    private void deleteReponse(Reponse r) {
        Optional<ButtonType> confirm = showConfirm("Supprimer cette réponse?");
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            try { reponseService.deleteOne(r); showQuizManager(currentQuizFormation); }
            catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        }
    }

    // ═══════════════════════════════════
    //  SEARCH & REFRESH
    // ═══════════════════════════════════
    @FXML
    private void handleSearch() {
        String kw = tfSearch.getText().toLowerCase().trim();
        filteredList.setPredicate(f -> {
            if (kw.isEmpty()) return true;
            return f.getTitle().toLowerCase().contains(kw) ||
                    f.getDescription().toLowerCase().contains(kw) ||
                    f.getCategory().toLowerCase().contains(kw);
        });
        lblTotal.setText("Total: " + filteredList.size());
    }

    @FXML
    private void handleRefresh() { tfSearch.clear(); loadFormations(); }

    @FXML
    private void handleClear() { clearForm(); }

    private void clearForm() {
        if (tfTitle != null) tfTitle.clear();
        if (taDescription != null) taDescription.clear();
        if (tfVideoUrl != null) tfVideoUrl.clear();
        if (cbCategory != null) cbCategory.getSelectionModel().clearSelection();
        if (cbCoach != null) cbCoach.setValue("Non assigné");
        if (lblError != null) lblError.setText("");
        editingFormation = null;
        isEditMode = false;
        resetFieldStyles();
    }

    // ═══════════════════════════════════
    //  FORM VALIDATION
    // ═══════════════════════════════════
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        boolean hasError = false;
        String errStyle = "-fx-background-color: #fff5f5; -fx-border-color: #d63031; " +
                "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;";
        String okStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #00b894; " +
                "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;";

        String titleErr = validateTitle(tfTitle.getText());
        if (titleErr != null) { errors.append("• ").append(titleErr).append("\n"); tfTitle.setStyle(errStyle); hasError = true; }
        else tfTitle.setStyle(okStyle);

        String descErr = validateDescription(taDescription.getText());
        if (descErr != null) { errors.append("• ").append(descErr).append("\n"); taDescription.setStyle(errStyle); hasError = true; }
        else taDescription.setStyle(okStyle);

        String videoErr = validateVideoUrl(tfVideoUrl.getText());
        if (videoErr != null) { errors.append("• ").append(videoErr).append("\n"); tfVideoUrl.setStyle(errStyle); hasError = true; }
        else tfVideoUrl.setStyle(okStyle);

        String catErr = validateCategory(cbCategory.getValue());
        if (catErr != null) { errors.append("• ").append(catErr).append("\n"); cbCategory.setStyle(errStyle); hasError = true; }
        else cbCategory.setStyle(okStyle);

        if (hasError) lblError.setText(errors.toString());
        else lblError.setText("");
        return !hasError;
    }

    // ═══════════════════════════════════
    //  ALERTS
    // ═══════════════════════════════════
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }
}