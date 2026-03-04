package controllers;

import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import services.Servicechathistory;
import services.Servicechathistory.SessionInfo;
import services.Servicechathistory.MessageRecord;

import java.net.URI;
import java.net.http.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatbotController {

    @FXML private VBox       messagesContainer;
    @FXML private ScrollPane scrollMessages;
    @FXML private TextField  tfMessage;
    @FXML private Label      lblTyping;
    @FXML private VBox       rootPane;

    private User currentUser;
    private String currentSessionId;
    private Servicechathistory chatService;
    private final List<String[]> history = new ArrayList<>();

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";
    private static final String API_KEY = "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";
    private static final DateTimeFormatter FMT  = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DFMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.chatService = new Servicechathistory();
        startNewSession();
        showWelcomeMessage();
    }

    @FXML void initialize() {}

    // --- Demarrer une nouvelle session vide ---
    private void startNewSession() {
        currentSessionId = UUID.randomUUID().toString().substring(0, 8);
        history.clear();
    }

    // --- Reprendre une session existante depuis la BD ---
    private void resumeSession(String sessionId) {
        if (chatService == null || currentUser == null) return;
        currentSessionId = sessionId;
        history.clear();
        messagesContainer.getChildren().clear();

        List<MessageRecord> msgs = chatService.getMessages(currentUser.getId_user(), sessionId);
        for (MessageRecord msg : msgs) {
            boolean isUser = "user".equals(msg.role);
            // Reconstituer l'historique pour l'IA
            history.add(new String[]{msg.role, msg.content});
            // Afficher les bulles dans le chat
            addBubble(msg.content, isUser, false, false);
        }
        // Scroller en bas
        Platform.runLater(() -> { if (scrollMessages != null) scrollMessages.setVvalue(1.0); });
    }

    private String buildSystemPrompt() {
        String name = (currentUser != null)
                ? currentUser.getPrenom() + " " + currentUser.getNom() : "Patient";
        return "Tu es Echo, un assistant bien-etre bienveillant integre dans EchoCare. "
                + "Ecoute sans jugement, propose des exercices de respiration et pleine conscience, "
                + "encourage a parler au coach si necessaire, reponds en francais, "
                + "sois concis (3-4 phrases max), ne pose pas de diagnostic medical. "
                + "Patient: " + name + ".";
    }

    private void showWelcomeMessage() {
        messagesContainer.getChildren().clear();
        String prenom = (currentUser != null) ? currentUser.getPrenom() : "vous";
        addBubble("Bonjour " + prenom + " ! Je suis Echo, votre assistant bien-etre. "
                + "Comment vous sentez-vous aujourd'hui ?", false, true, false);
    }

    @FXML
    void sendMessage() {
        String text = tfMessage.getText().trim();
        if (text.isEmpty()) return;
        tfMessage.clear();
        addBubble(text, true, false, true);
        history.add(new String[]{"user", text});
        if (chatService != null && currentUser != null)
            chatService.saveMessage(currentUser.getId_user(), currentSessionId, "user", text);

        lblTyping.setVisible(true);
        lblTyping.setManaged(true);
        Thread t = new Thread(() -> {
            String response = callGroqAPI();
            Platform.runLater(() -> {
                lblTyping.setVisible(false);
                lblTyping.setManaged(false);
                addBubble(response, false, false, true);
                history.add(new String[]{"assistant", response});
                if (chatService != null && currentUser != null)
                    chatService.saveMessage(currentUser.getId_user(), currentSessionId, "assistant", response);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    void onEnterPressed(javafx.scene.input.KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.ENTER) sendMessage();
    }

    @FXML
    void clearChat() {
        startNewSession();
        showWelcomeMessage();
    }

    // ============================================================
    // HISTORIQUE - Dialog avec bouton "Reprendre"
    // ============================================================
    @FXML
    void showHistory() {
        if (chatService == null || currentUser == null) return;
        List<SessionInfo> sessions = chatService.getSessions(currentUser.getId_user());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Historique des conversations");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(720);
        dialog.getDialogPane().setPrefHeight(540);
        dialog.getDialogPane().setStyle("-fx-background-color: #F2F0EC;");

        if (sessions.isEmpty()) {
            Label empty = new Label("Aucune conversation sauvegardee pour l'instant.");
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #A0AEC0; -fx-padding: 40;");
            dialog.getDialogPane().setContent(empty);
            dialog.showAndWait();
            return;
        }

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.36);

        // ---- Panneau gauche : liste des sessions ----
        VBox leftPanel = new VBox(6);
        leftPanel.setStyle("-fx-background-color: white; -fx-padding: 12;");
        Label leftTitle = new Label("Conversations");
        leftTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2D3748; -fx-padding: 0 0 8 0;");
        leftPanel.getChildren().add(leftTitle);
        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setStyle("-fx-background: white; -fx-background-color: white;");

        // ---- Panneau droit : messages + bouton Reprendre ----
        VBox rightWrapper = new VBox(0);
        rightWrapper.setStyle("-fx-background-color: #F7F5F2;");

        // Barre du haut avec bouton Reprendre
        HBox rightTopBar = new HBox(10);
        rightTopBar.setAlignment(Pos.CENTER_RIGHT);
        rightTopBar.setStyle("-fx-padding: 10 14; -fx-background-color: white;"
                + " -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;");
        Label lblSessionTitle = new Label("Selectionnez une conversation");
        lblSessionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0AEC0; -fx-font-style: italic;");
        HBox.setHgrow(lblSessionTitle, Priority.ALWAYS);
        Button btnResume = new Button("Reprendre cette conversation");
        btnResume.setStyle("-fx-background-color: #52B788; -fx-text-fill: white;"
                + " -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 7 16;"
                + " -fx-background-radius: 20; -fx-cursor: hand;");
        btnResume.setDisable(true);
        rightTopBar.getChildren().add(lblSessionTitle);
        rightTopBar.getChildren().add(btnResume);

        VBox rightPanel = new VBox(8);
        rightPanel.setStyle("-fx-background-color: #F7F5F2; -fx-padding: 14;");
        VBox.setVgrow(rightPanel, Priority.ALWAYS);
        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setStyle("-fx-background: #F7F5F2; -fx-background-color: #F7F5F2;");
        VBox.setVgrow(rightScroll, Priority.ALWAYS);

        rightWrapper.getChildren().add(rightTopBar);
        rightWrapper.getChildren().add(rightScroll);

        // Variable pour garder la session selectionnee
        final String[] selectedSessionId = {null};

        // ---- Methode d'affichage d'une session dans le panneau droit ----
        Runnable loadSelectedSession = () -> {
            if (selectedSessionId[0] == null) return;
            rightPanel.getChildren().clear();
            List<MessageRecord> msgs = chatService.getMessages(currentUser.getId_user(), selectedSessionId[0]);
            for (MessageRecord msg : msgs) {
                boolean isUser = "user".equals(msg.role);
                Label bubble = new Label(msg.content);
                bubble.setWrapText(true);
                bubble.setMaxWidth(360);
                bubble.setPadding(new Insets(9, 14, 9, 14));
                bubble.setStyle(isUser
                        ? "-fx-background-color: #4A6FA5; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 14 14 4 14;"
                        : "-fx-background-color: white; -fx-text-fill: #2D3748; -fx-font-size: 12px; -fx-background-radius: 14 14 14 4;");
                Label time = new Label(msg.createdAt.format(FMT));
                time.setStyle("-fx-font-size: 9px; -fx-text-fill: #A0AEC0;");
                VBox msgBox = new VBox(3);
                msgBox.getChildren().add(bubble);
                msgBox.getChildren().add(time);
                msgBox.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                HBox row = new HBox(msgBox);
                row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                rightPanel.getChildren().add(row);
            }
            Platform.runLater(() -> rightScroll.setVvalue(1.0));
        };

        // ---- Bouton Reprendre : charge la session et ferme le dialog ----
        btnResume.setOnAction(ev -> {
            if (selectedSessionId[0] != null) {
                resumeSession(selectedSessionId[0]);
                dialog.close();
            }
        });

        // ---- Creer les boutons de sessions ----
        Button[] sessionBtns = new Button[sessions.size()];
        for (int idx = 0; idx < sessions.size(); idx++) {
            final SessionInfo sess = sessions.get(idx);
            final int fi = idx;
            Button sessBtn = new Button();
            sessBtn.setText(sess.startedAt.format(DFMT) + "\n" + sess.msgCount + " message(s)");
            sessBtn.setMaxWidth(Double.MAX_VALUE);
            sessBtn.setStyle("-fx-background-color: #EEF4FF; -fx-text-fill: #4A6FA5;"
                    + " -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 8;"
                    + " -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;");
            sessionBtns[idx] = sessBtn;

            sessBtn.setOnAction(e -> {
                // Mettre en evidence le bouton selectionne
                for (Button b : sessionBtns) {
                    if (b != null) b.setStyle("-fx-background-color: #EEF4FF; -fx-text-fill: #4A6FA5;"
                            + " -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 8;"
                            + " -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;");
                }
                sessBtn.setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white;"
                        + " -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 8;"
                        + " -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;");

                selectedSessionId[0] = sess.sessionId;
                lblSessionTitle.setText("Conversation du " + sess.startedAt.format(DFMT));
                lblSessionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #2D3748; -fx-font-weight: 600;");
                btnResume.setDisable(false);
                loadSelectedSession.run();
            });

            // Bouton supprimer
            Button delBtn = new Button("X");
            delBtn.setStyle("-fx-background-color: #FFE8E8; -fx-text-fill: #E07070;"
                    + " -fx-font-size: 10px; -fx-padding: 3 7; -fx-background-radius: 6; -fx-cursor: hand;");
            delBtn.setOnAction(e -> {
                chatService.deleteSession(currentUser.getId_user(), sess.sessionId);
                if (sessBtn.getParent() instanceof HBox) {
                    leftPanel.getChildren().remove(sessBtn.getParent());
                }
                rightPanel.getChildren().clear();
                selectedSessionId[0] = null;
                lblSessionTitle.setText("Selectionnez une conversation");
                lblSessionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0AEC0; -fx-font-style: italic;");
                btnResume.setDisable(true);
                sessionBtns[fi] = null;
            });

            HBox sessRow = new HBox(6);
            sessRow.getChildren().add(sessBtn);
            sessRow.getChildren().add(delBtn);
            sessRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(sessBtn, Priority.ALWAYS);
            leftPanel.getChildren().add(sessRow);
        }

        // Selectionner et afficher la premiere session automatiquement
        if (!sessions.isEmpty()) {
            selectedSessionId[0] = sessions.get(0).sessionId;
            lblSessionTitle.setText("Conversation du " + sessions.get(0).startedAt.format(DFMT));
            lblSessionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #2D3748; -fx-font-weight: 600;");
            btnResume.setDisable(false);
            sessionBtns[0].setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white;"
                    + " -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 8;"
                    + " -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;");
            loadSelectedSession.run();
        }

        split.getItems().add(leftScroll);
        split.getItems().add(rightWrapper);
        dialog.getDialogPane().setContent(split);
        dialog.showAndWait();
    }

    // ============================================================
    // API GROQ
    // ============================================================
    private String callGroqAPI() {
        try {
            StringBuilder msgs = new StringBuilder("[");
            msgs.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(buildSystemPrompt())).append("\"}");
            for (String[] msg : history) {
                msgs.append(",{\"role\":\"").append(msg[0])
                        .append("\",\"content\":\"").append(escapeJson(msg[1])).append("\"}");
            }
            msgs.append("]");
            String body = "{\"model\":\"" + MODEL + "\",\"max_tokens\":600,\"temperature\":0.7,"
                    + "\"messages\":" + msgs + "}";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Groq Status: " + response.statusCode());
            if (response.statusCode() != 200) {
                if (response.statusCode() == 401) return "Cle API Groq invalide. Allez sur console.groq.com.";
                if (response.statusCode() == 429) return "Trop de requetes. Attendez et reessayez.";
                return "Erreur API (" + response.statusCode() + ").";
            }
            return extractContent(response.body());
        } catch (Exception e) {
            System.err.println("Groq error: " + e.getMessage());
            return "Difficulte technique. Verifiez votre connexion.";
        }
    }

    private String extractContent(String json) {
        try {
            String marker = "\"content\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return "Pas de reponse. Reessayez.";
            start += marker.length();
            StringBuilder sb = new StringBuilder();
            int i = start;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"')  { sb.append('"');  i += 2; continue; }
                    if (next == 'n')  { sb.append('\n'); i += 2; continue; }
                    if (next == '\\') { sb.append('\\'); i += 2; continue; }
                    if (next == 't')  { sb.append('\t'); i += 2; continue; }
                    if (next == 'r')  { i += 2; continue; }
                }
                if (c == '"') break;
                sb.append(c);
                i++;
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Reponse non disponible.";
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");
    }

    private void addBubble(String text, boolean isUser, boolean isWelcome, boolean doScroll) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(11, 16, 11, 16));
        Label time = new Label(LocalDateTime.now().format(FMT));
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: "
                + (isUser ? "rgba(255,255,255,0.55)" : "#A0AEC0") + ";");

        if (isUser) {
            bubble.setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white; -fx-font-size: 13px;"
                    + " -fx-background-radius: 16 16 4 16;"
                    + " -fx-effect: dropshadow(gaussian,rgba(74,111,165,0.25),6,0,0,2);");
        } else if (isWelcome) {
            bubble.setStyle("-fx-background-color: linear-gradient(to right, #E8956D, #F5C87A);"
                    + " -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600;"
                    + " -fx-background-radius: 16 16 16 4;"
                    + " -fx-effect: dropshadow(gaussian,rgba(232,149,109,0.3),6,0,0,2);");
        } else {
            bubble.setStyle("-fx-background-color: white; -fx-text-fill: #2D3748; -fx-font-size: 13px;"
                    + " -fx-background-radius: 16 16 16 4;"
                    + " -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
        }

        VBox content = new VBox(4);
        content.getChildren().add(bubble);
        content.getChildren().add(time);
        content.setMaxWidth(440);
        content.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox row = new HBox(10);
        row.setPadding(new Insets(4, 16, 4, 16));
        if (!isUser) {
            StackPane avatar = new StackPane();
            avatar.setMinSize(32, 32);
            avatar.setMaxSize(32, 32);
            Circle c = new Circle(16);
            c.setFill(Color.web(isWelcome ? "#E8956D" : "#4A6FA5"));
            Label init = new Label("E");
            init.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700;");
            avatar.getChildren().add(c);
            avatar.getChildren().add(init);
            avatar.setAlignment(Pos.TOP_CENTER);
            row.getChildren().add(avatar);
            row.getChildren().add(content);
            row.setAlignment(Pos.CENTER_LEFT);
        } else {
            row.getChildren().add(content);
            row.setAlignment(Pos.CENTER_RIGHT);
        }
        messagesContainer.getChildren().add(row);
        if (doScroll) Platform.runLater(() -> { if (scrollMessages != null) scrollMessages.setVvalue(1.0); });
    }

    @FXML void sendBien()    { tfMessage.setText("Je me sens bien aujourd'hui");       sendMessage(); }
    @FXML void sendMoyen()   { tfMessage.setText("Je me sens moyen, pas top...");      sendMessage(); }
    @FXML void sendStresse() { tfMessage.setText("Je suis tres stresse en ce moment"); sendMessage(); }
    @FXML void sendTriste()  { tfMessage.setText("Je me sens triste et seul(e)");      sendMessage(); }
}