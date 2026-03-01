package controllers;

import entities.Call;
import entities.Message;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.*;
import services.Callservice;
import services.Notificationservice;
import services.Servicemessage;
import services.serviceUser;
import utils.LightDialog;
import utils.Notificationhelper;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Messageriecontroller {

    @FXML private VBox       conversationsList;
    @FXML private VBox       messagesContainer;
    @FXML private ScrollPane scrollMessages;
    @FXML private TextField  tfMessage;
    @FXML private Label      lblContactName, lblContactRole, lblContactInitial;
    @FXML private VBox       lblNoConv;
    @FXML private VBox       chatPane;
    @FXML private HBox       inputBar;
    @FXML private Label      lblCallBtn; // bouton appel dans le header

    private User currentUser;
    private User selectedContact;
    private final Servicemessage msgSvc  = new Servicemessage();
    private final serviceUser    us      = new serviceUser();
    private final Callservice    callSvc = new Callservice();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm · dd/MM");

    @FXML
    void initialize() {
        if (chatPane != null) { chatPane.setVisible(false); chatPane.setManaged(false); }
        if (inputBar != null) { inputBar.setVisible(false); inputBar.setManaged(false); }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadConversations();
        Notificationservice.INSTANCE.refreshBadge();
    }

    // ─── Conversations list ───────────────────────────────────
    private void loadConversations() {
        if (conversationsList == null) return;
        conversationsList.getChildren().clear();
        try {
            List<User> contacts = getContacts();
            if (contacts.isEmpty()) {
                Label empty = new Label("Aucun contact disponible");
                empty.setStyle("-fx-text-fill: #A0AEC0; -fx-font-size: 12px; -fx-padding: 20;");
                conversationsList.getChildren().add(empty);
                return;
            }
            for (User contact : contacts)
                conversationsList.getChildren().add(buildConvRow(contact));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private List<User> getContacts() throws SQLException {
        return us.selectALL().stream()
                .filter(u -> currentUser.getRole().equalsIgnoreCase("PATIENT")
                        ? u.getRole().equalsIgnoreCase("COACH")
                        : u.getRole().equalsIgnoreCase("PATIENT"))
                .toList();
    }

    private VBox buildConvRow(User contact) throws SQLException {
        int unread = msgSvc.countUnread(currentUser.getId_user());

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color: white; -fx-cursor: hand; " +
                "-fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(44, 44); avatar.setMaxSize(44, 44);
        Circle circ = new Circle(22);
        circ.setFill(Color.web(contact.getRole().equalsIgnoreCase("COACH") ? "#F5C87A" : "#E8956D"));
        Label init = new Label(contact.getPrenom().substring(0, 1).toUpperCase());
        init.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 700;");
        avatar.getChildren().addAll(circ, init);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(contact.getPrenom() + " " + contact.getNom());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2D3748;");
        Label role = new Label(contact.getRole());
        role.setStyle("-fx-font-size: 10px; -fx-text-fill: #A0AEC0;");
        info.getChildren().addAll(name, role);
        row.getChildren().addAll(avatar, info);

        if (unread > 0) {
            StackPane badge = new StackPane();
            badge.setMinSize(22,22); badge.setMaxSize(22,22);
            Circle bc = new Circle(11); bc.setFill(Color.web("#E8956D"));
            Label bl = new Label(String.valueOf(unread));
            bl.setStyle("-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:700;");
            badge.getChildren().addAll(bc, bl);
            row.getChildren().add(badge);
        }

        VBox wrapper = new VBox(row);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #FFF8F0; -fx-cursor: hand; -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color: white; -fx-cursor: hand; -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;"));
        row.setOnMouseClicked(e -> openConversation(contact));
        return wrapper;
    }

    // ─── Open conversation ────────────────────────────────────
    private void openConversation(User contact) {
        this.selectedContact = contact;
        if (chatPane != null) { chatPane.setVisible(true);  chatPane.setManaged(true); }
        if (inputBar != null) { inputBar.setVisible(true);  inputBar.setManaged(true); }
        if (lblNoConv != null) { lblNoConv.setVisible(false); lblNoConv.setManaged(false); }

        if (lblContactName    != null) lblContactName.setText(contact.getPrenom() + " " + contact.getNom());
        if (lblContactRole    != null) lblContactRole.setText(contact.getRole());
        if (lblContactInitial != null) lblContactInitial.setText(contact.getPrenom().substring(0,1).toUpperCase());

        loadMessages();
        try { msgSvc.markAsRead(contact.getId_user(), currentUser.getId_user()); }
        catch (SQLException e) { /* ignore */ }
        loadConversations();
        Notificationservice.INSTANCE.refreshBadge();
    }

    private void loadMessages() {
        if (messagesContainer == null || selectedContact == null) return;
        messagesContainer.getChildren().clear();
        try {
            List<Message> messages = msgSvc.getConversation(
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
                boolean mine = msg.getId_expediteur() == currentUser.getId_user();
                messagesContainer.getChildren().add(buildBubble(msg, mine));
            }
            Platform.runLater(() -> scrollMessages.setVvalue(1.0));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox buildBubble(Message msg, boolean mine) {
        Label bubble = new Label(msg.getContenu());
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        Label time = new Label(msg.getDate_envoi().format(FMT));
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: " +
                (mine ? "rgba(255,255,255,0.7)" : "#A0AEC0") + ";");
        VBox content = new VBox(4, bubble, time);
        content.setMaxWidth(420);
        if (mine) {
            bubble.setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white; -fx-font-size: 13px;" +
                    "-fx-background-radius: 16 16 4 16; -fx-effect: dropshadow(gaussian,rgba(74,111,165,0.2),6,0,0,2);");
            content.setAlignment(Pos.CENTER_RIGHT);
        } else {
            bubble.setStyle("-fx-background-color: white; -fx-text-fill: #2D3748; -fx-font-size: 13px;" +
                    "-fx-background-radius: 16 16 16 4; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
            content.setAlignment(Pos.CENTER_LEFT);
        }
        HBox row = new HBox(content);
        row.setPadding(new Insets(4, 16, 4, 16));
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    // ─── Send message ─────────────────────────────────────────
    @FXML void sendMessage() {
        if (selectedContact == null || tfMessage == null) return;
        String text = tfMessage.getText().trim();
        if (text.isEmpty()) return;
        Message msg = new Message(currentUser.getId_user(), selectedContact.getId_user(), text);
        try {
            msgSvc.sendMessage(msg);
            tfMessage.clear();
            loadMessages();
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible d'envoyer le message.");
        }
    }

    @FXML void onEnterPressed(javafx.scene.input.KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.ENTER) sendMessage();
    }

    // ─── Appel vocal ──────────────────────────────────────────
    @FXML void handleCall() {
        if (selectedContact == null) return;
        try {
            int callId = callSvc.initiateCall(currentUser.getId_user(), selectedContact.getId_user());
            openCallScreen(false, null, callId);
        } catch (Exception e) {
            LightDialog.showError("Erreur", "Impossible de lancer l'appel.");
        }
    }

    /** Ouvrir l'écran d'appel — outgoing ou incoming */
    public void openCallScreen(boolean incoming, Call incomingCall, int callId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Call.fxml"));
            StackPane root = loader.load();
            Callcontroller ctrl = loader.getController();

            Stage callStage = new Stage();
            callStage.initStyle(StageStyle.UNDECORATED);
            callStage.setResizable(false);
            callStage.setAlwaysOnTop(true);
            Scene scene = new Scene(root, 400, 520);
            scene.setFill(Color.TRANSPARENT);
            callStage.setScene(scene);

            Notificationservice.INSTANCE.setCallScreenOpen(true);
            ctrl.setOnCallEnded(() -> {
                Notificationservice.INSTANCE.setCallScreenOpen(false);
                Notificationservice.INSTANCE.setPendingCallId(-1);
            });

            if (incoming && incomingCall != null) {
                ctrl.setupIncomingCall(currentUser, selectedContact, incomingCall);
            } else {
                ctrl.setupOutgoingCall(currentUser, selectedContact, callId);
            }

            callStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible d'ouvrir l'écran d'appel.");
        }
    }
}