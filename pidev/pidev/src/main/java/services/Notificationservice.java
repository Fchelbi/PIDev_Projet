package services;

import entities.Call;
import entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Service notifications — polling 2.5s
 * Badge par expéditeur + toast avec nom de l'expéditeur
 */
public class Notificationservice {

    private final Servicemessage msgService = new Servicemessage();
    private final Callservice    callService = new Callservice();
    private final serviceUser    userService = new serviceUser();

    private Timeline pollingTimer;
    private User currentUser;

    // Callbacks
    private Consumer<Integer>          onUnreadCountChanged;
    private Consumer<Map<Integer, String>> onUnreadPerSender; // senderId → preview
    private Consumer<Call>             onIncomingCall;
    private Consumer<Call>             onCallStatusChanged;
    private Runnable                   onNewMessage;

    private int     lastUnreadCount = 0;
    private int     pendingCallId   = -1;
    private boolean callScreenOpen  = false;

    public static final Notificationservice INSTANCE = new Notificationservice();
    private Notificationservice() {}

    public void start(User user) {
        this.currentUser = user;
        if (pollingTimer != null) pollingTimer.stop();
        lastUnreadCount = 0;
        pendingCallId   = -1;
        callScreenOpen  = false;

        pollingTimer = new Timeline(new KeyFrame(Duration.seconds(2.5), e -> poll()));
        pollingTimer.setCycleCount(Timeline.INDEFINITE);
        pollingTimer.play();
        System.out.println("🔔 NotificationService → " + user.getPrenom());
    }

    public void stop() {
        if (pollingTimer != null) { pollingTimer.stop(); pollingTimer = null; }
    }

    public void setPendingCallId(int id)     { this.pendingCallId   = id; }
    public void setCallScreenOpen(boolean v)  { this.callScreenOpen  = v; }

    public void setOnUnreadCountChanged(Consumer<Integer> cb)               { this.onUnreadCountChanged = cb; }
    public void setOnUnreadPerSender(Consumer<Map<Integer, String>> cb)     { this.onUnreadPerSender = cb; }
    public void setOnIncomingCall(Consumer<Call> cb)                        { this.onIncomingCall = cb; }
    public void setOnCallStatusChanged(Consumer<Call> cb)                   { this.onCallStatusChanged = cb; }
    public void setOnNewMessage(Runnable cb)                                { this.onNewMessage = cb; }

    // ── Poll ─────────────────────────────────────────────────
    private void poll() {
        if (currentUser == null) return;
        checkMessages();
        checkCalls();
    }

    private void checkMessages() {
        try {
            // Badge total
            int unread = msgService.countUnread(currentUser.getId_user());

            if (unread != lastUnreadCount) {
                boolean increased = unread > lastUnreadCount;
                lastUnreadCount = unread;

                if (onUnreadCountChanged != null)
                    Platform.runLater(() -> onUnreadCountChanged.accept(unread));

                // Notification toast si nouveau message
                if (increased && onNewMessage != null) {
                    // Récupérer préview par expéditeur pour le toast
                    Map<Integer, String> previews = msgService.getUnreadSenderPreviews(currentUser.getId_user());
                    if (!previews.isEmpty()) {
                        int senderId = previews.keySet().iterator().next();
                        String preview = previews.values().iterator().next();
                        try {
                            User sender = userService.getUserById(senderId);
                            String senderName = sender != null ?
                                    sender.getPrenom() + " " + sender.getNom() + " · " + sender.getRole() :
                                    "Nouveau message";
                            String truncated = preview.length() > 60 ? preview.substring(0, 60) + "..." : preview;
                            // Déclencher le toast avec le nom de l'expéditeur
                            Platform.runLater(() -> {
                                utils.Notificationhelper.show("💬", senderName, truncated, onNewMessage);
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> onNewMessage.run());
                        }
                    }

                    // Badge par expéditeur
                    if (onUnreadPerSender != null)
                        Platform.runLater(() -> {
                            try { onUnreadPerSender.accept(msgService.getUnreadSenderPreviews(currentUser.getId_user())); }
                            catch (SQLException ex) { /* ignore */ }
                        });
                }
            }
        } catch (SQLException e) {
            System.err.println("Poll messages: " + e.getMessage());
        }
    }

    private void checkCalls() {
        try {
            if (!callScreenOpen) {
                callService.getIncomingCall(currentUser.getId_user()).ifPresent(call -> {
                    callScreenOpen = true;
                    if (onIncomingCall != null)
                        Platform.runLater(() -> onIncomingCall.accept(call));
                });
            }
            if (pendingCallId >= 0) {
                callService.getCallById(pendingCallId).ifPresent(call -> {
                    if (call.getStatus() != Call.Status.RINGING) {
                        pendingCallId = -1;
                        if (onCallStatusChanged != null)
                            Platform.runLater(() -> onCallStatusChanged.accept(call));
                    }
                });
            }
        } catch (SQLException e) {
            System.err.println("Poll calls: " + e.getMessage());
        }
    }

    public void refreshBadge() {
        try {
            int unread = msgService.countUnread(currentUser.getId_user());
            lastUnreadCount = unread;
            if (onUnreadCountChanged != null)
                Platform.runLater(() -> onUnreadCountChanged.accept(unread));
            if (onUnreadPerSender != null) {
                Map<Integer, String> previews = msgService.getUnreadSenderPreviews(currentUser.getId_user());
                Platform.runLater(() -> onUnreadPerSender.accept(previews));
            }
        } catch (SQLException e) { /* ignore */ }
    }
}