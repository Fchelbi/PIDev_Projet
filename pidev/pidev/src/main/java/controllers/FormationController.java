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
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import services.FormationService;
import services.QuizService;
import services.QuestionService;
import services.ReponseService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class FormationController implements Initializable {

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
    @FXML private Label lblError;
    @FXML private Label lblFormTitle;
    @FXML private Button btnSave;

    @FXML private WebView webVideoPlayer;
    @FXML private Label lblVideoTitle;
    @FXML private Label lblVideoDescription;

    @FXML private TextField tfQuizTitle;
    @FXML private TextField tfPassingScore;
    @FXML private Label lblQuizTitle;
    @FXML private Label lblQuizError;
    @FXML private Button btnSaveQuiz;
    @FXML private VBox quizFormArea;
    @FXML private VBox questionsArea;
    @FXML private VBox questionsList;

    private FormationService formationService;
    private QuizService quizService;
    private QuestionService questionService;
    private ReponseService reponseService;

    private ObservableList<Formation> formationList;
    private FilteredList<Formation> filteredList;
    private Formation editingFormation = null;
    private boolean isEditMode = false;
    private Formation currentQuizFormation = null;

    private final String[] CATEGORIES = {
            "Communication", "Leadership", "Teamwork",
            "Self-Improvement", "Public Speaking",
            "Conflict Resolution", "Emotional Intelligence",
            "Time Management"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();
        quizService = new QuizService();
        questionService = new QuestionService();
        reponseService = new ReponseService();

        if (cbCategory != null)
            cbCategory.setItems(FXCollections.observableArrayList(CATEGORIES));

        if (tvFormations != null) {
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
            setupVideoColumn();
            setupQuizColumn();
            setupActionsColumn();
            loadFormations();
        }
    }

    private void loadFormations() {
        try {
            formationList = FXCollections.observableArrayList(formationService.selectALL());
            filteredList = new FilteredList<>(formationList, p -> true);
            tvFormations.setItems(filteredList);
            lblTotal.setText("Total: " + formationList.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ═══════════════════════════════════
    //  TABLE COLUMNS
    // ═══════════════════════════════════
    private void setupVideoColumn() {
        colVideo.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("▶️");
            {
                btn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btn.setTooltip(new Tooltip("Voir la vidéo"));
                btn.setOnAction(e -> showVideoPlayer(getTableView().getItems().get(getIndex())));
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
                btn.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btn.setTooltip(new Tooltip("Gérer le quiz"));
                btn.setOnAction(e -> showQuizManager(getTableView().getItems().get(getIndex())));
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
                btnEdit.setStyle("-fx-background-color: #fdcb6e; -fx-cursor: hand; -fx-background-radius: 5;");
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setStyle("-fx-background-color: #d63031; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                buttons.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> showEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
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
        if (webVideoPlayer != null) webVideoPlayer.getEngine().load(null);
    }

    @FXML
    private void showAddForm() {
        hideAllSections();
        isEditMode = false;
        editingFormation = null;
        lblFormTitle.setText("➕ Ajouter Formation");
        btnSave.setText("✅ Enregistrer");
        clearForm();
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
        lblError.setText("");
        formScrollPane.setVisible(true);
        formScrollPane.setManaged(true);
    }

    // ═══════════════════════════════════
    //  VIDEO PLAYER
    // ═══════════════════════════════════
    private void showVideoPlayer(Formation f) {
        hideAllSections();
        lblVideoTitle.setText("🎬 " + f.getTitle());
        lblVideoDescription.setText(f.getDescription());

        String videoUrl = f.getVideoUrl();
        if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
            String videoId = extractYouTubeId(videoUrl);
            webVideoPlayer.getEngine().load(
                    "https://www.youtube.com/embed/" + videoId + "?autoplay=1");
        } else {
            File file = new File(videoUrl);
            if (file.exists()) {
                String html = "<html><body style='margin:0;background:black;'>" +
                        "<video width='100%' height='100%' controls autoplay>" +
                        "<source src='" + file.toURI().toString() + "'/>" +
                        "</video></body></html>";
                webVideoPlayer.getEngine().loadContent(html);
            } else {
                webVideoPlayer.getEngine().loadContent(
                        "<html><body style='color:white;background:black;text-align:center;padding-top:180px;'>" +
                                "<h2>Vidéo non trouvée</h2><p>" + videoUrl + "</p></body></html>");
            }
        }
        videoSection.setVisible(true);
        videoSection.setManaged(true);
    }

    @FXML
    private void handleChooseVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une vidéo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mkv", "*.mov"),
                new FileChooser.ExtensionFilter("Tous", "*.*")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(tfVideoUrl.getScene().getWindow());
        if (file != null) {
            tfVideoUrl.setText(file.getAbsolutePath());
        }
    }

    private String extractYouTubeId(String url) {
        String id = "";
        if (url.contains("v=")) id = url.split("v=")[1];
        else if (url.contains("youtu.be/")) id = url.split("youtu.be/")[1];
        if (id.contains("&")) id = id.split("&")[0];
        return id;
    }

    // ═══════════════════════════════════
    //  FORMATION CRUD
    // ═══════════════════════════════════
    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        if (isEditMode) {
            editingFormation.setTitle(tfTitle.getText().trim());
            editingFormation.setDescription(taDescription.getText().trim());
            editingFormation.setVideoUrl(tfVideoUrl.getText().trim());
            editingFormation.setCategory(cbCategory.getValue());
            try {
                formationService.updateOne(editingFormation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Modifié! ✅");
                showTable();
            } catch (SQLException e) { lblError.setText(e.getMessage()); }
        } else {
            Formation f = new Formation();
            f.setTitle(tfTitle.getText().trim());
            f.setDescription(taDescription.getText().trim());
            f.setVideoUrl(tfVideoUrl.getText().trim());
            f.setCategory(cbCategory.getValue());
            try {
                formationService.insertOne(f);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Ajouté! ✅");
                showTable();
            } catch (SQLException e) { lblError.setText(e.getMessage()); }
        }
    }

    private void handleDelete(Formation f) {
        Optional<ButtonType> r = showConfirm(
                "Supprimer \"" + f.getTitle() + "\"?\nLe quiz sera aussi supprimé!");
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
                loadFormations();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════
    //  QUIZ CRUD
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
        } catch (SQLException e) { lblQuizError.setText(e.getMessage()); }
        quizScrollPane.setVisible(true);
        quizScrollPane.setManaged(true);
    }

    @FXML
    private void handleSaveQuiz() {
        if (tfQuizTitle.getText().trim().isEmpty()) {
            lblQuizError.setText("Titre obligatoire");
            return;
        }
        int score;
        try {
            score = Integer.parseInt(tfPassingScore.getText().trim());
        } catch (NumberFormatException e) {
            lblQuizError.setText("Score doit être un nombre");
            return;
        }
        try {
            Quiz existing = quizService.selectByFormation(currentQuizFormation.getId());
            if (existing != null) {
                existing.setTitle(tfQuizTitle.getText().trim());
                existing.setPassingScore(score);
                quizService.updateOne(existing);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz mis à jour! ✅");
            } else {
                Quiz q = new Quiz();
                q.setFormationId(currentQuizFormation.getId());
                q.setTitle(tfQuizTitle.getText().trim());
                q.setPassingScore(score);
                quizService.insertOne(q);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz créé! ✅");
            }
            showQuizManager(currentQuizFormation);
        } catch (SQLException e) { lblQuizError.setText(e.getMessage()); }
    }

    @FXML
    private void handleDeleteQuiz() {
        Optional<ButtonType> r = showConfirm("Supprimer ce quiz et toutes ses questions?");
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                Quiz quiz = quizService.selectByFormation(currentQuizFormation.getId());
                if (quiz != null) {
                    List<Question> questions = questionService.selectByQuiz(quiz.getId());
                    for (Question q : questions) {
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
    //  QUESTION CRUD
    // ═══════════════════════════════════
    private void loadQuestions(int quizId) {
        questionsList.getChildren().clear();
        try {
            List<Question> questions = questionService.selectByQuiz(quizId);
            int num = 1;
            for (Question q : questions)
                questionsList.getChildren().add(createQuestionBox(q, num++));
            if (questions.isEmpty())
                questionsList.getChildren().add(
                        new Label("Aucune question. Cliquez '➕ Ajouter Question'"));
        } catch (SQLException e) {
            questionsList.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }

    private VBox createQuestionBox(Question q, int num) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15;" +
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
        btnEditQ.setTooltip(new Tooltip("Modifier question"));
        btnEditQ.setOnAction(e -> editQuestion(q));

        Button btnDelQ = new Button("🗑️");
        btnDelQ.setStyle("-fx-background-color: #d63031; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnDelQ.setTooltip(new Tooltip("Supprimer question"));
        btnDelQ.setOnAction(e -> deleteQuestion(q));

        header.getChildren().addAll(lblQ, lblPts, spacer, btnEditQ, btnDelQ);
        box.getChildren().add(header);

        // Responses with edit and delete
        try {
            List<Reponse> reponses = reponseService.selectByQuestion(q.getId());
            for (Reponse r : reponses) {
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
                btnEditR.setTooltip(new Tooltip("Modifier réponse"));
                btnEditR.setOnAction(e -> editReponse(r, q));

                Button btnDelR = new Button("✕");
                btnDelR.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-cursor: hand;");
                btnDelR.setTooltip(new Tooltip("Supprimer réponse"));
                btnDelR.setOnAction(e -> deleteReponse(r));

                respRow.getChildren().addAll(lblResp, btnEditR, btnDelR);
                box.getChildren().add(respRow);
            }
        } catch (SQLException e) { /* skip */ }

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
            TextArea taQ = new TextArea();
            taQ.setPromptText("Entrer la question...");
            taQ.setPrefRowCount(3);
            TextField tfPts = new TextField("5");
            tfPts.setPromptText("Points");
            content.getChildren().addAll(new Label("Question:"), taQ, new Label("Points:"), tfPts);
            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(btn -> {
                if (btn == saveBtn && !taQ.getText().trim().isEmpty()) {
                    Question q = new Question();
                    q.setQuizId(quiz.getId());
                    q.setQuestionText(taQ.getText().trim());
                    try { q.setPoints(Integer.parseInt(tfPts.getText().trim())); }
                    catch (NumberFormatException e) { q.setPoints(5); }
                    return q;
                }
                return null;
            });

            Optional<Question> result = dialog.showAndWait();
            result.ifPresent(q -> {
                try { questionService.insertOne(q); showQuizManager(currentQuizFormation); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void editQuestion(Question q) {
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle("Modifier Question");
        ButtonType saveBtn = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        TextArea taQ = new TextArea(q.getQuestionText());
        taQ.setPrefRowCount(3);
        TextField tfPts = new TextField(String.valueOf(q.getPoints()));
        content.getChildren().addAll(new Label("Question:"), taQ, new Label("Points:"), tfPts);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !taQ.getText().trim().isEmpty()) {
                q.setQuestionText(taQ.getText().trim());
                try { q.setPoints(Integer.parseInt(tfPts.getText().trim())); }
                catch (NumberFormatException e) { q.setPoints(5); }
                return q;
            }
            return null;
        });

        Optional<Question> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try { questionService.updateOne(updated); showQuizManager(currentQuizFormation); }
            catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        });
    }

    private void deleteQuestion(Question q) {
        Optional<ButtonType> r = showConfirm("Supprimer cette question et ses réponses?");
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                for (Reponse rep : reponseService.selectByQuestion(q.getId()))
                    reponseService.deleteOne(rep);
                questionService.deleteOne(q);
                showQuizManager(currentQuizFormation);
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════
    //  REPONSE CRUD
    // ═══════════════════════════════════
    private void addReponse(Question q) {
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Réponse");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        TextField tfOpt = new TextField();
        tfOpt.setPromptText("Entrer la réponse...");
        CheckBox cbCorrect = new CheckBox("Réponse correcte ✅");
        content.getChildren().addAll(new Label("Réponse:"), tfOpt, cbCorrect);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !tfOpt.getText().trim().isEmpty()) {
                Reponse r = new Reponse();
                r.setQuestionId(q.getId());
                r.setOptionText(tfOpt.getText().trim());
                r.setCorrect(cbCorrect.isSelected());
                return r;
            }
            return null;
        });

        Optional<Reponse> result = dialog.showAndWait();
        result.ifPresent(rep -> {
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
        TextField tfOpt = new TextField(r.getOptionText());
        CheckBox cbCorrect = new CheckBox("Réponse correcte ✅");
        cbCorrect.setSelected(r.isCorrect());
        content.getChildren().addAll(new Label("Réponse:"), tfOpt, cbCorrect);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !tfOpt.getText().trim().isEmpty()) {
                r.setOptionText(tfOpt.getText().trim());
                r.setCorrect(cbCorrect.isSelected());
                return r;
            }
            return null;
        });

        Optional<Reponse> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try { reponseService.updateOne(updated); showQuizManager(currentQuizFormation); }
            catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        });
    }

    private void deleteReponse(Reponse r) {
        try {
            reponseService.deleteOne(r);
            showQuizManager(currentQuizFormation);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ═══════════════════════════════════
    //  SEARCH + CLEAR + REFRESH
    // ═══════════════════════════════════
    @FXML
    private void handleSearch() {
        String kw = tfSearch.getText().toLowerCase().trim();
        filteredList.setPredicate(f -> kw.isEmpty() ||
                f.getTitle().toLowerCase().contains(kw) ||
                f.getCategory().toLowerCase().contains(kw));
        lblTotal.setText("Total: " + filteredList.size());
    }

    @FXML
    private void handleRefresh() {
        tfSearch.clear();
        loadFormations();
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    private void clearForm() {
        if (tfTitle != null) tfTitle.clear();
        if (taDescription != null) taDescription.clear();
        if (tfVideoUrl != null) tfVideoUrl.clear();
        if (cbCategory != null) cbCategory.getSelectionModel().clearSelection();
        if (lblError != null) lblError.setText("");
        editingFormation = null;
        isEditMode = false;
    }

    private boolean validateForm() {
        StringBuilder err = new StringBuilder();
        if (tfTitle.getText().trim().isEmpty()) err.append("• Titre obligatoire\n");
        if (taDescription.getText().trim().isEmpty()) err.append("• Description obligatoire\n");
        if (tfVideoUrl.getText().trim().isEmpty()) err.append("• Vidéo obligatoire\n");
        if (cbCategory.getValue() == null) err.append("• Catégorie obligatoire\n");
        if (err.length() > 0) { lblError.setText(err.toString()); return false; }
        return true;
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait();
    }
}