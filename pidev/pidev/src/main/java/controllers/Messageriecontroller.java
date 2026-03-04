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
import services.*;
import utils.LightDialog;
import utils.Notificationhelper;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Messageriecontroller {

    @FXML private VBox       conversationsList;
    @FXML private VBox       messagesContainer;
    @FXML private ScrollPane scrollMessages;
    @FXML private TextField  tfMessage;
    @FXML private Label      lblContactName, lblContactRole, lblContactInitial;
    @FXML private VBox       lblNoConv;
    @FXML private VBox       chatPane;
    @FXML private HBox       inputBar;

    private User currentUser;
    private User selectedContact;
    private final Servicemessage msgSvc  = new Servicemessage();
    private final serviceUser    us      = new serviceUser();
    private final Callservice    callSvc = new Callservice();

    // Pour l'édition en cours
    private Message editingMessage = null;
    private Label   editingBubble  = null;

    // Badge par sender
    private Map<Integer, String> unreadPerSender = java.util.Collections.emptyMap();

    // Auto-refresh
    private javafx.animation.Timeline refreshTimer;
    private int lastMessageCount = 0;

    private static final DateTimeFormatter FMT_TIME  = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_FULL  = DateTimeFormatter.ofPattern("HH:mm · dd/MM");

    @FXML void initialize() {
        if (chatPane != null) { chatPane.setVisible(false); chatPane.setManaged(false); }
        if (inputBar != null) { inputBar.setVisible(false); inputBar.setManaged(false); }
    }

    /** Définir le contact sélectionné (utilisé par les homes pour appels entrants) */
    public void setSelectedContact(User contact) { this.selectedContact = contact; }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadConversations();
        startAutoRefresh();
        Notificationservice.INSTANCE.refreshBadge();
        // Écouter badges par sender
        Notificationservice.INSTANCE.setOnUnreadPerSender(map -> {
            this.unreadPerSender = map;
            Platform.runLater(this::loadConversations);
        });
    }

    // ── Auto-refresh messages ─────────────────────────────────
    private void startAutoRefresh() {
        refreshTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    if (selectedContact != null) silentRefreshMessages();
                })
        );
        refreshTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        refreshTimer.play();
    }

    private void silentRefreshMessages() {
        try {
            List<Message> msgs = msgSvc.getConversation(
                    currentUser.getId_user(), selectedContact.getId_user());
            if (msgs.size() != lastMessageCount) {
                lastMessageCount = msgs.size();
                renderMessages(msgs);
            }
        } catch (SQLException e) { /* ignore */ }
    }

    // ── Conversations list ────────────────────────────────────
    private void loadConversations() {
        if (conversationsList == null) return;
        conversationsList.getChildren().clear();
        try {
            List<User> contacts = getContacts();
            for (User contact : contacts)
                conversationsList.getChildren().add(buildConvRow(contact));
            if (contacts.isEmpty()) {
                Label empty = new Label("Aucun contact disponible");
                empty.setStyle("-fx-text-fill: #A0AEC0; -fx-font-size: 12px; -fx-padding: 20;");
                conversationsList.getChildren().add(empty);
            }
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
        // Unread count for this specific contact
        int unread = msgSvc.countUnreadFrom(contact.getId_user(), currentUser.getId_user());
        String lastPreview = msgSvc.getLastMessagePreview(currentUser.getId_user(), contact.getId_user());
        boolean isSelected = selectedContact != null && selectedContact.getId_user() == contact.getId_user();

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        String bgColor = isSelected ? "#FFF8F0" : "white";
        row.setStyle("-fx-background-color: " + bgColor + "; -fx-cursor: hand; " +
                "-fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(48, 48); avatar.setMaxSize(48, 48);
        Circle circ = new Circle(24);
        circ.setFill(Color.web(contact.getRole().equalsIgnoreCase("COACH") ? "#F5C87A" : "#4A6FA5"));
        Label init = new Label(contact.getPrenom().substring(0, 1).toUpperCase());
        init.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 700;");
        avatar.getChildren().addAll(circ, init);

        // Info column
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        String nameFontWeight = unread > 0 ? "700" : "600";
        Label name = new Label(contact.getPrenom() + " " + contact.getNom());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: " + nameFontWeight + "; -fx-text-fill: #2D3748;");
        Label roleTag = new Label(contact.getRole());
        roleTag.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: #4A6FA5;" +
                "-fx-padding: 1 6; -fx-background-radius: 8;");
        nameRow.getChildren().addAll(name, roleTag);

        Label preview = new Label(lastPreview.isEmpty() ? "Démarrez la conversation..." : lastPreview);
        preview.setMaxWidth(180);
        preview.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (unread > 0 ? "#2D3748" : "#A0AEC0") + ";" +
                (unread > 0 ? "-fx-font-weight:600;" : ""));
        preview.setEllipsisString("...");

        info.getChildren().addAll(nameRow, preview);
        row.getChildren().addAll(avatar, info);

        // Badge non-lus style Instagram
        if (unread > 0) {
            StackPane badge = new StackPane();
            badge.setMinSize(22, 22); badge.setMaxSize(22, 22);
            Circle bc = new Circle(11); bc.setFill(Color.web("#E8956D"));
            Label bl = new Label(unread > 9 ? "9+" : String.valueOf(unread));
            bl.setStyle("-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:700;");
            badge.getChildren().addAll(bc, bl);
            row.getChildren().add(badge);
        }

        VBox wrapper = new VBox(row);
        row.setOnMouseEntered(e -> { if(!isSelected) row.setStyle("-fx-background-color: #FFF8F0; -fx-cursor: hand; -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;"); });
        row.setOnMouseExited(e  -> { if(!isSelected) row.setStyle("-fx-background-color: white; -fx-cursor: hand; -fx-border-color: transparent transparent #F0EDE8 transparent; -fx-border-width: 1;"); });
        row.setOnMouseClicked(e -> openConversation(contact));
        return wrapper;
    }

    // ── Open conversation ─────────────────────────────────────
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
        try {
            List<Message> messages = msgSvc.getConversation(
                    currentUser.getId_user(), selectedContact.getId_user());
            lastMessageCount = messages.size();
            renderMessages(messages);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void renderMessages(List<Message> messages) {
        if (messagesContainer == null) return;
        messagesContainer.getChildren().clear();

        if (messages.isEmpty()) {
            Label empty = new Label("Commencez la conversation 👋");
            empty.setStyle("-fx-text-fill: #A0AEC0; -fx-font-size: 13px; -fx-font-style: italic;");
            HBox c = new HBox(empty);
            c.setAlignment(Pos.CENTER);
            c.setPadding(new Insets(40));
            messagesContainer.getChildren().add(c);
            return;
        }

        // Grouper par date
        String lastDate = "";
        for (Message msg : messages) {
            String dateStr = msg.getDate_envoi().toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            if (!dateStr.equals(lastDate)) {
                lastDate = dateStr;
                messagesContainer.getChildren().add(buildDateSeparator(dateStr));
            }

            boolean mine = (msg.getId_expediteur() == currentUser.getId_user());
            if (msg.getType() == Message.Type.TEXT) {
                messagesContainer.getChildren().add(buildBubble(msg, mine));
            } else {
                messagesContainer.getChildren().add(buildCallRecord(msg, mine));
            }
        }
        Platform.runLater(() -> { if(scrollMessages!=null) scrollMessages.setVvalue(1.0); });
    }

    // ── Bulle de message ──────────────────────────────────────
    private HBox buildBubble(Message msg, boolean mine) {
        Label bubble = new Label(msg.getContenu() + (msg.isModifie() ? "  ✏️" : ""));
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        Label time = new Label(msg.getDate_envoi().format(FMT_TIME));
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: " +
                (mine ? "rgba(255,255,255,0.6)" : "#A0AEC0") + ";");

        VBox content = new VBox(4, bubble, time);
        content.setMaxWidth(440);

        if (mine) {
            bubble.setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white; -fx-font-size: 13px;" +
                    "-fx-background-radius: 16 16 4 16; -fx-effect: dropshadow(gaussian,rgba(74,111,165,0.2),6,0,0,2);");
            content.setAlignment(Pos.CENTER_RIGHT);
            time.setAlignment(Pos.CENTER_RIGHT);
        } else {
            bubble.setStyle("-fx-background-color: white; -fx-text-fill: #2D3748; -fx-font-size: 13px;" +
                    "-fx-background-radius: 16 16 16 4; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
            content.setAlignment(Pos.CENTER_LEFT);
        }

        // Clic droit → menu contextuel (seulement mes messages)
        if (mine) {
            ContextMenu ctx = buildContextMenu(msg, bubble);
            content.setOnContextMenuRequested(e ->
                    ctx.show(content, e.getScreenX(), e.getScreenY()));
        }

        HBox row = new HBox(content);
        row.setPadding(new Insets(4, 16, 4, 16));
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    // ── Menu contextuel (Edit/Delete) ─────────────────────────
    private ContextMenu buildContextMenu(Message msg, Label bubble) {
        ContextMenu ctx = new ContextMenu();
        ctx.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);");

        MenuItem editItem = new MenuItem("✏️  Modifier");
        editItem.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        editItem.setOnAction(e -> startEditing(msg, bubble));

        MenuItem deleteItem = new MenuItem("🗑️  Supprimer");
        deleteItem.setStyle("-fx-font-size: 12px; -fx-text-fill: #E07070; -fx-padding: 8 16;");
        deleteItem.setOnAction(e -> deleteMessage(msg));

        ctx.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
        return ctx;
    }

    // ── Éditer un message ─────────────────────────────────────
    private void startEditing(Message msg, Label bubble) {
        this.editingMessage = msg;
        this.editingBubble  = bubble;
        if (tfMessage != null) {
            tfMessage.setText(msg.getContenu());
            tfMessage.setStyle(tfMessage.getStyle() +
                    "-fx-border-color: #F5C87A; -fx-border-width: 2;");
            tfMessage.requestFocus();
            tfMessage.selectAll();
        }
    }

    // ── Supprimer un message ──────────────────────────────────
    private void deleteMessage(Message msg) {
        if (!LightDialog.showConfirmation("Supprimer", "Supprimer ce message ?", "🗑️")) return;
        try {
            msgSvc.deleteMessage(msg.getId_message());
            loadMessages();
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de supprimer.");
        }
    }

    // ── Enregistrement d'appel dans la conversation ───────────
    private HBox buildCallRecord(Message msg, boolean mine) {
        String icon = msg.getType() == Message.Type.CALL_MISSED ? "📵" : "📞";
        String label;
        if (msg.getType() == Message.Type.CALL_MISSED)
            label = mine ? "Appel manqué" : "Appel manqué";
        else
            label = mine ? "Appel · " + msg.getContenu() : "Appel reçu · " + msg.getContenu();

        HBox bubble = new HBox(8);
        bubble.setAlignment(Pos.CENTER);
        bubble.setPadding(new Insets(8, 14, 8, 14));
        bubble.setStyle("-fx-background-color: " + (msg.getType() == Message.Type.CALL_MISSED ?
                "#FFE8E8" : "#EBF4FF") + "; -fx-background-radius: 20;");
        bubble.getChildren().addAll(
                new Label(icon),
                new Label(label) {{
                    setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                            (msg.getType() == Message.Type.CALL_MISSED ? "#E07070" : "#4A6FA5") +
                            "; -fx-font-weight: 600;");
                }},
                new Label(msg.getDate_envoi().format(FMT_TIME)) {{
                    setStyle("-fx-font-size: 10px; -fx-text-fill: #A0AEC0;");
                }}
        );
        HBox row = new HBox(bubble);
        row.setPadding(new Insets(4, 16, 4, 16));
        row.setAlignment(Pos.CENTER);
        return row;
    }

    // ── Séparateur de date ────────────────────────────────────
    private HBox buildDateSeparator(String dateStr) {
        Label lbl = new Label(dateStr);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #A0AEC0; -fx-font-weight: 600;" +
                "-fx-background-color: #F0EDE8; -fx-padding: 3 12; -fx-background-radius: 10;");
        HBox h = new HBox(lbl);
        h.setAlignment(Pos.CENTER);
        h.setPadding(new Insets(8, 0, 8, 0));
        return h;
    }

    // ── Envoyer / terminer édition ────────────────────────────
    @FXML void sendMessage() {
        if (selectedContact == null || tfMessage == null) return;
        String text = tfMessage.getText().trim();
        if (text.isEmpty()) return;

        // Mode édition
        if (editingMessage != null) {
            try {
                msgSvc.editMessage(editingMessage.getId_message(), text);
                cancelEdit();
                loadMessages();
            } catch (SQLException e) {
                LightDialog.showError("Erreur", "Impossible de modifier.");
            }
            return;
        }

        // Nouveau message
        Message msg = new Message(currentUser.getId_user(), selectedContact.getId_user(), text);
        try {
            msgSvc.sendMessage(msg);
            tfMessage.clear();
            loadMessages();
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible d'envoyer le message.");
        }
    }

    private void cancelEdit() {
        editingMessage = null;
        editingBubble  = null;
        if (tfMessage != null) {
            tfMessage.clear();
            tfMessage.setStyle(tfMessage.getStyle().replace("-fx-border-color: #F5C87A; -fx-border-width: 2;", ""));
        }
    }

    @FXML void onEnterPressed(javafx.scene.input.KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.ENTER) sendMessage();
        if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) cancelEdit();
    }

    // ── Supprimer conversation ────────────────────────────────
    @FXML void handleDeleteConversation() {
        if (selectedContact == null) return;
        if (!LightDialog.showConfirmation("Supprimer",
                "Supprimer toute la conversation avec " + selectedContact.getPrenom() + " ?", "🗑️")) return;
        try {
            msgSvc.deleteConversation(currentUser.getId_user(), selectedContact.getId_user());
            selectedContact = null;
            if (chatPane != null) { chatPane.setVisible(false); chatPane.setManaged(false); }
            if (inputBar != null) { inputBar.setVisible(false); inputBar.setManaged(false); }
            if (lblNoConv != null) { lblNoConv.setVisible(true); lblNoConv.setManaged(true); }
            loadConversations();
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de supprimer la conversation.");
        }
    }

    // ── Appel vocal ───────────────────────────────────────────
    @FXML void handleCall() {
        if (selectedContact == null) return;
        try {
            // 1. Ouvrir audio server → obtenir port
            Audiocallservice audioSvc = new Audiocallservice();
            int port = audioSvc.startAsServer();
            String ip = Audiocallservice.getLocalIP();

            // 2. Enregistrer l'appel en BD avec IP:port
            int callId = callSvc.initiateCall(
                    currentUser.getId_user(), selectedContact.getId_user(), ip, port);

            // 3. Ouvrir l'écran d'appel
            openCallScreen(false, null, callId, audioSvc);

        } catch (Exception e) {
            LightDialog.showError("Erreur", "Impossible de lancer l'appel: " + e.getMessage());
        }
    }

    /** Ouvrir l'écran d'appel */
    public void openCallScreen(boolean incoming, Call incomingCall, int callId,
                               Audiocallservice audioSvc) {
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
                loadMessages(); // rafraîchir pour voir l'historique appel
            });

            if (incoming && incomingCall != null) {
                // Résoudre l'appelant depuis la BD (selectedContact peut être null si
                // la notification arrive avant qu'une conversation soit ouverte)
                User caller = selectedContact;
                if (caller == null || caller.getId_user() != incomingCall.getId_caller()) {
                    try { caller = us.getUserById(incomingCall.getId_caller()); }
                    catch (SQLException ex) { ex.printStackTrace(); }
                }
                if (caller == null) {
                    LightDialog.showError("Erreur", "Impossible de trouver l'appelant.");
                    Notificationservice.INSTANCE.setCallScreenOpen(false);
                    callStage.close();
                    return;
                }
                ctrl.setupIncomingCall(currentUser, caller, incomingCall);
            } else {
                // Pour appel sortant: selectedContact doit être défini
                if (selectedContact == null) {
                    LightDialog.showError("Erreur", "Sélectionnez d'abord un contact.");
                    Notificationservice.INSTANCE.setCallScreenOpen(false);
                    callStage.close();
                    return;
                }
                ctrl.setupOutgoingCall(currentUser, selectedContact, callId);
            }

            callStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible d'ouvrir l'écran d'appel.");
        }
    }

    // Surcharge pour compatibilité
    public void openCallScreen(boolean incoming, Call incomingCall, int callId) {
        openCallScreen(incoming, incomingCall, callId, null);
    }
}