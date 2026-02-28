package controllers;

import entities.Message;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import services.Servicemessage;
import services.serviceUser;
import utils.LightDialog;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Messageriecontroller {

    @FXML private VBox conversationsList;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollMessages;
    @FXML private TextField tfMessage;
    @FXML private Label lblContactName, lblContactRole, lblContactInitial;
    @FXML private VBox lblNoConv;
    @FXML private VBox chatPane;
    @FXML private HBox inputBar;

    private User currentUser;
    private User selectedContact;
    private final Servicemessage svc = new Servicemessage();
    private final serviceUser us = new serviceUser();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm · dd/MM");

    @FXML
    void initialize() {
        if (chatPane != null) chatPane.setVisible(false);
        if (inputBar != null) inputBar.setVisible(false);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadConversations();
    }

    // ─── Load conversation list ───────────────────────────────
    private void loadConversations() {
        if (conversationsList == null) return;
        conversationsList.getChildren().clear();

        try {
            // Obtenir tous les interlocuteurs possibles selon le rôle
            List<User> contacts;
            if (currentUser.getRole().equalsIgnoreCase("PATIENT")) {
                contacts = us.selectALL().stream()
                        .filter(u -> u.getRole().equalsIgnoreCase("COACH"))
                        .toList();
            } else {
                // Coach ou Admin → peut écrire aux patients
                contacts = us.selectALL().stream()
                        .filter(u -> u.getRole().equalsIgnoreCase("PATIENT"))
                        .toList();
            }

            if (contacts.isEmpty()) {
                Label empty = new Label("Aucun contact disponible");
                empty.setStyle("-fx-text-fill: #A0AEC0; -fx-font-size: 12px; -fx-padding: 20;");
                conversationsList.getChildren().add(empty);
                return;
            }

            for (User contact : contacts) {
                int unread = svc.countUnread(currentUser.getId_user());
                VBox row = buildConvRow(contact, unread > 0);
                conversationsList.getChildren().add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildConvRow(User contact, boolean hasUnread) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color: white; -fx-cursor: hand; -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        Circle circle = new Circle(22);
        circle.setFill(javafx.scene.paint.Color.web(contact.getRole().equalsIgnoreCase("COACH") ? "#F5C87A" : "#E8956D"));
        Label initial = new Label(contact.getPrenom().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 700;");
        avatar.getChildren().addAll(circle, initial);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(contact.getPrenom() + " " + contact.getNom());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2D3748;");
        Label role = new Label(contact.getRole());
        role.setStyle("-fx-font-size: 10px; -fx-text-fill: #A0AEC0;");
        info.getChildren().addAll(name, role);

        row.getChildren().addAll(avatar, info);

        // Badge non-lu
        if (hasUnread) {
            Circle badge = new Circle(6);
            badge.setFill(javafx.scene.paint.Color.web("#E8956D"));
            row.getChildren().add(badge);
        }

        VBox wrapper = new VBox(row);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #FFF8F0; -fx-cursor: hand; -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color: white; -fx-cursor: hand; -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;"));
        row.setOnMouseClicked(e -> openConversation(contact));

        return wrapper;
    }

    // ─── Open conversation with contact ──────────────────────
    private void openConversation(User contact) {
        this.selectedContact = contact;

        if (chatPane != null) chatPane.setVisible(true);
        if (inputBar != null) inputBar.setVisible(true);
        if (lblNoConv != null) lblNoConv.setVisible(false);

        if (lblContactName != null) lblContactName.setText(contact.getPrenom() + " " + contact.getNom());
        if (lblContactRole != null) lblContactRole.setText(contact.getRole());
        if (lblContactInitial != null) lblContactInitial.setText(contact.getPrenom().substring(0, 1).toUpperCase());

        loadMessages();
        // Marquer comme lu
        try { svc.markAsRead(contact.getId_user(), currentUser.getId_user()); }
        catch (SQLException e) { /* ignore */ }
        // Refresh conv list
        loadConversations();
    }

    private void loadMessages() {
        if (messagesContainer == null || selectedContact == null) return;
        messagesContainer.getChildren().clear();
        try {
            List<Message> messages = svc.getConversation(
                    currentUser.getId_user(), selectedContact.getId_user());

            if (messages.isEmpty()) {
                Label empty = new Label("Commencez la conversation 👋");
                empty.setStyle("-fx-text-fill: #A0AEC0; -fx-font-size: 13px; -fx-font-style: italic;");
                HBox c = new HBox(empty);
                c.setAlignment(Pos.CENTER);
                c.setPadding(new Insets(40));
                messagesContainer.getChildren().add(c);
                return;
            }

            for (Message msg : messages) {
                boolean mine = (msg.getId_expediteur() == currentUser.getId_user());
                messagesContainer.getChildren().add(buildBubble(msg, mine));
            }

            // Scroll to bottom
            Platform.runLater(() -> scrollMessages.setVvalue(1.0));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox buildBubble(Message msg, boolean mine) {
        Label bubble = new Label(msg.getContenu());
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        Label time = new Label(msg.getDate_envoi().format(FMT));
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: " + (mine ? "rgba(255,255,255,0.7)" : "#A0AEC0") + ";");

        VBox content = new VBox(4, bubble, time);
        content.setMaxWidth(420);

        if (mine) {
            bubble.setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white; -fx-font-size: 13px; " +
                    "-fx-background-radius: 16 16 4 16; -fx-effect: dropshadow(gaussian,rgba(74,111,165,0.2),6,0,0,2);");
            time.setAlignment(Pos.CENTER_RIGHT);
            content.setAlignment(Pos.CENTER_RIGHT);
        } else {
            bubble.setStyle("-fx-background-color: white; -fx-text-fill: #2D3748; -fx-font-size: 13px; " +
                    "-fx-background-radius: 16 16 16 4; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
            content.setAlignment(Pos.CENTER_LEFT);
        }

        HBox row = new HBox(content);
        row.setPadding(new Insets(4, 16, 4, 16));
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    // ─── Send message ─────────────────────────────────────────
    @FXML
    void sendMessage() {
        if (selectedContact == null || tfMessage == null) return;
        String text = tfMessage.getText().trim();
        if (text.isEmpty()) return;

        Message msg = new Message(currentUser.getId_user(), selectedContact.getId_user(), text);
        try {
            svc.sendMessage(msg);
            tfMessage.clear();
            loadMessages();
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible d'envoyer le message.");
        }
    }

    @FXML
    void onEnterPressed(javafx.scene.input.KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.ENTER) sendMessage();
    }
}